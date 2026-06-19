package com.farm.plantprotection.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farm.plantprotection.common.BusinessException;
import com.farm.plantprotection.entity.*;
import com.farm.plantprotection.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class HarvestPlanService {

    private static final AtomicInteger planCounter = new AtomicInteger(0);

    @Resource
    private HarvestPlanMapper harvestPlanMapper;

    @Resource
    private FarmPlotMapper farmPlotMapper;

    @Resource
    private FlightOperationMapper flightOperationMapper;

    @Resource
    private SafetyIntervalConfigMapper safetyIntervalConfigMapper;

    @Resource
    private PesticideMapper pesticideMapper;

    @Resource
    private PesticideOutboundDetailMapper outboundDetailMapper;

    @Transactional(rollbackFor = Exception.class)
    public HarvestPlan createHarvestPlan(HarvestPlan plan) {
        if (plan.getPlotId() == null) {
            throw new BusinessException("请选择地块");
        }
        FarmPlot plot = farmPlotMapper.selectById(plan.getPlotId());
        if (plot == null) {
            throw new BusinessException("地块不存在");
        }

        plan.setPlotName(plot.getPlotName());
        plan.setCropType(plot.getCropType());
        plan.setCropVariety(plot.getCropVariety());
        plan.setPlanNo(generatePlanNo());

        if (plan.getStatus() == null) {
            plan.setStatus("PLANNED");
        }
        if (plan.getIsLocked() == null) {
            plan.setIsLocked(0);
        }

        checkAndLockIfNeeded(plan);

        harvestPlanMapper.insert(plan);
        log.info("创建采收计划成功，计划编号：{}", plan.getPlanNo());
        return plan;
    }

    private void checkAndLockIfNeeded(HarvestPlan plan) {
        List<FlightOperation> recentOps = flightOperationMapper.selectList(
                new LambdaQueryWrapper<FlightOperation>()
                        .eq(FlightOperation::getPlotId, plan.getPlotId())
                        .eq(FlightOperation::getOperationStatus, "COMPLETED")
                        .ge(FlightOperation::getOperationDate, plan.getPlannedHarvestDate().minusDays(60))
                        .orderByDesc(FlightOperation::getOperationDate)
        );

        if (recentOps.isEmpty()) return;

        FarmPlot plot = farmPlotMapper.selectById(plan.getPlotId());
        String cropType = plot != null ? plot.getCropType() : null;

        for (FlightOperation op : recentOps) {
            List<PesticideOutboundDetail> details = outboundDetailMapper.selectList(
                    new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, op.getOutboundId())
            );
            for (PesticideOutboundDetail pod : details) {
                int intervalDays = getEffectiveIntervalDays(pod.getPesticideId(), cropType);
                LocalDate safeEndDate = op.getOperationDate().plusDays(intervalDays);

                if (plan.getPlannedHarvestDate().isBefore(safeEndDate)) {
                    Pesticide pesticide = pesticideMapper.selectById(pod.getPesticideId());
                    String pName = pesticide != null ? pesticide.getPesticideName() : "未知农药";
                    plan.setIsLocked(1);
                    plan.setStatus("LOCKED");
                    plan.setLockReason(String.format("%s喷施%s，安全间隔期%d天，到期日%s，期间禁止采收",
                            op.getOperationDate(), pName, intervalDays, safeEndDate));
                    plan.setLockExpireDate(safeEndDate);
                    log.info("采收计划自动锁定：地块【{}】{}喷施{}，安全期至{}", plan.getPlotName(), op.getOperationDate(), pName, safeEndDate);
                    return;
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlockExpiredPlans() {
        LocalDate today = LocalDate.now();
        List<HarvestPlan> lockedPlans = harvestPlanMapper.selectList(
                new LambdaQueryWrapper<HarvestPlan>()
                        .eq(HarvestPlan::getIsLocked, 1)
                        .eq(HarvestPlan::getStatus, "LOCKED")
        );

        for (HarvestPlan plan : lockedPlans) {
            if (plan.getLockExpireDate() != null && !today.isBefore(plan.getLockExpireDate())) {
                plan.setIsLocked(0);
                plan.setStatus("ACTIVE");
                plan.setLockReason(plan.getLockReason() + "（已于" + today + "自动解锁）");
                harvestPlanMapper.updateById(plan);
                log.info("采收计划【{}】安全间隔期已满，自动解锁", plan.getPlanNo());
            }
        }
    }

    public IPage<HarvestPlan> queryPage(int pageNum, int pageSize, Long plotId, String status, Boolean lockedOnly) {
        Page<HarvestPlan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HarvestPlan> wrapper = new LambdaQueryWrapper<>();
        if (plotId != null) {
            wrapper.eq(HarvestPlan::getPlotId, plotId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(HarvestPlan::getStatus, status);
        }
        if (Boolean.TRUE.equals(lockedOnly)) {
            wrapper.eq(HarvestPlan::getIsLocked, 1);
        }
        wrapper.orderByDesc(HarvestPlan::getPlannedHarvestDate);
        return harvestPlanMapper.selectPage(page, wrapper);
    }

    public HarvestPlan getDetail(Long id) {
        return harvestPlanMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public HarvestPlan completeHarvest(Long id, Long operatorId) {
        HarvestPlan plan = harvestPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("采收计划不存在");
        }
        if (plan.getIsLocked() != null && plan.getIsLocked() == 1) {
            throw new BusinessException("采收计划已锁定，安全间隔期未满！锁定原因：" + plan.getLockReason());
        }
        plan.setStatus("COMPLETED");
        harvestPlanMapper.updateById(plan);
        log.info("采收计划【{}】已完成", plan.getPlanNo());
        return plan;
    }

    @Transactional(rollbackFor = Exception.class)
    public HarvestPlan cancelHarvest(Long id) {
        HarvestPlan plan = harvestPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("采收计划不存在");
        }
        plan.setStatus("CANCELLED");
        harvestPlanMapper.updateById(plan);
        return plan;
    }

    private int getEffectiveIntervalDays(Long pesticideId, String cropType) {
        if (cropType != null) {
            SafetyIntervalConfig config = safetyIntervalConfigMapper.selectOne(
                    new LambdaQueryWrapper<SafetyIntervalConfig>()
                            .eq(SafetyIntervalConfig::getPesticideId, pesticideId)
                            .eq(SafetyIntervalConfig::getCropType, cropType)
                            .eq(SafetyIntervalConfig::getStatus, 1)
            );
            if (config != null) {
                return config.getIntervalDays();
            }
        }
        Pesticide pesticide = pesticideMapper.selectById(pesticideId);
        if (pesticide != null && pesticide.getSafetyInterval() != null && pesticide.getSafetyInterval() > 0) {
            return pesticide.getSafetyInterval();
        }
        return 7;
    }

    private synchronized String generatePlanNo() {
        String datePart = DateUtil.format(DateUtil.date(), "yyyyMM");
        int seq = planCounter.incrementAndGet() % 10000;
        return "HV" + datePart + String.format("%04d", seq);
    }
}
