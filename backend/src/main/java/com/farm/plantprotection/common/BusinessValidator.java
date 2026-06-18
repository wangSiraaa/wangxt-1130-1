package com.farm.plantprotection.common;

import cn.hutool.core.date.DateUtil;
import com.farm.plantprotection.entity.*;
import com.farm.plantprotection.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
public class BusinessValidator {

    @Resource
    private PesticideMapper pesticideMapper;

    @Resource
    private SafetyIntervalConfigMapper safetyIntervalConfigMapper;

    @Resource
    private FlightOperationMapper flightOperationMapper;

    @Resource
    private FarmPlotMapper farmPlotMapper;

    @Resource
    private PesticideStockMapper pesticideStockMapper;

    public void validatePesticideNotForbidden(Long pesticideId) {
        Pesticide pesticide = pesticideMapper.selectById(pesticideId);
        if (pesticide == null) {
            throw new BusinessException("农药不存在");
        }
        if (pesticide.getIsForbidden() != null && pesticide.getIsForbidden() == 1) {
            throw new BusinessException(400, "农药【" + pesticide.getPesticideName() + "】为禁用农药，禁止出库使用！禁用原因：" + pesticide.getForbiddenReason());
        }
    }

    public void validatePesticideStock(Long pesticideId, String batchNo, BigDecimal requiredQuantity) {
        if (pesticideId == null || batchNo == null) {
            return;
        }
        PesticideStock stock = pesticideStockMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PesticideStock>()
                        .eq(PesticideStock::getPesticideId, pesticideId)
                        .eq(PesticideStock::getBatchNo, batchNo)
        );
        if (stock == null) {
            throw new BusinessException("库存中不存在该批号的农药");
        }
        if (stock.getQuantity().compareTo(requiredQuantity) < 0) {
            throw new BusinessException(400, "农药库存不足！批号【" + batchNo + "】库存：" + stock.getQuantity() + stock.getUnit() + "，需要：" + requiredQuantity + stock.getUnit());
        }
        if (stock.getExpiryDate() != null && stock.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "农药【批号：" + batchNo + "】已过期，有效期至：" + stock.getExpiryDate());
        }
    }

    public void validateWindSpeed(Long pesticideId, BigDecimal windSpeed) {
        if (windSpeed == null) {
            throw new BusinessException("风速不能为空");
        }
        Pesticide pesticide = pesticideMapper.selectById(pesticideId);
        if (pesticide != null && pesticide.getMaxWindSpeed() != null) {
            if (windSpeed.compareTo(pesticide.getMaxWindSpeed()) > 0) {
                throw new BusinessException(400, "风速超限！当前风速" + windSpeed + "m/s，超过该农药【" + pesticide.getPesticideName() + "】最大允许风速" + pesticide.getMaxWindSpeed() + "m/s，禁止飞行作业！");
            }
        } else {
            BigDecimal defaultMaxWind = new BigDecimal("5.0");
            if (windSpeed.compareTo(defaultMaxWind) > 0) {
                throw new BusinessException(400, "风速超限！当前风速" + windSpeed + "m/s，超过植保无人机通用安全作业风速5.0m/s，禁止飞行作业！");
            }
        }
    }

    public void validateOperationWeather(String weather, BigDecimal temperature, BigDecimal humidity) {
        if (weather != null) {
            String[] forbiddenWeathers = {"大雨", "暴雨", "雷阵雨", "暴雪", "大雾", "沙尘暴"};
            for (String fw : forbiddenWeathers) {
                if (weather.contains(fw)) {
                    throw new BusinessException(400, "天气条件不适合作业！当前天气：" + weather);
                }
            }
            if (weather.contains("雨")) {
                throw new BusinessException(400, "雨天禁止进行植保作业！当前天气：" + weather + "，喷施农药后会被雨水冲刷失效");
            }
        }
        if (temperature != null) {
            if (temperature.compareTo(new BigDecimal("5")) < 0) {
                throw new BusinessException(400, "温度过低！当前温度" + temperature + "℃，低于5℃时农药活性降低，不建议作业");
            }
            if (temperature.compareTo(new BigDecimal("35")) > 0) {
                throw new BusinessException(400, "温度过高！当前温度" + temperature + "℃，超过35℃时农药易蒸发且存在高温药害风险");
            }
        }
        if (humidity != null && humidity.compareTo(new BigDecimal("90")) > 0) {
            throw new BusinessException(400, "湿度过高！当前湿度" + humidity + "%，高湿条件下雾滴不易沉降且易产生药害");
        }
    }

    public void validateSafetyInterval(Long plotId, Long pesticideId, LocalDate operationDate) {
        if (plotId == null || pesticideId == null) {
            return;
        }
        FarmPlot plot = farmPlotMapper.selectById(plotId);
        if (plot == null) {
            return;
        }

        SafetyIntervalConfig config = safetyIntervalConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SafetyIntervalConfig>()
                        .eq(SafetyIntervalConfig::getPesticideId, pesticideId)
                        .eq(SafetyIntervalConfig::getCropType, plot.getCropType())
                        .eq(SafetyIntervalConfig::getStatus, 1)
        );

        if (config == null) {
            Pesticide pesticide = pesticideMapper.selectById(pesticideId);
            if (pesticide != null && pesticide.getSafetyInterval() != null && pesticide.getSafetyInterval() > 0) {
                int intervalDays = pesticide.getSafetyInterval();
                checkLastOperationInterval(plotId, operationDate, intervalDays, pesticide.getPesticideName());
            }
        } else {
            checkLastOperationInterval(plotId, operationDate, config.getIntervalDays(), config.getPesticideName());
        }
    }

    private void checkLastOperationInterval(Long plotId, LocalDate operationDate, int intervalDays, String pesticideName) {
        List<FlightOperation> recentOperations = flightOperationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FlightOperation>()
                        .eq(FlightOperation::getPlotId, plotId)
                        .eq(FlightOperation::getOperationStatus, "COMPLETED")
                        .ge(FlightOperation::getOperationDate, operationDate.minusDays(intervalDays * 2L))
                        .orderByDesc(FlightOperation::getOperationDate)
        );

        for (FlightOperation op : recentOperations) {
            long daysBetween = ChronoUnit.DAYS.between(op.getOperationDate(), operationDate);
            if (daysBetween < intervalDays) {
                int remainingDays = intervalDays - (int) daysBetween;
                throw new BusinessException(400,
                        "安全间隔期校验失败！该地块【" + op.getPlotName() + "】于" +
                        DateUtil.format(DateUtil.date(op.getOperationDate()), "yyyy-MM-dd") +
                        "进行过施药作业，距安全间隔期" + intervalDays + "天还差" + remainingDays + "天，期间禁止重复施药/采收！" +
                        "（上次施用农药涉及：" + pesticideName + "）");
            }
        }
    }

    public boolean isInSafetyInterval(Long plotId, LocalDate checkDate) {
        if (plotId == null) {
            return false;
        }

        List<FlightOperation> operations = flightOperationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FlightOperation>()
                        .eq(FlightOperation::getPlotId, plotId)
                        .eq(FlightOperation::getOperationStatus, "COMPLETED")
                        .ge(FlightOperation::getOperationDate, checkDate.minusDays(60))
                        .orderByDesc(FlightOperation::getOperationDate)
        );

        FarmPlot plot = farmPlotMapper.selectById(plotId);
        if (plot == null) {
            return false;
        }

        for (FlightOperation op : operations) {
            String[] pesticideIds = parsePesticideIdsFromOperation(op);
            for (String pidStr : pesticideIds) {
                try {
                    Long pid = Long.parseLong(pidStr.trim());
                    int interval = getIntervalDays(pid, plot.getCropType());
                    long daysBetween = ChronoUnit.DAYS.between(op.getOperationDate(), checkDate);
                    if (daysBetween < interval) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    private String[] parsePesticideIdsFromOperation(FlightOperation op) {
        return new String[]{};
    }

    private int getIntervalDays(Long pesticideId, String cropType) {
        SafetyIntervalConfig config = safetyIntervalConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SafetyIntervalConfig>()
                        .eq(SafetyIntervalConfig::getPesticideId, pesticideId)
                        .eq(SafetyIntervalConfig::getCropType, cropType)
                        .eq(SafetyIntervalConfig::getStatus, 1)
        );
        if (config != null) {
            return config.getIntervalDays();
        }
        Pesticide pesticide = pesticideMapper.selectById(pesticideId);
        if (pesticide != null && pesticide.getSafetyInterval() != null) {
            return pesticide.getSafetyInterval();
        }
        return 7;
    }

    public void validatePlotHarvestable(Long plotId, LocalDate harvestDate) {
        if (plotId == null) {
            return;
        }

        List<FlightOperation> operations = flightOperationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FlightOperation>()
                        .eq(FlightOperation::getPlotId, plotId)
                        .eq(FlightOperation::getOperationStatus, "COMPLETED")
                        .ge(FlightOperation::getOperationDate, harvestDate.minusDays(60))
                        .orderByDesc(FlightOperation::getOperationDate)
        );

        if (operations.isEmpty()) {
            return;
        }

        FarmPlot plot = farmPlotMapper.selectById(plotId);
        StringBuilder violationMsg = new StringBuilder();

        for (FlightOperation op : operations) {
            String[] pesticideIds = parsePesticideIdsFromOperation(op);
            for (String pidStr : pesticideIds) {
                try {
                    Long pid = Long.parseLong(pidStr.trim());
                    int interval = getIntervalDays(pid, plot != null ? plot.getCropType() : null);
                    long daysBetween = ChronoUnit.DAYS.between(op.getOperationDate(), harvestDate);
                    if (daysBetween < interval) {
                        int remaining = interval - (int) daysBetween;
                        Pesticide pesticide = pesticideMapper.selectById(pid);
                        String pname = pesticide != null ? pesticide.getPesticideName() : "未知农药";
                        violationMsg.append(String.format(
                                "地块【%s】于%s施用%s，安全间隔期%d天，当前距采收仅%d天，还差%d天才能采收！%n",
                                plot != null ? plot.getPlotName() : "未知地块",
                                DateUtil.format(DateUtil.date(op.getOperationDate()), "yyyy-MM-dd"),
                                pname, interval, daysBetween, remaining));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (violationMsg.length() > 0) {
            throw new BusinessException(400, "采收安全间隔期校验未通过！\n" + violationMsg.toString());
        }
    }
}
