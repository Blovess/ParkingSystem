package com.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.entity.GraphVertex;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GraphVertexMapper extends BaseMapper<GraphVertex> {
}
