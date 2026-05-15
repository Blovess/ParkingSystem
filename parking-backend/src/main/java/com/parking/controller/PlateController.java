package com.parking.controller;

import com.parking.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/plate")
public class PlateController {

    private static final Logger log = LoggerFactory.getLogger(PlateController.class);
    private static final String PLATE_SERVICE_URL = "http://127.0.0.1:5000/recognize";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/recognize")
    public Result recognize(@RequestBody Map<String, String> body) {
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    PLATE_SERVICE_URL, body, Map.class);
            Map<String, Object> result = resp.getBody();
            if (result != null) {
                return Result.ok(result);
            }
            log.error("Python微服务返回空响应");
            return Result.error(500, "车牌识别服务返回空结果");
        } catch (HttpClientErrorException e) {
            log.warn("Python微服务客户端错误: {}", e.getMessage());
            return Result.error(e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Python微服务内部错误: {}", e.getMessage());
            return Result.error(500, "车牌识别服务内部错误");
        } catch (ResourceAccessException e) {
            log.error("无法连接Python微服务: {}", e.getMessage());
            return Result.error(500, "车牌识别服务不可用，请确认微服务已启动: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用Python微服务未知异常: {}", e.getMessage());
            return Result.error(500, "调用车牌识别异常: " + e.getMessage());
        }
    }
}
