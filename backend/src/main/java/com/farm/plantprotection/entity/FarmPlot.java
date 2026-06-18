package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("farm_plot")
public class FarmPlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String plotCode;

    private String plotName;

    private String location;

    private BigDecimal area;

    private String cropType;

    private String cropVariety;

    private LocalDate plantingDate;

    private Integer status;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String remark;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
