package com.parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.dto.Result;
import com.parking.entity.ParkingRecord;
import com.parking.entity.ParkingSpace;
import com.parking.service.ParkingRecordService;
import com.parking.service.ParkingSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/space")
public class SpaceController {

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @Autowired
    private ParkingRecordService parkingRecordService;

    @GetMapping("/list")
    public Result list() {
        return Result.ok(parkingSpaceService.list());
    }

    @PostMapping
    public Result add(@RequestBody ParkingSpace space) {
        parkingSpaceService.save(space);
        return Result.ok(space);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody ParkingSpace space) {
        ParkingSpace existing = parkingSpaceService.getById(id);
        if (existing == null) {
            return Result.error(400, "车位不存在");
        }

        int oldStatus = existing.getStatus();
        int newStatus = space.getStatus();

        if (oldStatus == 1 && newStatus == 0) {
            LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
            qw.eq(ParkingRecord::getSpaceId, id)
              .eq(ParkingRecord::getStatus, 0);
            if (parkingRecordService.count(qw) > 0) {
                return Result.error(400, "该车位当前有车辆停放（停车记录进行中），无法手动释放。请先完成离场或缴费。");
            }
        }

        if (oldStatus == 0 && newStatus == 1) {
            LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
            qw.eq(ParkingRecord::getSpaceId, id)
              .eq(ParkingRecord::getStatus, 0);
            if (parkingRecordService.count(qw) > 0) {
                return Result.error(400, "该车位已被进行中的停车记录占用，无法手动设为占用");
            }
        }

        space.setId(id);
        parkingSpaceService.updateById(space);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        ParkingSpace space = parkingSpaceService.getById(id);
        if (space == null) {
            return Result.error(400, "车位不存在");
        }
        if (space.getStatus() == 1) {
            return Result.error(400, "该车位当前被占用，无法删除");
        }
        LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(ParkingRecord::getSpaceId, id)
          .eq(ParkingRecord::getStatus, 0);
        if (parkingRecordService.count(qw) > 0) {
            return Result.error(400, "该车位有关联的进行中停车记录，无法删除");
        }
        parkingSpaceService.removeById(id);
        return Result.ok();
    }
}
