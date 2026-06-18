package com.farm.plantprotection.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.farm.plantprotection.common.Result;
import com.farm.plantprotection.entity.FlightOperation;
import com.farm.plantprotection.entity.SafetyIntervalReminder;
import com.farm.plantprotection.service.FlightOperationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/flight")
public class FlightOperationController {

    @Resource
    private FlightOperationService flightOperationService;

    @PostMapping("/pre-check")
    public Result<Map<String, Object>> preCheck(@RequestParam Long outboundId,
                                                @RequestParam(required = false) LocalDate operationDate,
                                                @RequestParam(required = false) BigDecimal windSpeed) {
        return Result.success(flightOperationService.preCheckSafety(outboundId, operationDate, windSpeed));
    }

    @PostMapping("/create")
    public Result<FlightOperation> create(@RequestBody Map<String, Object> params) {
        Long outboundId = Long.valueOf(params.get("outboundId").toString());
        Long pilotId = Long.valueOf(params.get("pilotId").toString());
        String pilotName = params.get("pilotName") != null ? params.get("pilotName").toString() : "";

        FlightOperation operation = new FlightOperation();
        if (params.get("operationDate") != null) {
            operation.setOperationDate(LocalDate.parse(params.get("operationDate").toString()));
        }
        if (params.get("operationArea") != null) {
            operation.setOperationArea(new BigDecimal(params.get("operationArea").toString()));
        }
        if (params.get("droneNo") != null) operation.setDroneNo(params.get("droneNo").toString());
        if (params.get("weather") != null) operation.setWeather(params.get("weather").toString());
        if (params.get("temperature") != null) operation.setTemperature(new BigDecimal(params.get("temperature").toString()));
        if (params.get("humidity") != null) operation.setHumidity(new BigDecimal(params.get("humidity").toString()));
        if (params.get("windDirection") != null) operation.setWindDirection(params.get("windDirection").toString());
        if (params.get("windSpeed") != null) operation.setWindSpeed(new BigDecimal(params.get("windSpeed").toString()));
        if (params.get("windLevel") != null) operation.setWindLevel(params.get("windLevel").toString());
        if (params.get("sprayingVolume") != null) operation.setSprayingVolume(new BigDecimal(params.get("sprayingVolume").toString()));
        if (params.get("flightHeight") != null) operation.setFlightHeight(new BigDecimal(params.get("flightHeight").toString()));
        if (params.get("flightSpeed") != null) operation.setFlightSpeed(new BigDecimal(params.get("flightSpeed").toString()));
        if (params.get("sprayWidth") != null) operation.setSprayWidth(new BigDecimal(params.get("sprayWidth").toString()));
        if (params.get("operationStatus") != null) operation.setOperationStatus(params.get("operationStatus").toString());
        if (params.get("safetyCheck") != null) operation.setSafetyCheck(Integer.valueOf(params.get("safetyCheck").toString()));
        if (params.get("safetyRemark") != null) operation.setSafetyRemark(params.get("safetyRemark").toString());
        if (params.get("startTime") != null) operation.setStartTime(java.time.LocalDateTime.parse(params.get("startTime").toString()));
        if (params.get("remark") != null) operation.setRemark(params.get("remark").toString());

        return Result.success(flightOperationService.createOperation(outboundId, pilotId, pilotName, operation));
    }

    @PostMapping("/complete/{id}")
    public Result<FlightOperation> complete(@PathVariable Long id, @RequestBody FlightOperation updateData) {
        return Result.success(flightOperationService.completeOperation(id, updateData));
    }

    @GetMapping("/{id}")
    public Result<FlightOperation> getDetail(@PathVariable Long id) {
        return Result.success(flightOperationService.getDetail(id));
    }

    @GetMapping("/page")
    public Result<IPage<FlightOperation>> page(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long plotId,
                                               @RequestParam(required = false) Long pilotId) {
        return Result.success(flightOperationService.queryPage(pageNum, pageSize, status, plotId, pilotId));
    }

    @GetMapping("/reminders")
    public Result<IPage<SafetyIntervalReminder>> reminders(@RequestParam(defaultValue = "1") int pageNum,
                                                           @RequestParam(defaultValue = "10") int pageSize,
                                                           @RequestParam(required = false) Long plotId,
                                                           @RequestParam(required = false) String reminderType,
                                                           @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly) {
        return Result.success(flightOperationService.queryReminders(pageNum, pageSize, plotId, reminderType, unreadOnly));
    }

    @GetMapping("/plot-warnings/{plotId}")
    public Result<List<SafetyIntervalReminder>> getPlotWarnings(@PathVariable(required = false) Long plotId) {
        return Result.success(flightOperationService.getActivePlotWarnings(plotId));
    }

    @PostMapping("/reminders/{id}/read")
    public Result<Void> markReminderRead(@PathVariable Long id, @RequestParam Long userId) {
        flightOperationService.markReminderRead(id, userId);
        return Result.success();
    }
}
