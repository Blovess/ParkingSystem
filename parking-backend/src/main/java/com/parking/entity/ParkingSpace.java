package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("parking_space")
public class ParkingSpace {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String spaceCode;
    private String area;
    private Integer status;
    private Double xCoordinate;
    private Double yCoordinate;
}
