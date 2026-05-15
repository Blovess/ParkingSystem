package com.parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.dto.Result;
import com.parking.entity.ParkingOrder;
import com.parking.entity.ParkingRecord;
import com.parking.entity.ParkingSpace;
import com.parking.service.ParkingOrderService;
import com.parking.service.ParkingRecordService;
import com.parking.service.ParkingSpaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private static final Logger log = LoggerFactory.getLogger(ParkingController.class);

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @Autowired
    private ParkingRecordService parkingRecordService;

    @Autowired
    private ParkingOrderService parkingOrderService;

    @PostMapping("/entry")
    @Transactional
    public Result entry(@RequestBody Map<String, Object> body) {
        String plateNumber = (String) body.get("plateNumber");
        if (plateNumber == null) {
            return Result.error(400, "缺少车牌号");
        }

        LambdaQueryWrapper<ParkingRecord> dupQw = new LambdaQueryWrapper<>();
        dupQw.eq(ParkingRecord::getPlateNumber, plateNumber)
             .eq(ParkingRecord::getStatus, 0);
        if (parkingRecordService.count(dupQw) > 0) {
            return Result.error(400, "该车辆已在停车场内，无法重复入场");
        }

        ParkingRecord record = new ParkingRecord();
        record.setPlateNumber(plateNumber);
        record.setSpaceId(null);
        record.setEntryTime(LocalDateTime.now());
        record.setStatus(0);
        parkingRecordService.save(record);

        ParkingOrder order = new ParkingOrder();
        order.setRecordId(record.getId());
        order.setAmount(new BigDecimal("0"));
        order.setPayStatus(0);
        parkingOrderService.save(order);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", record.getId());
        data.put("orderId", order.getId());
        data.put("message", "入场登记成功，请选择车位");
        return Result.ok(data);
    }

    @PutMapping("/assign-space")
    @Transactional
    public Result assignSpace(@RequestBody Map<String, Object> body) {
        Object recordIdObj = body.get("recordId");
        Object spaceIdObj = body.get("spaceId");
        if (recordIdObj == null || spaceIdObj == null) {
            return Result.error(400, "缺少记录ID或车位ID");
        }
        Long recordId = Long.valueOf(recordIdObj.toString());
        Long spaceId = Long.valueOf(spaceIdObj.toString());

        ParkingRecord record = parkingRecordService.getById(recordId);
        if (record == null) {
            return Result.error(400, "停车记录不存在");
        }
        if (record.getStatus() != 0) {
            return Result.error(400, "该记录已结束");
        }
        if (record.getSpaceId() != null) {
            return Result.error(400, "该记录已分配车位");
        }

        ParkingSpace space = parkingSpaceService.getById(spaceId);
        if (space == null) {
            return Result.error(400, "车位不存在");
        }
        if (space.getStatus() == 1) {
            return Result.error(400, "车位已被占用");
        }

        record.setSpaceId(spaceId);
        parkingRecordService.updateById(record);

        space.setStatus(1);
        parkingSpaceService.updateById(space);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", record.getId());
        data.put("spaceId", spaceId);
        data.put("plateNumber", record.getPlateNumber());
        return Result.ok(data);
    }

    @PostMapping("/exit")
    @Transactional
    public Result exit(@RequestBody Map<String, Object> body) {
        String plateNumber = (String) body.get("plateNumber");
        if (plateNumber == null) {
            return Result.error(400, "缺少车牌号");
        }
        log.info("[离场] 车牌={} 开始离场流程", plateNumber);

        LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(ParkingRecord::getPlateNumber, plateNumber)
          .eq(ParkingRecord::getStatus, 0)
          .orderByDesc(ParkingRecord::getId)
          .last("LIMIT 1");
        ParkingRecord record = parkingRecordService.getOne(qw);
        if (record == null) {
            log.warn("[离场] 车牌={} 未找到进行中的停车记录", plateNumber);
            return Result.error(400, "未找到进行中的停车记录");
        }
        log.info("[离场] 找到记录 recordId={} spaceId={}", record.getId(), record.getSpaceId());

        ParkingOrder order = parkingOrderService.findByRecordId(record.getId());
        if (order == null || order.getPayStatus() != 1) {
            log.warn("[离场] recordId={} 订单未支付 payStatus={}", record.getId(),
                    order != null ? order.getPayStatus() : "null");
            return Result.error(400, "请先进行缴费");
        }
        log.info("[离场] recordId={} 订单已支付 orderId={}", record.getId(), order.getId());

        LocalDateTime now = LocalDateTime.now();
        long minutes = Duration.between(record.getEntryTime(), now).toMinutes();
        BigDecimal amount;
        if (minutes <= 30) {
            amount = BigDecimal.ZERO;
        } else {
            long hours = (long) Math.ceil(minutes / 60.0);
            amount = new BigDecimal(hours * 3);
        }
        order.setAmount(amount);
        parkingOrderService.updateById(order);
        log.info("[离场] recordId={} 停车{}分钟 计费{}元", record.getId(), minutes, amount);

        record.setStatus(1);
        record.setExitTime(now);
        boolean recordOk = parkingRecordService.updateById(record);
        log.info("[离场] 更新停车记录 recordId={} status=1 exitTime={} result={}",
                record.getId(), record.getExitTime(), recordOk);

        ParkingSpace space = parkingSpaceService.getById(record.getSpaceId());
        if (space != null) {
            int oldStatus = space.getStatus();
            space.setStatus(0);
            boolean spaceOk = parkingSpaceService.updateById(space);
            log.info("[离场] 释放车位 spaceCode={} status {}→0 result={}",
                    space.getSpaceCode(), oldStatus, spaceOk);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "缴费成功，一路顺风");
        log.info("[离场] 车牌={} 离场完成", plateNumber);
        return Result.ok(data);
    }

    @PostMapping("/route")
    public Result route(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> route = new ArrayList<>();
        Map<String, Object> p1 = new HashMap<>(); p1.put("x", 0); p1.put("y", 0); route.add(p1);
        Map<String, Object> p2 = new HashMap<>(); p2.put("x", 100); p2.put("y", 50); route.add(p2);
        Map<String, Object> p3 = new HashMap<>(); p3.put("x", 120); p3.put("y", 80); route.add(p3);
        return Result.ok(route);
    }
}
