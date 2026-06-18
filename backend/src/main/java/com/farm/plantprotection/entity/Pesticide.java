package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pesticide")
public class Pesticide implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String pesticideCode;

    private String pesticideName;

    private String commonName;

    private String registrationNo;

    private String manufacturer;

    private String pesticideType;

    private String toxicity;

    private String specification;

    private String unit;

    private Integer isForbidden;

    private String forbiddenReason;

    private Integer safetyInterval;

    private BigDecimal maxWindSpeed;

    private String applicableCrops;

    private String controlObjects;

    private String dosage;

    private String usageMethod;

    private String precautions;

    private Integer status;

    private String remark;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
