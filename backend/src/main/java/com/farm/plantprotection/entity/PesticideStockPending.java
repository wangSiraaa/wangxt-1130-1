package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pesticide_stock_pending")
public class PesticideStockPending implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String pendingNo;

    private Long pesticideId;

    private String pesticideCode;

    private String pesticideName;

    private String batchNo;

    private BigDecimal quantity;

    private String unit;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private String pendingReason;

    private String status;

    private Long verifyBy;

    private String verifyByName;

    private LocalDateTime verifyTime;

    private String verifyRemark;

    private Long stockId;

    private String warehouseName;

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
