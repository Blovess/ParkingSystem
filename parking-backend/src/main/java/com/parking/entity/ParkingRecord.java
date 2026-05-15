package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("parking_record")
public class ParkingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String plateNumber;
    private Long spaceId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer status;

    @TableField(exist = false)
    private BigDecimal amount;
}
