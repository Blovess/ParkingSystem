package com.parking.controller;

import com.parking.dto.Result;
import com.parking.entity.ParkingOrder;
import com.parking.service.ParkingOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private ParkingOrderService parkingOrderService;

    @PutMapping("/pay/{recordId}")
    public Result pay(@PathVariable Long recordId) {
        ParkingOrder order = parkingOrderService.findByRecordId(recordId);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        if (order.getPayStatus() == 1) {
            return Result.error(400, "订单已支付");
        }
        order.setPayStatus(1);
        parkingOrderService.updateById(order);
        return Result.ok();
    }
}
