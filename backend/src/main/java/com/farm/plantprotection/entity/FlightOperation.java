package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("flight_operation")
public class FlightOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operationNo;

    private Long outboundId;

    private String outboundNo;

    private Long prescriptionId;

    private Long plotId;

    private String plotName;

    private Long pilotId;

    private String pilotName;

    private LocalDate operationDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer flightDuration;

    private BigDecimal operationArea;

    private String droneNo;

    private String weather;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private String windDirection;

    private BigDecimal windSpeed;

    private String windLevel;

    private BigDecimal sprayingVolume;

    private String pesticideUsage;

    private BigDecimal flightHeight;

    private BigDecimal flightSpeed;

    private BigDecimal sprayWidth;

    private String operationStatus;

    private Integer safetyCheck;

    private String safetyRemark;

    private String problemDescription;

    private String solution;

    private String effectEvaluation;

    private String photoUrls;

    private String videoUrl;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
