package com.farm.plantprotection.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.farm.plantprotection.common.Result;
import com.farm.plantprotection.entity.HarvestPlan;
import com.farm.plantprotection.service.HarvestPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/harvest-plan")
public class HarvestPlanController {

    @Resource
    private HarvestPlanService harvestPlanService;

    @PostMapping("/create")
    public Result<HarvestPlan> create(@RequestBody HarvestPlan plan) {
        return Result.success(harvestPlanService.createHarvestPlan(plan));
    }

    @GetMapping("/{id}")
    public Result<HarvestPlan> getDetail(@PathVariable Long id) {
        return Result.success(harvestPlanService.getDetail(id));
    }

    @GetMapping("/page")
    public Result<IPage<HarvestPlan>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Long plotId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false, defaultValue = "false") Boolean lockedOnly) {
        return Result.success(harvestPlanService.queryPage(pageNum, pageSize, plotId, status, lockedOnly));
    }

    @PostMapping("/complete/{id}")
    public Result<HarvestPlan> complete(@PathVariable Long id, @RequestParam Long operatorId) {
        return Result.success(harvestPlanService.completeHarvest(id, operatorId));
    }

    @PostMapping("/cancel/{id}")
    public Result<HarvestPlan> cancel(@PathVariable Long id) {
        return Result.success(harvestPlanService.cancelHarvest(id));
    }

    @PostMapping("/unlock-expired")
    public Result<Void> unlockExpiredPlans() {
        harvestPlanService.unlockExpiredPlans();
        return Result.success();
    }
}
