package com.fanruan.service.health;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.SummaryAnalysis;
import com.fanruan.common.Tools;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 查看堆栈是否存在omm问题
 * @author: Henry.Wang
 * @create: 2020/03/31 16:01
 */
@Service
public class OMMHealthService implements HealthService{

    @Autowired
    BatchProcessEntity batchProcessEntity;

    @Autowired
    Tools tools;

    @Override
    public List<JSONObject> analysis(String sessionProcessName) {
        List<JSONObject> conclusions = new ArrayList<>();
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = processEntity.getThreadEntities();
        JSONObject conclusion0 = monitoringPoint0(threadEntities);
        if (conclusion0 != null) {
            conclusions.add(conclusion0);
        }
        return conclusions;
    }

    /**
     * @Description: 检测是否存在阻塞线程多，但是这个进程拥有的锁却很少的情况
     * @param threadEntities
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/2 15:57
     */
    private JSONObject monitoringPoint0(List<ThreadEntity> threadEntities) {
        double blockRatio = getBlockRatio(threadEntities);
        double lockedLockByPreThread = getLockedLockByPreThread(threadEntities);
        if (blockRatio > 0.7 && lockedLockByPreThread < 0.1) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tip","发生内存溢出，JVM进行疯狂GC，导致大量线程阻塞");
            return jsonObject;
        }
        return null;
    }

    private double getBlockRatio(List<ThreadEntity> threadEntities) {
        Map<String, Integer> stateRatio = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            String threadState = threadEntity.getState();
            tools.increaseProgressiveMap(stateRatio, threadState);
        }
        int timeWaitingCount = stateRatio.containsKey("TIMED_WAITING") ? stateRatio.get("TIMED_WAITING") : 0;
        int waitCount = stateRatio.containsKey("WAITING") ? stateRatio.get("WAITING") : 0;
        int blockCount = stateRatio.containsKey("BLOCKED") ? stateRatio.get("BLOCKED") : 0;
        int runCount = stateRatio.containsKey("RUNNABLE") ? stateRatio.get("RUNNABLE") : 0;
        return (double) (timeWaitingCount + waitCount + blockCount) / (double) (timeWaitingCount + waitCount + blockCount + runCount);
    }

    private double getLockedLockByPreThread(List<ThreadEntity> threadEntities) {
        double threadCount = threadEntities.size();
        double lockedLockCount = 0;
        for (ThreadEntity threadEntity : threadEntities) {
            List<String> lockedLock = threadEntity.getLocked();
            List<String> parkLockedLock = threadEntity.getParkLocked();
            lockedLockCount = lockedLockCount + lockedLock.size() + parkLockedLock.size();
        }
        return lockedLockCount / threadCount;
    }
}
