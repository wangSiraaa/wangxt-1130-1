package com.farm.plantprotection.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farm.plantprotection.common.BusinessException;
import com.farm.plantprotection.common.BusinessValidator;
import com.farm.plantprotection.entity.*;
import com.farm.plantprotection.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrescriptionService {

    private static final AtomicInteger rxCounter = new AtomicInteger(0);

    @Resource
    private PrescriptionMapper prescriptionMapper;

    @Resource
    private PrescriptionDetailMapper prescriptionDetailMapper;

    @Resource
    private FarmPlotMapper farmPlotMapper;

    @Resource
    private PesticideMapper pesticideMapper;

    @Resource
    private BusinessValidator businessValidator;

    @Resource
    private PlotNeighborMapper plotNeighborMapper;

    @Resource
    private SafetyIntervalConfigMapper safetyIntervalConfigMapper;

    @Resource
    private SafetyIntervalReminderMapper safetyIntervalReminderMapper;

    public List<String> validatePrescriptionComprehensive(Long plotId, List<PrescriptionDetail> details, LocalDate prescriptionDate) {
        List<String> warnings = new ArrayList<>();
        if (plotId == null || details == null || details.isEmpty()) {
            return warnings;
        }
        FarmPlot plot = farmPlotMapper.selectById(plotId);
        if (plot == null) {
            return warnings;
        }

        for (PrescriptionDetail detail : details) {
            if (detail.getPesticideId() == null) continue;
            Pesticide pesticide = pesticideMapper.selectById(detail.getPesticideId());
            if (pesticide == null) continue;

            if (pesticide.getIsForbidden() != null && pesticide.getIsForbidden() == 1) {
                warnings.add("【禁限用药】农药【" + pesticide.getPesticideName() + "】为禁用农药！原因：" + pesticide.getForbiddenReason());
            }

            if (plot.getCropType() != null && pesticide.getApplicableCrops() != null) {
                String[] applicableArr = pesticide.getApplicableCrops().split("/");
                boolean applicable = false;
                for (String ac : applicableArr) {
                    if (ac.trim().equals(plot.getCropType())) {
                        applicable = true;
                        break;
                    }
                }
                if (!applicable) {
                    warnings.add("【作物不适用】农药【" + pesticide.getPesticideName() + "】适用作物为" + pesticide.getApplicableCrops() + "，当前地块作物为" + plot.getCropType() + "，可能产生药害！");
                }
            }

            SafetyIntervalConfig config = safetyIntervalConfigMapper.selectOne(
                    new LambdaQueryWrapper<SafetyIntervalConfig>()
                            .eq(SafetyIntervalConfig::getPesticideId, detail.getPesticideId())
                            .eq(SafetyIntervalConfig::getCropType, plot.getCropType())
                            .eq(SafetyIntervalConfig::getStatus, 1)
            );
            int intervalDays = config != null ? config.getIntervalDays() :
                    (pesticide.getSafetyInterval() != null && pesticide.getSafetyInterval() > 0 ? pesticide.getSafetyInterval() : 7);
            warnings.add("【安全间隔期】农药【" + pesticide.getPesticideName() + "】安全间隔期" + intervalDays + "天，施药后" + intervalDays + "天内禁止采收");

            List<PlotNeighbor> neighbors = plotNeighborMapper.selectList(
                    new LambdaQueryWrapper<PlotNeighbor>().eq(PlotNeighbor::getPlotId, plotId)
            );
            for (PlotNeighbor neighbor : neighbors) {
                FarmPlot neighborPlot = farmPlotMapper.selectById(neighbor.getNeighborPlotId());
                if (neighborPlot == null) continue;
                if (neighborPlot.getCropType() != null && pesticide.getApplicableCrops() != null) {
                    String[] appArr = pesticide.getApplicableCrops().split("/");
                    boolean neighborApplicable = false;
                    for (String ac : appArr) {
                        if (ac.trim().equals(neighborPlot.getCropType())) {
                            neighborApplicable = true;
                            break;
                        }
                    }
                    if (!neighborApplicable) {
                        warnings.add("【邻近地块风险】邻近地块【" + neighbor.getNeighborPlotName() + "】(" + neighbor.getNeighborDirection() + "，" + neighbor.getDistance() + "m)种植" + neighborPlot.getCropType() + "，农药【" + pesticide.getPesticideName() + "】对该作物不适用，存在漂移药害风险！");
                    }
                }
                if (neighbor.getDistance() != null && neighbor.getDistance().compareTo(new BigDecimal("100")) <= 0) {
                    if (pesticide.getToxicity() != null && ("高毒".equals(pesticide.getToxicity()) || "剧毒".equals(pesticide.getToxicity()))) {
                        warnings.add("【邻近地块风险】农药【" + pesticide.getPesticideName() + "】为" + pesticide.getToxicity() + "农药，邻近地块【" + neighbor.getNeighborPlotName() + "】距离仅" + neighbor.getDistance() + "m，建议使用低毒替代药！");
                    }
                }
            }
        }

        return warnings;
    }

    @Transactional(rollbackFor = Exception.class)
    public Prescription createPrescription(Prescription prescription) {
        if (prescription.getPlotId() == null) {
            throw new BusinessException("请选择地块");
        }
        FarmPlot plot = farmPlotMapper.selectById(prescription.getPlotId());
        if (plot == null) {
            throw new BusinessException("地块不存在");
        }
        prescription.setPlotName(plot.getPlotName());

        if (prescription.getPrescriptionDate() == null) {
            prescription.setPrescriptionDate(LocalDate.now());
        }

        prescription.setPrescriptionNo(generatePrescriptionNo());
        prescription.setStatus("DRAFT");

        if (prescription.getOccurrenceArea() != null && prescription.getOccurrenceArea().compareTo(plot.getArea()) > 0) {
            throw new BusinessException("发生面积不能超过地块总面积" + plot.getArea() + "亩");
        }

        prescriptionMapper.insert(prescription);

        if (prescription.getDetails() != null && !prescription.getDetails().isEmpty()) {
            for (PrescriptionDetail detail : prescription.getDetails()) {
                validatePrescriptionDetail(detail, plot, prescription.getOccurrenceArea() != null ? prescription.getOccurrenceArea() : plot.getArea());
                detail.setPrescriptionId(prescription.getId());
                prescriptionDetailMapper.insert(detail);
            }
        }

        log.info("创建处方成功，处方号：{}", prescription.getPrescriptionNo());
        return prescription;
    }

    private void validatePrescriptionDetail(PrescriptionDetail detail, FarmPlot plot, BigDecimal area) {
        if (detail.getPesticideId() == null) {
            throw new BusinessException("请选择农药");
        }
        Pesticide pesticide = pesticideMapper.selectById(detail.getPesticideId());
        if (pesticide == null) {
            throw new BusinessException("农药不存在");
        }

        businessValidator.validatePesticideNotForbidden(detail.getPesticideId());

        if (plot.getCropType() != null && pesticide.getApplicableCrops() != null) {
            String[] applicableArr = pesticide.getApplicableCrops().split("/");
            boolean applicable = false;
            for (String ac : applicableArr) {
                if (ac.trim().equals(plot.getCropType())) {
                    applicable = true;
                    break;
                }
            }
            if (!applicable) {
                throw new BusinessException("农药【" + pesticide.getPesticideName() + "】适用作物为" + pesticide.getApplicableCrops() + "，当前地块作物为" + plot.getCropType() + "，禁止使用！");
            }
        }

        businessValidator.validateSafetyInterval(plot.getId(), detail.getPesticideId(), LocalDate.now());

        List<PlotNeighbor> neighbors = plotNeighborMapper.selectList(
                new LambdaQueryWrapper<PlotNeighbor>().eq(PlotNeighbor::getPlotId, plot.getId())
        );
        for (PlotNeighbor neighbor : neighbors) {
            FarmPlot neighborPlot = farmPlotMapper.selectById(neighbor.getNeighborPlotId());
            if (neighborPlot == null) continue;
            if (neighborPlot.getCropType() != null && pesticide.getApplicableCrops() != null) {
                String[] appArr = pesticide.getApplicableCrops().split("/");
                boolean neighborApplicable = false;
                for (String ac : appArr) {
                    if (ac.trim().equals(neighborPlot.getCropType())) {
                        neighborApplicable = true;
                        break;
                    }
                }
                if (!neighborApplicable && neighbor.getDistance() != null && neighbor.getDistance().compareTo(new BigDecimal("100")) <= 0) {
                    throw new BusinessException("邻近地块【" + neighbor.getNeighborPlotName() + "】(" + neighbor.getNeighborDirection() + "，" + neighbor.getDistance() + "m)种植" + neighborPlot.getCropType() + "，农药【" + pesticide.getPesticideName() + "】存在漂移药害风险，请更换低风险农药或增设隔离带！");
                }
            }
        }

        detail.setPesticideCode(pesticide.getPesticideCode());
        detail.setPesticideName(pesticide.getPesticideName());

        if (detail.getDosagePerMu() == null || detail.getDosagePerMu().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请输入有效的每亩用量");
        }

        if (detail.getTotalQuantity() == null) {
            detail.setTotalQuantity(detail.getDosagePerMu().multiply(area));
        }

        if (detail.getUnit() == null || detail.getUnit().isEmpty()) {
            detail.setUnit(pesticide.getUnit());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Prescription submitPrescription(Long id, Long agronomistId, String agronomistName) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (!"DRAFT".equals(prescription.getStatus())) {
            throw new BusinessException("当前状态不允许提交，状态：" + prescription.getStatus());
        }

        List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(
                new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, id)
        );
        if (details.isEmpty()) {
            throw new BusinessException("处方明细不能为空，请添加农药");
        }

        prescription.setStatus("SUBMITTED");
        prescription.setAgronomistId(agronomistId);
        prescription.setAgronomistName(agronomistName);
        prescriptionMapper.updateById(prescription);

        log.info("处方提交成功，处方号：{}", prescription.getPrescriptionNo());
        return prescription;
    }

    @Transactional(rollbackFor = Exception.class)
    public Prescription approvePrescription(Long id, Long approveBy, String approveRemark, boolean passed) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (!"SUBMITTED".equals(prescription.getStatus())) {
            throw new BusinessException("当前状态不允许审批，状态：" + prescription.getStatus());
        }

        prescription.setStatus(passed ? "APPROVED" : "REJECTED");
        prescription.setApproveBy(approveBy);
        prescription.setApproveTime(LocalDateTime.now());
        prescription.setApproveRemark(approveRemark);
        prescriptionMapper.updateById(prescription);

        log.info("处方审批{}，处方号：{}", passed ? "通过" : "驳回", prescription.getPrescriptionNo());
        return prescription;
    }

    public Prescription getDetail(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription != null) {
            List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(
                    new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, id)
            );
            prescription.setDetails(details);
        }
        return prescription;
    }

    public IPage<Prescription> queryPage(int pageNum, int pageSize, String status, Long plotId, Long agronomistId) {
        Page<Prescription> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Prescription::getStatus, status);
        }
        if (plotId != null) {
            wrapper.eq(Prescription::getPlotId, plotId);
        }
        if (agronomistId != null) {
            wrapper.eq(Prescription::getAgronomistId, agronomistId);
        }
        wrapper.orderByDesc(Prescription::getCreateTime);
        return prescriptionMapper.selectPage(page, wrapper);
    }

    public List<Prescription> getApprovedPrescriptionsForOutbound() {
        return prescriptionMapper.selectList(
                new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getStatus, "APPROVED")
                        .orderByDesc(Prescription::getCreateTime)
        );
    }

    private synchronized String generatePrescriptionNo() {
        String datePart = DateUtil.format(DateUtil.date(), "yyyyMM");
        int seq = rxCounter.incrementAndGet() % 10000;
        return "RX" + datePart + String.format("%04d", seq);
    }
}
