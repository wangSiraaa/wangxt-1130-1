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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class PesticideOutboundService {

    private static final AtomicInteger outCounter = new AtomicInteger(0);

    @Resource
    private PesticideOutboundMapper outboundMapper;

    @Resource
    private PesticideOutboundDetailMapper outboundDetailMapper;

    @Resource
    private PrescriptionMapper prescriptionMapper;

    @Resource
    private PrescriptionDetailMapper prescriptionDetailMapper;

    @Resource
    private PesticideStockMapper pesticideStockMapper;

    @Resource
    private PesticideMapper pesticideMapper;

    @Resource
    private FarmPlotMapper farmPlotMapper;

    @Resource
    private BusinessValidator businessValidator;

    @Transactional(rollbackFor = Exception.class)
    public PesticideOutbound createOutboundFromPrescription(Long prescriptionId, Long warehouseKeeperId, String warehouseKeeperName) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        if (!"APPROVED".equals(prescription.getStatus())) {
            throw new BusinessException("处方未审批通过，不能出库，当前状态：" + prescription.getStatus());
        }

        Long count = outboundMapper.selectCount(
                new LambdaQueryWrapper<PesticideOutbound>()
                        .eq(PesticideOutbound::getPrescriptionId, prescriptionId)
                        .ne(PesticideOutbound::getStatus, "CANCELLED")
        );
        if (count > 0) {
            throw new BusinessException("该处方已生成出库单，请勿重复出库");
        }

        PesticideOutbound outbound = new PesticideOutbound();
        outbound.setOutboundNo(generateOutboundNo());
        outbound.setPrescriptionId(prescriptionId);
        outbound.setPrescriptionNo(prescription.getPrescriptionNo());
        outbound.setPlotId(prescription.getPlotId());
        outbound.setPlotName(prescription.getPlotName());
        outbound.setOutboundType("PRESCRIPTION");
        outbound.setOutboundDate(LocalDate.now());
        outbound.setWarehouseKeeperId(warehouseKeeperId);
        outbound.setWarehouseKeeperName(warehouseKeeperName);
        outbound.setStatus("PENDING");
        outboundMapper.insert(outbound);

        List<PrescriptionDetail> rxDetails = prescriptionDetailMapper.selectList(
                new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, prescriptionId)
        );
        for (PrescriptionDetail rxd : rxDetails) {
            PesticideOutboundDetail detail = new PesticideOutboundDetail();
            detail.setOutboundId(outbound.getId());
            detail.setPrescriptionDetailId(rxd.getId());
            detail.setPesticideId(rxd.getPesticideId());
            detail.setPesticideCode(rxd.getPesticideCode());
            detail.setPesticideName(rxd.getPesticideName());
            detail.setQuantity(rxd.getTotalQuantity());
            detail.setUnit(rxd.getUnit());
            outboundDetailMapper.insert(detail);
        }

        log.info("创建出库单成功，出库单号：{}", outbound.getOutboundNo());
        return getDetail(outbound.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PesticideOutbound confirmOutbound(Long outboundId, List<PesticideOutboundDetail> details, Long receiverId, String receiverName) {
        PesticideOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }
        if (!"PENDING".equals(outbound.getStatus())) {
            throw new BusinessException("当前状态不允许确认出库，状态：" + outbound.getStatus());
        }

        if (details == null || details.isEmpty()) {
            throw new BusinessException("出库明细不能为空");
        }

        for (PesticideOutboundDetail d : details) {
            businessValidator.validatePesticideNotForbidden(d.getPesticideId());

            if (d.getBatchNo() == null || d.getBatchNo().isEmpty()) {
                throw new BusinessException("请选择农药【" + d.getPesticideName() + "】的批号");
            }

            PesticideStock stock = pesticideStockMapper.selectOne(
                    new LambdaQueryWrapper<PesticideStock>()
                            .eq(PesticideStock::getPesticideId, d.getPesticideId())
                            .eq(PesticideStock::getBatchNo, d.getBatchNo())
            );
            if (stock == null) {
                throw new BusinessException("库存中不存在批号【" + d.getBatchNo() + "】的" + d.getPesticideName());
            }

            businessValidator.validatePesticideStock(d.getPesticideId(), d.getBatchNo(), d.getQuantity());

            d.setStockId(stock.getId());

            PesticideOutboundDetail existDetail = outboundDetailMapper.selectById(d.getId());
            if (existDetail != null) {
                existDetail.setBatchNo(d.getBatchNo());
                existDetail.setQuantity(d.getQuantity());
                existDetail.setStockId(d.getId());
                existDetail.setRemark(d.getRemark());
                outboundDetailMapper.updateById(existDetail);
            }
        }

        for (PesticideOutboundDetail d : details) {
            PesticideStock stock = pesticideStockMapper.selectById(d.getStockId());
            if (stock != null) {
                stock.setQuantity(stock.getQuantity().subtract(d.getQuantity()));
                pesticideStockMapper.updateById(stock);
            }
        }

        outbound.setReceiverId(receiverId);
        outbound.setReceiverName(receiverName);
        outbound.setStatus("OUTBOUND");
        outbound.setOutboundTime(LocalDateTime.now());
        outboundMapper.updateById(outbound);

        Prescription prescription = prescriptionMapper.selectById(outbound.getPrescriptionId());
        if (prescription != null) {
            prescription.setStatus("COMPLETED");
            prescriptionMapper.updateById(prescription);
        }

        log.info("出库确认成功，出库单号：{}", outbound.getOutboundNo());
        return getDetail(outboundId);
    }

    public PesticideOutbound getDetail(Long id) {
        PesticideOutbound outbound = outboundMapper.selectById(id);
        if (outbound != null) {
            List<PesticideOutboundDetail> details = outboundDetailMapper.selectList(
                    new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, id)
            );
            outbound.setDetails(details);
        }
        return outbound;
    }

    public IPage<PesticideOutbound> queryPage(int pageNum, int pageSize, String status, Long plotId) {
        Page<PesticideOutbound> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PesticideOutbound> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PesticideOutbound::getStatus, status);
        }
        if (plotId != null) {
            wrapper.eq(PesticideOutbound::getPlotId, plotId);
        }
        wrapper.orderByDesc(PesticideOutbound::getCreateTime);
        return outboundMapper.selectPage(page, wrapper);
    }

    public List<PesticideOutbound> getPendingOperationsForPilot(Long pilotId) {
        return outboundMapper.selectList(
                new LambdaQueryWrapper<PesticideOutbound>()
                        .eq(PesticideOutbound::getStatus, "OUTBOUND")
                        .notInSql(PesticideOutbound::getId,
                                "SELECT outbound_id FROM flight_operation WHERE operation_status != 'CANCELLED'")
                        .orderByDesc(PesticideOutbound::getOutboundTime)
        );
    }

    public List<PesticideStock> getStockByPesticide(Long pesticideId) {
        return pesticideStockMapper.selectList(
                new LambdaQueryWrapper<PesticideStock>()
                        .eq(PesticideStock::getPesticideId, pesticideId)
                        .gt(PesticideStock::getQuantity, BigDecimal.ZERO)
                        .orderByAsc(PesticideStock::getExpiryDate)
        );
    }

    private synchronized String generateOutboundNo() {
        String datePart = DateUtil.format(DateUtil.date(), "yyyyMM");
        int seq = outCounter.incrementAndGet() % 10000;
        return "OUT" + datePart + String.format("%04d", seq);
    }
}
