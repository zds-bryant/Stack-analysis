package com.fanruan.analysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 线程明细列表
 * @author: Henry.Wang
 * @create: 2020/04/08 20:08
 */
@Service
public class ThreadListAnalysis extends Analysis {
    @Autowired
    BatchProcessEntity batchProcessEntity;

    @Autowired
    Tools tools;

    Map<String, Map<String, ThreadEntity>> sessionThreadNameToThreadEntityMap = new HashMap<>();

    @Override
    public void filterResultJsonExecutor(String sessionProcessName, String resultJson, String threadStateFilter, String threadGroupNameFilter,
                                         String stackPatternFilter, List<String> blockAllThreadFilter) {
        JSONObject resultJsonObject = (JSONObject) JSONObject.parse(resultJson);
        JSONArray threadNameJsonArray = resultJsonObject.getJSONArray("threadName");
        JSONArray filterThreadNameJsonArray = tools.filterThread(sessionThreadNameToThreadEntityMap.get(sessionProcessName), threadNameJsonArray,
                threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        this.filterResultJsonObject.put("threadName", filterThreadNameJsonArray);
        //JSONArray keyWordIndex = getKeyWordIndex(filterThreadNameJsonArray, stackPatternFilter);
        //this.filterResultJsonObject.put("keyWordIndex", keyWordIndex);
    }

    /**
     * @param filterThreadNameJsonArray
     * @param stackPatternFilter
     * @Description: 查找关键字所在位置的信息，方便前端定位到
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/13 20:43
     */
    private JSONArray getKeyWordIndex(String sessionProcessName, JSONArray filterThreadNameJsonArray, String stackPatternFilter) {
        JSONArray keyWordLIndex = new JSONArray();
        if ("".equals(stackPatternFilter) || stackPatternFilter == null) {
            return keyWordLIndex;
        }
        for (int i = 0; i < filterThreadNameJsonArray.size(); i++) {
            ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(filterThreadNameJsonArray.getString(i));
            String stackData = threadEntity.getSummaryInformation();
            Pattern pattern = Pattern.compile(stackPatternFilter);
            Matcher matcher = pattern.matcher(stackData);
            while (matcher.find()) {
                JSONObject keyWordPos = new JSONObject();
                keyWordPos.put("start", matcher.start());
                keyWordPos.put("end", matcher.end());
                keyWordPos.put("threadNo", i);
                keyWordPos.put("threadName", threadEntity.getName());
                keyWordLIndex.add(keyWordPos);
            }
        }
        for (int i = 0; i < keyWordLIndex.size(); i++) {
            int start = keyWordLIndex.getJSONObject(i).getInteger("start");
            String threadName = keyWordLIndex.getJSONObject(i).getString("threadName");
            ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(threadName);
            String summaryInformation = threadEntity.getSummaryInformation();
            String[] lines = summaryInformation.split("\n");
            int sumSize = 0;
            for (int j = 0; j < lines.length; j++) {
                if (sumSize > start) {
                    keyWordLIndex.getJSONObject(i).put("location", (double) j / lines.length);
                    break;
                }
                sumSize = sumSize + lines[j].length() + 1;
            }
        }

        return keyWordLIndex;
    }

    @Override
    public void resultJsonExecutor(String sessionProcessName) {
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = processEntity.getThreadEntities();
        initToolMap(sessionProcessName, threadEntities);
        JSONArray threadNameJsonArray = new JSONArray();
        for (int i = 0; i < threadEntities.size(); i++) {
            threadNameJsonArray.add(threadEntities.get(i).getName());
        }
        this.resultJsonObject.put("threadName", threadNameJsonArray);
    }

    private void initToolMap(String sessionProcessName, List<ThreadEntity> threadEntities) {
        sessionThreadNameToThreadEntityMap.put(sessionProcessName, tools.getThreadNameToThreadEntityMap(threadEntities));
    }
}
