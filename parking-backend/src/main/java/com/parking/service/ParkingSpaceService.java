package com.parking.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.entity.ParkingSpace;
import com.parking.mapper.ParkingSpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class ParkingSpaceService extends ServiceImpl<ParkingSpaceMapper, ParkingSpace> {
}
