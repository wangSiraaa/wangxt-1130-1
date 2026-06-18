package com.farm.plantprotection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("safety_interval_reminder")
public class SafetyIntervalReminder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reminderNo;

    private Long plotId;

    private String plotName;

    private Long pesticideId;

    private String pesticideName;

    private Long lastOperationId;

    private LocalDate lastOperationDate;

    private LocalDate safeEndDate;

    private Integer intervalDays;

    private Integer remainingDays;

    private String reminderType;

    private String reminderLevel;

    private String remindContent;

    private Integer isRead;

    private LocalDateTime readTime;

    private Long readBy;

    private String notifyUsers;

    private LocalDateTime reminderTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
