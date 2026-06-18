package com.farm.plantprotection.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.farm.plantprotection.common.Result;
import com.farm.plantprotection.entity.*;
import com.farm.plantprotection.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private CommonService commonService;

    @GetMapping("/plots")
    public Result<List<FarmPlot>> listPlots(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(commonService.listPlots(keyword, status));
    }

    @GetMapping("/pesticides")
    public Result<List<Pesticide>> listPesticides(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer isForbidden,
                                                  @RequestParam(required = false) Integer status) {
        return Result.success(commonService.listPesticides(keyword, isForbidden, status));
    }

    @GetMapping("/stocks")
    public Result<IPage<PesticideStock>> queryStocks(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) Long pesticideId,
                                                     @RequestParam(required = false) String batchNo,
                                                     @RequestParam(required = false) String warehouseName) {
        return Result.success(commonService.queryStockPage(pageNum, pageSize, pesticideId, batchNo, warehouseName));
    }

    @GetMapping("/interval-configs")
    public Result<List<SafetyIntervalConfig>> listIntervalConfigs(@RequestParam(required = false) String cropType,
                                                                   @RequestParam(required = false) Long pesticideId) {
        return Result.success(commonService.listIntervalConfigs(cropType, pesticideId));
    }

    @GetMapping("/users")
    public Result<List<User>> listUsersByRole(@RequestParam(required = false) String roleCode) {
        return Result.success(commonService.listUsersByRole(roleCode));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(commonService.getDashboardStats());
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        return Result.success(commonService.login(username, password));
    }
}
