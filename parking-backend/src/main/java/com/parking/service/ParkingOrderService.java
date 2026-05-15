package com.parking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.entity.ParkingOrder;
import com.parking.mapper.ParkingOrderMapper;
import org.springframework.stereotype.Service;

@Service
public class ParkingOrderService extends ServiceImpl<ParkingOrderMapper, ParkingOrder> {

    public ParkingOrder findByRecordId(Long recordId) {
        LambdaQueryWrapper<ParkingOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(ParkingOrder::getRecordId, recordId);
        return getOne(qw);
    }
}
