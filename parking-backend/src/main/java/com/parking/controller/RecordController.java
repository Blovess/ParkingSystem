package com.parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.Result;
import com.parking.entity.ParkingOrder;
import com.parking.entity.ParkingRecord;
import com.parking.service.ParkingOrderService;
import com.parking.service.ParkingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Autowired
    private ParkingRecordService parkingRecordService;

    @Autowired
    private ParkingOrderService parkingOrderService;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String plateNumber,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(plateNumber)) {
            qw.like(ParkingRecord::getPlateNumber, plateNumber);
        }
        if (status != null) {
            qw.eq(ParkingRecord::getStatus, status);
        }
        if (StringUtils.hasText(startTime)) {
            qw.ge(ParkingRecord::getEntryTime, startTime);
        }
        if (StringUtils.hasText(endTime)) {
            qw.le(ParkingRecord::getEntryTime, endTime);
        }
        qw.orderByDesc(ParkingRecord::getId);
        Page<ParkingRecord> page = new Page<>(pageNum, pageSize);
        Page<ParkingRecord> result = parkingRecordService.page(page, qw);
        for (ParkingRecord record : result.getRecords()) {
            ParkingOrder order = parkingOrderService.findByRecordId(record.getId());
            if (order != null) {
                // For in-progress records, calculate amount dynamically
                if (record.getStatus() == 0 && record.getEntryTime() != null) {
                    long minutes = Duration.between(record.getEntryTime(), LocalDateTime.now()).toMinutes();
                    if (minutes <= 30) {
                        record.setAmount(BigDecimal.ZERO);
                    } else {
                        long hours = (long) Math.ceil(minutes / 60.0);
                        record.setAmount(new BigDecimal(hours * 3));
                    }
                } else {
                    record.setAmount(order.getAmount());
                }
            } else {
                record.setAmount(null);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.ok(data);
    }
}
