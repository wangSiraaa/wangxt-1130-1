package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pesticide_outbound_detail")
public class PesticideOutboundDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long outboundId;

    private Long prescriptionDetailId;

    private Long pesticideId;

    private String pesticideCode;

    private String pesticideName;

    private String batchNo;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private Long stockId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
