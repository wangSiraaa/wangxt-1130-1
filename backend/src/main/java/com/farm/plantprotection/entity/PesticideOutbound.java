package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("pesticide_outbound")
public class PesticideOutbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String outboundNo;

    private Long prescriptionId;

    private String prescriptionNo;

    private Long plotId;

    private String plotName;

    private String outboundType;

    private LocalDate outboundDate;

    private Long warehouseKeeperId;

    private String warehouseKeeperName;

    private Long receiverId;

    private String receiverName;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime outboundTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private List<PesticideOutboundDetail> details;
}
