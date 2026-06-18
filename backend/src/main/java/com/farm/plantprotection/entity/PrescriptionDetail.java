package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription_detail")
public class PrescriptionDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long prescriptionId;

    private Long pesticideId;

    private String pesticideCode;

    private String pesticideName;

    private BigDecimal dosagePerMu;

    private String unit;

    private BigDecimal totalQuantity;

    private String dilutionRatio;

    private String usageMethod;

    private Integer sprayingTimes;

    private Integer sprayingInterval;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
