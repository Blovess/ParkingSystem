package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("graph_edge")
public class GraphEdge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromVertexId;
    private Long toVertexId;
    private Double weight;
}
