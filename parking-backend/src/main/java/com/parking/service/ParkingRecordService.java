package com.parking.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.entity.ParkingRecord;
import com.parking.mapper.ParkingRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class ParkingRecordService extends ServiceImpl<ParkingRecordMapper, ParkingRecord> {
}
