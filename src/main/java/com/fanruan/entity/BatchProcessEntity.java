package com.fanruan.entity;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.service.fileparse.AdaptiveFileFormatService;
import com.fanruan.service.fileparse.ShiftUploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description 一批进程的实体
 */
@Component
public class BatchProcessEntity {
    private Map<String, ProcessEntity> processEntityMap = new HashMap<String, ProcessEntity>();
    //key为进程名，value是该进程对象的json
    //json的key是线程名，value是该线程对应的堆栈信息。
    private Map<String, JSONObject> processEntityJsonObjectMap = new HashMap<>();

    @Autowired
    ShiftUploadFileService shiftUploadFile;

    public ProcessEntity processEntityBuild(String sessionProcessName) {
        synchronized (this) {
            if (processEntityMap.keySet().contains(sessionProcessName)) {
                return processEntityMap.get(sessionProcessName);
            }
            String processData = shiftUploadFile.getFiles().get(sessionProcessName);
            ProcessEntity processEntity = AdaptiveFileFormatService.build(processData);
            processEntityMap.put(sessionProcessName, processEntity);
            return processEntity;
        }
    }

    public JSONObject processEntityJsonObjectBuild(String sessionProcessName) {
        if (processEntityJsonObjectMap.containsKey(sessionProcessName)) {
            return processEntityJsonObjectMap.get(sessionProcessName);
        }
        ProcessEntity processEntity = processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = processEntity.getThreadEntities();
        JSONObject processEntityJsonObject = new JSONObject();
        for (ThreadEntity threadEntity : threadEntities) {
            String threadName = threadEntity.getName();
            String threadStack = threadEntity.getSummaryInformation();
            processEntityJsonObject.put(threadName, threadStack);
        }
        processEntityJsonObjectMap.put(sessionProcessName, processEntityJsonObject);
        return processEntityJsonObject;
    }
}
