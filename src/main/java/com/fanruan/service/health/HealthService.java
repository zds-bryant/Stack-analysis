package com.fanruan.service.health;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @description: 堆栈健康问题接口
 * @author: Henry.Wang
 * @create: 2020/03/31 16:01
 */
public interface HealthService {
    List<JSONObject> analysis(String sessionProcessName);
}
