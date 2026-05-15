package com.parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.Result;
import com.parking.entity.SysUser;
import com.parking.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            qw.like(SysUser::getUsername, username);
        }
        qw.orderByDesc(SysUser::getId);
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> result = sysUserService.page(page, qw);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.ok(data);
    }

    @PostMapping
    public Result add(@RequestBody SysUser user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error(400, "用户名和密码不能为空");
        }
        SysUser exist = sysUserService.findByUsername(user.getUsername());
        if (exist != null) {
            return Result.error(400, "用户名已存在");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        sysUserService.save(user);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser dbUser = sysUserService.getById(id);
        if (dbUser == null) {
            return Result.error(400, "用户不存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            user.setPassword(encoder.encode(user.getPassword()));
        }
        user.setId(id);
        sysUserService.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.ok();
    }
}
