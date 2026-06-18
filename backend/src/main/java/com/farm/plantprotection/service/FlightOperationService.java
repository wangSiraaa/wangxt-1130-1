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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FlightOperationService {

    private static final AtomicInteger flyCounter = new AtomicInteger(0);
    private static final AtomicInteger remCounter = new AtomicInteger(0);

    @Resource
    private FlightOperationMapper flightOperationMapper;

    @Resource
    private PesticideOutboundMapper outboundMapper;

    @Resource
    private PesticideOutboundDetailMapper outboundDetailMapper;

    @Resource
    private SafetyIntervalConfigMapper safetyIntervalConfigMapper;

    @Resource
    private SafetyIntervalReminderMapper reminderMapper;

    @Resource
    private PesticideMapper pesticideMapper;

    @Resource
    private FarmPlotMapper farmPlotMapper;

    @Resource
    private BusinessValidator businessValidator;

    public Map<String, Object> preCheckSafety(Long outboundId, LocalDate operationDate, BigDecimal windSpeed) {
        PesticideOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }

        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        boolean passed = true;

        try {
            if (windSpeed != null) {
                List<PesticideOutboundDetail> details = outboundDetailMapper.selectList(
                        new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, outboundId)
                );
                for (PesticideOutboundDetail d : details) {
                    try {
                        businessValidator.validateWindSpeed(d.getPesticideId(), windSpeed);
                    } catch (BusinessException e) {
                        passed = false;
                        warnings.add(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            warnings.add("风速校验异常: " + e.getMessage());
        }

        try {
            List<PesticideOutboundDetail> details = outboundDetailMapper.selectList(
                    new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, outboundId)
            );
            for (PesticideOutboundDetail d : details) {
                try {
                    businessValidator.validateSafetyInterval(outbound.getPlotId(), d.getPesticideId(), operationDate != null ? operationDate : LocalDate.now());
                } catch (BusinessException e) {
                    passed = false;
                    warnings.add(e.getMessage());
                }
            }
        } catch (Exception e) {
            warnings.add("安全间隔期校验异常: " + e.getMessage());
        }

        result.put("passed", passed);
        result.put("warnings", warnings);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlightOperation createOperation(Long outboundId, Long pilotId, String pilotName, FlightOperation operation) {
        PesticideOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }
        if (!"OUTBOUND".equals(outbound.getStatus())) {
            throw new BusinessException("出库单状态不允许作业，状态：" + outbound.getStatus());
        }

        if (operation.getOperationDate() == null) {
            operation.setOperationDate(LocalDate.now());
        }
        if (operation.getOperationArea() == null) {
            FarmPlot plot = farmPlotMapper.selectById(outbound.getPlotId());
            if (plot != null) {
                operation.setOperationArea(plot.getArea());
            }
        }

        List<PesticideOutboundDetail> details = outboundDetailMapper.selectList(
                new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, outboundId)
        );
        for (PesticideOutboundDetail d : details) {
            if (operation.getWindSpeed() != null) {
                businessValidator.validateWindSpeed(d.getPesticideId(), operation.getWindSpeed());
            }
            businessValidator.validateSafetyInterval(outbound.getPlotId(), d.getPesticideId(), operation.getOperationDate());
        }

        businessValidator.validateOperationWeather(
                operation.getWeather(),
                operation.getTemperature(),
                operation.getHumidity()
        );

        operation.setOperationNo(generateOperationNo());
        operation.setOutboundId(outboundId);
        operation.setOutboundNo(outbound.getOutboundNo());
        operation.setPrescriptionId(outbound.getPrescriptionId());
        operation.setPlotId(outbound.getPlotId());
        operation.setPlotName(outbound.getPlotName());
        operation.setPilotId(pilotId);
        operation.setPilotName(pilotName);

        if (operation.getOperationStatus() == null) {
            operation.setOperationStatus("IN_PROGRESS");
        }

        StringBuilder sb = new StringBuilder();
        for (PesticideOutboundDetail d : details) {
            sb.append(d.getPesticideName()).append("(").append(d.getBatchNo()).append("):").append(d.getQuantity()).append(d.getUnit()).append(";");
        }
        operation.setPesticideUsage(sb.toString());

        if (operation.getSafetyCheck() == null) {
            operation.setSafetyCheck(1);
        }

        flightOperationMapper.insert(operation);

        generateSafetyIntervalReminders(operation);

        log.info("创建飞行作业成功，作业编号：{}", operation.getOperationNo());
        return operation;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlightOperation completeOperation(Long id, FlightOperation updateData) {
        FlightOperation operation = flightOperationMapper.selectById(id);
        if (operation == null) {
            throw new BusinessException("作业记录不存在");
        }
        if (!"IN_PROGRESS".equals(operation.getOperationStatus()) && !"PAUSED".equals(operation.getOperationStatus())) {
            throw new BusinessException("当前状态不允许完成作业，状态：" + operation.getOperationStatus());
        }

        if (updateData.getEndTime() == null) {
            updateData.setEndTime(LocalDateTime.now());
        }
        if (updateData.getStartTime() != null && updateData.getEndTime() != null && operation.getFlightDuration() == null) {
            long minutes = ChronoUnit.MINUTES.between(updateData.getStartTime(), updateData.getEndTime());
            updateData.setFlightDuration((int) minutes);
        }

        updateData.setOperationStatus("COMPLETED");
        updateData.setId(id);
        flightOperationMapper.updateById(updateData);

        log.info("作业完成，作业编号：{}", operation.getOperationNo());
        return flightOperationMapper.selectById(id);
    }

    private void generateSafetyIntervalReminders(FlightOperation operation) {
        List<PesticideOutboundDetail> pesticideDetails = outboundDetailMapper.selectList(
                new LambdaQueryWrapper<PesticideOutboundDetail>().eq(PesticideOutboundDetail::getOutboundId, operation.getOutboundId())
        );

        FarmPlot plot = farmPlotMapper.selectById(operation.getPlotId());
        String cropType = plot != null ? plot.getCropType() : null;

        for (PesticideOutboundDetail pod : pesticideDetails) {
            int intervalDays = getEffectiveIntervalDays(pod.getPesticideId(), cropType);
            LocalDate safeEndDate = operation.getOperationDate().plusDays(intervalDays);
            long remaining = ChronoUnit.DAYS.between(LocalDate.now(), safeEndDate);

            SafetyIntervalReminder reminder = new SafetyIntervalReminder();
            reminder.setReminderNo(generateReminderNo());
            reminder.setPlotId(operation.getPlotId());
            reminder.setPlotName(operation.getPlotName());
            reminder.setPesticideId(pod.getPesticideId());
            reminder.setPesticideName(pod.getPesticideName());
            reminder.setLastOperationId(operation.getId());
            reminder.setLastOperationDate(operation.getOperationDate());
            reminder.setSafeEndDate(safeEndDate);
            reminder.setIntervalDays(intervalDays);
            reminder.setRemainingDays((int) Math.max(0, remaining));

            String level;
            String type;
            String content;
            if (remaining > 3) {
                type = "BEFORE_INTERVAL";
                level = "WARNING";
                content = String.format("地块【%s】于%s喷施%s，安全间隔期%d天，当前距可采收日期还有%d天，禁止采收！",
                        operation.getPlotName(), DateUtil.format(DateUtil.date(operation.getOperationDate()), "yyyy-MM-dd"),
                        pod.getPesticideName(), intervalDays, remaining);
            } else if (remaining > 0) {
                type = "APPROACHING";
                level = "DANGER";
                content = String.format("地块【%s】喷施%s即将到期！还有%d天解禁可采收，请密切关注！（安全期至%s）",
                        operation.getPlotName(), pod.getPesticideName(), remaining,
                        DateUtil.format(DateUtil.date(safeEndDate), "yyyy-MM-dd"));
            } else {
                type = "EXPIRED";
                level = "INFO";
                content = String.format("地块【%s】喷施%s安全间隔期已过，现已解禁可采收！（安全期至%s）",
                        operation.getPlotName(), pod.getPesticideName(),
                        DateUtil.format(DateUtil.date(safeEndDate), "yyyy-MM-dd"));
            }
            reminder.setReminderType(type);
            reminder.setReminderLevel(level);
            reminder.setRemindContent(content);
            reminder.setIsRead(0);
            reminder.setReminderTime(LocalDateTime.now());
            reminder.setNotifyUsers(String.valueOf(operation.getPilotId()));

            reminderMapper.insert(reminder);
        }
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

    public IPage<FlightOperation> queryPage(int pageNum, int pageSize, String status, Long plotId, Long pilotId) {
        Page<FlightOperation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlightOperation> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FlightOperation::getOperationStatus, status);
        }
        if (plotId != null) {
            wrapper.eq(FlightOperation::getPlotId, plotId);
        }
        if (pilotId != null) {
            wrapper.eq(FlightOperation::getPilotId, pilotId);
        }
        wrapper.orderByDesc(FlightOperation::getOperationDate);
        return flightOperationMapper.selectPage(page, wrapper);
    }

    public IPage<SafetyIntervalReminder> queryReminders(int pageNum, int pageSize, Long plotId, String reminderType, Boolean unreadOnly) {
        Page<SafetyIntervalReminder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SafetyIntervalReminder> wrapper = new LambdaQueryWrapper<>();
        if (plotId != null) {
            wrapper.eq(SafetyIntervalReminder::getPlotId, plotId);
        }
        if (reminderType != null && !reminderType.isEmpty()) {
            wrapper.eq(SafetyIntervalReminder::getReminderType, reminderType);
        }
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(SafetyIntervalReminder::getIsRead, 0);
        }
        wrapper.orderByDesc(SafetyIntervalReminder::getReminderTime);
        return reminderMapper.selectPage(page, wrapper);
    }

    public List<SafetyIntervalReminder> getActivePlotWarnings(Long plotId) {
        LocalDate today = LocalDate.now();
        List<SafetyIntervalReminder> reminders = reminderMapper.selectList(
                new LambdaQueryWrapper<SafetyIntervalReminder>()
                        .eq(plotId != null, SafetyIntervalReminder::getPlotId, plotId)
                        .ge(SafetyIntervalReminder::getSafeEndDate, today)
                        .ne(SafetyIntervalReminder::getReminderLevel, "INFO")
                        .orderByAsc(SafetyIntervalReminder::getSafeEndDate)
        );

        Map<Long, SafetyIntervalReminder> latestMap = new HashMap<>();
        for (SafetyIntervalReminder r : reminders) {
            Long key = r.getPlotId() * 1000 + (r.getPesticideId() % 1000);
            if (!latestMap.containsKey(key) || r.getReminderTime().isAfter(latestMap.get(key).getReminderTime())) {
                long remaining = ChronoUnit.DAYS.between(today, r.getSafeEndDate());
                r.setRemainingDays((int) Math.max(0, remaining));
                latestMap.put(key, r);
            }
        }
        return new ArrayList<>(latestMap.values());
    }

    @Transactional(rollbackFor = Exception.class)
    public void markReminderRead(Long reminderId, Long userId) {
        SafetyIntervalReminder reminder = reminderMapper.selectById(reminderId);
        if (reminder != null) {
            reminder.setIsRead(1);
            reminder.setReadTime(LocalDateTime.now());
            reminder.setReadBy(userId);
            reminderMapper.updateById(reminder);
        }
    }

    public FlightOperation getDetail(Long id) {
        return flightOperationMapper.selectById(id);
    }

    private synchronized String generateOperationNo() {
        String datePart = DateUtil.format(DateUtil.date(), "yyyyMM");
        int seq = flyCounter.incrementAndGet() % 10000;
        return "FLY" + datePart + String.format("%04d", seq);
    }

    private synchronized String generateReminderNo() {
        String datePart = DateUtil.format(DateUtil.date(), "yyyyMM");
        int seq = remCounter.incrementAndGet() % 10000;
        return "REM" + datePart + String.format("%04d", seq);
    }
}
