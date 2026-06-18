package com.farm.plantprotection.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.farm.plantprotection.common.Result;
import com.farm.plantprotection.entity.Prescription;
import com.farm.plantprotection.service.PrescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/prescription")
public class PrescriptionController {

    @Resource
    private PrescriptionService prescriptionService;

    @PostMapping("/create")
    public Result<Prescription> create(@RequestBody Prescription prescription) {
        return Result.success(prescriptionService.createPrescription(prescription));
    }

    @PostMapping("/submit/{id}")
    public Result<Prescription> submit(@PathVariable Long id,
                                       @RequestParam Long agronomistId,
                                       @RequestParam String agronomistName) {
        return Result.success(prescriptionService.submitPrescription(id, agronomistId, agronomistName));
    }

    @PostMapping("/approve/{id}")
    public Result<Prescription> approve(@PathVariable Long id,
                                        @RequestParam Long approveBy,
                                        @RequestParam(required = false) String approveRemark,
                                        @RequestParam(defaultValue = "true") boolean passed) {
        return Result.success(prescriptionService.approvePrescription(id, approveBy, approveRemark, passed));
    }

    @GetMapping("/{id}")
    public Result<Prescription> getDetail(@PathVariable Long id) {
        return Result.success(prescriptionService.getDetail(id));
    }

    @GetMapping("/page")
    public Result<IPage<Prescription>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long plotId,
                                            @RequestParam(required = false) Long agronomistId) {
        return Result.success(prescriptionService.queryPage(pageNum, pageSize, status, plotId, agronomistId));
    }

    @GetMapping("/approved/list")
    public Result<List<Prescription>> getApprovedForOutbound() {
        return Result.success(prescriptionService.getApprovedPrescriptionsForOutbound());
    }
}
