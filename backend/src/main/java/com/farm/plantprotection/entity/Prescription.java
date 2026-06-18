package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("prescription")
public class Prescription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String prescriptionNo;

    private Long plotId;

    private String plotName;

    private String pestType;

    private String pestName;

    private String severity;

    private BigDecimal occurrenceArea;

    private String symptomDescription;

    private String diagnosisBasis;

    private String treatmentScheme;

    private String expectedEffect;

    private LocalDate prescriptionDate;

    private Long agronomistId;

    private String agronomistName;

    private String status;

    private Long approveBy;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private List<PrescriptionDetail> details;
}
