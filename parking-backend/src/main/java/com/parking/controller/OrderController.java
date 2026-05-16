package com.parking.controller;

import com.parking.dto.Result;
import com.parking.entity.ParkingOrder;
import com.parking.entity.ParkingRecord;
import com.parking.service.ParkingOrderService;
import com.parking.service.ParkingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private ParkingOrderService parkingOrderService;

    @Autowired
    private ParkingRecordService parkingRecordService;

    @PutMapping("/pay/{recordId}")
    public Result pay(@PathVariable Long recordId) {
        ParkingOrder order = parkingOrderService.findByRecordId(recordId);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        if (order.getPayStatus() == 1) {
            return Result.error(400, "订单已支付");
        }

        // Recalculate amount based on actual parking duration
        ParkingRecord record = parkingRecordService.getById(recordId);
        if (record != null && record.getEntryTime() != null) {
            long minutes = Duration.between(record.getEntryTime(), LocalDateTime.now()).toMinutes();
            if (minutes <= 30) {
                order.setAmount(BigDecimal.ZERO);
            } else {
                long hours = (long) Math.ceil(minutes / 60.0);
                order.setAmount(new BigDecimal(hours * 3));
            }
        }

        order.setPayStatus(1);
        parkingOrderService.updateById(order);
        return Result.ok();
    }
}
