package com.fanruan.service.health;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.entity.ProcessEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 查看堆栈是否存在死锁问题
 * @author: Henry.Wang
 * @create: 2020/04/01 16:07
 */
@Service
public class DeadLockHealthService implements HealthService {
    @Autowired
    BatchProcessEntity batchProcessEntity;

    @Override
    public List<JSONObject> analysis(String sessionProcessName) {
        List<JSONObject> conclusions = new ArrayList<>();
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        int deadLockCount = processEntity.getDeadLockCount();
        if (deadLockCount > 0) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tip","发现"+deadLockCount+"个死锁信息！");
            conclusions.add(jsonObject);
        }
        return conclusions;
    }
}
