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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
