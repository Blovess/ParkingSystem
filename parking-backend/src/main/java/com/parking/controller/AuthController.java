package com.parking.controller;

import com.parking.dto.Result;
import com.parking.entity.SysUser;
import com.parking.service.SysUserService;
import com.parking.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }
        boolean match = false;
        try {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            match = encoder.matches(password, user.getPassword());
        } catch (Exception e) {
            match = false;
        }
        if (!match) {
            return Result.error(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.ok(data);
    }
}
