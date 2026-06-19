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

    @Resource
    private PesticideStockPendingMapper pesticideStockPendingMapper;

    private static final AtomicInteger pendingCounter = new AtomicInteger(0);

    @Transactional(rollbackFor = Exception.class)
    public void cancelFlightToPending(Long outboundId, String cancelReason) {
        PesticideOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }
        if (!"OUTBOUND".equals(outbound.getStatus())) {
            throw new BusinessException("出库单状态不允许取消，当前状态：" + outbound.getStatus());
        }

        List<PesticideOutboundDetail> details = outboundDetailMapper.selectList(
                new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, outboundId)
        );

        for (PesticideOutboundDetail d : details) {
            PesticideStockPending pending = new PesticideStockPending();
            pending.setPendingNo(generatePendingNo());
            pending.setPesticideId(d.getPesticideId());
            pending.setPesticideCode(d.getPesticideCode());
            pending.setPesticideName(d.getPesticideName());
            pending.setBatchNo(d.getBatchNo());
            pending.setQuantity(d.getQuantity());
            pending.setUnit(d.getUnit());
            pending.setSourceType("FLIGHT_CANCEL");
            pending.setSourceId(outboundId);
            pending.setSourceNo(outbound.getOutboundNo());
            pending.setPendingReason(cancelReason != null ? cancelReason : "天气变更导致飞行取消，农药需核验后方可回库");
            pending.setStatus("PENDING_VERIFY");
            pending.setStockId(d.getStockId());
            pending.setCreateBy(outbound.getWarehouseKeeperId());
            pesticideStockPendingMapper.insert(pending);
            log.info("农药批号【{}】{}已进入待核验库存，原因：{}", d.getBatchNo(), d.getPesticideName(), cancelReason);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void verifyPendingStock(Long pendingId, Long verifyBy, String verifyByName, boolean approved, String verifyRemark) {
        PesticideStockPending pending = pesticideStockPendingMapper.selectById(pendingId);
        if (pending == null) {
            throw new BusinessException("待核验记录不存在");
        }
        if (!"PENDING_VERIFY".equals(pending.getStatus())) {
            throw new BusinessException("当前状态不允许核验，状态：" + pending.getStatus());
        }

        if (approved) {
            PesticideStock stock = pesticideStockMapper.selectById(pending.getStockId());
            if (stock != null) {
                stock.setQuantity(stock.getQuantity().add(pending.getQuantity()));
                pesticideStockMapper.updateById(stock);
            } else {
                PesticideStock newStock = new PesticideStock();
                newStock.setPesticideId(pending.getPesticideId());
                newStock.setBatchNo(pending.getBatchNo());
                newStock.setQuantity(pending.getQuantity());
                newStock.setUnit(pending.getUnit());
                pesticideStockMapper.insert(newStock);
                pending.setStockId(newStock.getId());
            }
            pending.setStatus("VERIFIED");
            pending.setVerifyRemark(verifyRemark != null ? verifyRemark : "核验通过，已回库");
        } else {
            pending.setStatus("REJECTED");
            pending.setVerifyRemark(verifyRemark != null ? verifyRemark : "核验拒绝，农药品质异常");
        }

        pending.setVerifyBy(verifyBy);
        pending.setVerifyByName(verifyByName);
        pending.setVerifyTime(LocalDateTime.now());
        pesticideStockPendingMapper.updateById(pending);

        log.info("待核验库存【{}】核验{}，农药：{} 批号：{}", pending.getPendingNo(), approved ? "通过回库" : "拒绝", pending.getPesticideName(), pending.getBatchNo());
    }

    public IPage<PesticideStockPending> queryPendingPage(int pageNum, int pageSize, String status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PesticideStockPending> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PesticideStockPending> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PesticideStockPending::getStatus, status);
        }
        wrapper.orderByDesc(PesticideStockPending::getCreateTime);
        return pesticideStockPendingMapper.selectPage(page, wrapper);
    }

    private synchronized String generatePendingNo() {
        String datePart = cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.date(), "yyyyMM");
        int seq = pendingCounter.incrementAndGet() % 10000;
        return "PND" + datePart + String.format("%04d", seq);
    }

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
