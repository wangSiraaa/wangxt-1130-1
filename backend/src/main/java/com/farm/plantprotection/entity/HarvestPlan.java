package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("harvest_plan")
public class HarvestPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;

    private Long plotId;

    private String plotName;

    private String cropType;

    private String cropVariety;

    private LocalDate plannedHarvestDate;

    private BigDecimal plannedYield;

    private String harvestMethod;

    private String status;

    private Integer isLocked;

    private String lockReason;

    private LocalDate lockExpireDate;

    private String remark;

    private Long createBy;

    private String createByName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
