package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("safety_interval_config")
public class SafetyIntervalConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    private Long pesticideId;

    private String pesticideName;

    private String cropType;

    private Integer intervalDays;

    private BigDecimal maxWindSpeed;

    private BigDecimal minTemperature;

    private BigDecimal maxTemperature;

    private BigDecimal minHumidity;

    private BigDecimal maxHumidity;

    private Integer forbiddenRain;

    private String description;

    private Integer status;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
