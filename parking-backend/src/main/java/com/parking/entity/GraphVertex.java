package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("graph_vertex")
public class GraphVertex {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Double x;
    private Double y;
}
