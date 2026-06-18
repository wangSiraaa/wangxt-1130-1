package com.farm.plantprotection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farm.plantprotection.common.BusinessException;
import com.farm.plantprotection.entity.*;
import com.farm.plantprotection.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommonService {

    @Resource
    private FarmPlotMapper farmPlotMapper;

    @Resource
    private PesticideMapper pesticideMapper;

    @Resource
    private PesticideStockMapper pesticideStockMapper;

    @Resource
    private SafetyIntervalConfigMapper safetyIntervalConfigMapper;

    @Resource
    private UserMapper userMapper;

    public List<FarmPlot> listPlots(String keyword, Integer status) {
        LambdaQueryWrapper<FarmPlot> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(FarmPlot::getPlotCode, keyword)
                    .or().like(FarmPlot::getPlotName, keyword)
                    .or().like(FarmPlot::getCropType, keyword));
        }
        if (status != null) {
            wrapper.eq(FarmPlot::getStatus, status);
        }
        wrapper.orderByAsc(FarmPlot::getPlotCode);
        return farmPlotMapper.selectList(wrapper);
    }

    public List<Pesticide> listPesticides(String keyword, Integer isForbidden, Integer status) {
        LambdaQueryWrapper<Pesticide> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Pesticide::getPesticideCode, keyword)
                    .or().like(Pesticide::getPesticideName, keyword)
                    .or().like(Pesticide::getCommonName, keyword)
                    .or().like(Pesticide::getPesticideType, keyword));
        }
        if (isForbidden != null) {
            wrapper.eq(Pesticide::getIsForbidden, isForbidden);
        }
        if (status != null) {
            wrapper.eq(Pesticide::getStatus, status);
        }
        wrapper.orderByAsc(Pesticide::getPesticideCode);
        return pesticideMapper.selectList(wrapper);
    }

    public IPage<PesticideStock> queryStockPage(int pageNum, int pageSize, Long pesticideId, String batchNo, String warehouseName) {
        Page<PesticideStock> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PesticideStock> wrapper = new LambdaQueryWrapper<>();
        if (pesticideId != null) {
            wrapper.eq(PesticideStock::getPesticideId, pesticideId);
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.like(PesticideStock::getBatchNo, batchNo);
        }
        if (warehouseName != null && !warehouseName.isEmpty()) {
            wrapper.like(PesticideStock::getWarehouseName, warehouseName);
        }
        wrapper.orderByAsc(PesticideStock::getPesticideId).orderByAsc(PesticideStock::getBatchNo);
        return pesticideStockMapper.selectPage(page, wrapper);
    }

    public List<SafetyIntervalConfig> listIntervalConfigs(String cropType, Long pesticideId) {
        LambdaQueryWrapper<SafetyIntervalConfig> wrapper = new LambdaQueryWrapper<>();
        if (cropType != null && !cropType.isEmpty()) {
            wrapper.eq(SafetyIntervalConfig::getCropType, cropType);
        }
        if (pesticideId != null) {
            wrapper.eq(SafetyIntervalConfig::getPesticideId, pesticideId);
        }
        wrapper.eq(SafetyIntervalConfig::getStatus, 1);
        wrapper.orderByAsc(SafetyIntervalConfig::getCropType);
        return safetyIntervalConfigMapper.selectList(wrapper);
    }

    public List<User> listUsersByRole(String roleCode) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (roleCode != null && !roleCode.isEmpty()) {
            wrapper.eq(User::getRoleCode, roleCode);
        }
        wrapper.eq(User::getStatus, 1);
        wrapper.orderByAsc(User::getRealName);
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("plotCount", farmPlotMapper.selectCount(null));
        stats.put("pesticideCount", pesticideMapper.selectCount(null));

        Long forbiddenCount = pesticideMapper.selectCount(
                new LambdaQueryWrapper<Pesticide>().eq(Pesticide::getIsForbidden, 1)
        );
        stats.put("forbiddenPesticideCount", forbiddenCount);

        long totalStock = 0;
        List<PesticideStock> stocks = pesticideStockMapper.selectList(null);
        for (PesticideStock s : stocks) {
            if (s.getQuantity() != null) {
                totalStock += s.getQuantity().longValue();
            }
        }
        stats.put("totalStockQty", totalStock);

        stats.put("agronomistCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRoleCode, "AGRONOMIST")));
        stats.put("warehouseCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRoleCode, "WAREHOUSE")));
        stats.put("pilotCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRoleCode, "PILOT")));

        return stats;
    }

    public User login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "用户已被禁用");
        }
        user.setPassword(null);
        return user;
    }
}
