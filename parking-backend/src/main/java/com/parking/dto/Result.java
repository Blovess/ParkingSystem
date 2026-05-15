package com.parking.dto;

import java.util.HashMap;
import java.util.Map;

public class Result extends HashMap<String, Object> {

    public static Result ok() {
        Result r = new Result();
        r.put("code", 200);
        r.put("message", "success");
        r.put("data", null);
        return r;
    }

    public static Result ok(Object data) {
        Result r = ok();
        r.put("data", data);
        return r;
    }

    public static Result error(int code, String message) {
        Result r = new Result();
        r.put("code", code);
        r.put("message", message);
        r.put("data", null);
        return r;
    }

    public static Result error(String message) {
        return error(500, message);
    }
}
