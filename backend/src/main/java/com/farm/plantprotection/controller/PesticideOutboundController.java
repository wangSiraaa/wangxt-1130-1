package com.farm.plantprotection.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.farm.plantprotection.common.Result;
import com.farm.plantprotection.entity.PesticideOutbound;
import com.farm.plantprotection.entity.PesticideOutboundDetail;
import com.farm.plantprotection.entity.PesticideStock;
import com.farm.plantprotection.entity.PesticideStockPending;
import com.farm.plantprotection.service.PesticideOutboundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/outbound")
public class PesticideOutboundController {

    @Resource
    private PesticideOutboundService outboundService;

    @PostMapping("/create-from-prescription")
    public Result<PesticideOutbound> createFromPrescription(@RequestParam Long prescriptionId,
                                                            @RequestParam Long warehouseKeeperId,
                                                            @RequestParam String warehouseKeeperName) {
        return Result.success(outboundService.createOutboundFromPrescription(prescriptionId, warehouseKeeperId, warehouseKeeperName));
    }

    @PostMapping("/confirm/{outboundId}")
    public Result<PesticideOutbound> confirmOutbound(@PathVariable Long outboundId,
                                                     @RequestBody Map<String, Object> params) {
        List<PesticideOutboundDetail> details = (List<PesticideOutboundDetail>) params.get("details");
        Long receiverId = params.get("receiverId") != null ? Long.valueOf(params.get("receiverId").toString()) : null;
        String receiverName = params.get("receiverName") != null ? params.get("receiverName").toString() : null;
        return Result.success(outboundService.confirmOutbound(outboundId, details, receiverId, receiverName));
    }

    @GetMapping("/{id}")
    public Result<PesticideOutbound> getDetail(@PathVariable Long id) {
        return Result.success(outboundService.getDetail(id));
    }

    @GetMapping("/page")
    public Result<IPage<PesticideOutbound>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long plotId) {
        return Result.success(outboundService.queryPage(pageNum, pageSize, status, plotId));
    }

    @GetMapping("/pending-operations/{pilotId}")
    public Result<List<PesticideOutbound>> getPendingForPilot(@PathVariable Long pilotId) {
        return Result.success(outboundService.getPendingOperationsForPilot(pilotId));
    }

    @GetMapping("/stock/{pesticideId}")
    public Result<List<PesticideStock>> getStockByPesticide(@PathVariable Long pesticideId) {
        return Result.success(outboundService.getStockByPesticide(pesticideId));
    }

    @PostMapping("/cancel-flight/{outboundId}")
    public Result<Void> cancelFlightToPending(@PathVariable Long outboundId,
                                               @RequestParam(required = false) String cancelReason) {
        outboundService.cancelFlightToPending(outboundId, cancelReason);
        return Result.success();
    }

    @GetMapping("/pending-stock/page")
    public Result<IPage<PesticideStockPending>> queryPendingStock(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(outboundService.queryPendingPage(pageNum, pageSize, status));
    }

    @PostMapping("/pending-stock/{pendingId}/verify")
    public Result<Void> verifyPendingStock(@PathVariable Long pendingId,
                                            @RequestParam Long verifyBy,
                                            @RequestParam String verifyByName,
                                            @RequestParam boolean approved,
                                            @RequestParam(required = false) String verifyRemark) {
        outboundService.verifyPendingStock(pendingId, verifyBy, verifyByName, approved, verifyRemark);
        return Result.success();
    }
}
