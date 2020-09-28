package com.fanruan.analysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;
import com.fanruan.service.health.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 健康分析
 * @author: Henry.Wang
 * @create: 2020/03/17 09:22
 */
@Service
public class HealthAnalysis extends Analysis {
    @Autowired
    OMMHealthService ommHealthService;

    @Autowired
    DeadLockHealthService deadLockHealthService;

    @Autowired
    GetDataHealthService getDataHealthService;

    @Autowired
    ClusterHealthService clusterHealthService;

    @Autowired
    BlockHealthService blockHealthService;

    @Autowired
    BatchProcessEntity batchProcessEntity;
    
    public void resultJsonExecutor(String sessionProcessName, JSONArray resultArr) {
        JSONArray jsonArray = new JSONArray();
        List<JSONObject> ommHealthList = ommHealthService.analysis(sessionProcessName);
        List<JSONObject> deadLockHealthList = deadLockHealthService.analysis(sessionProcessName);
        List<JSONObject> getDataHealthList = getDataHealthService.analysis(sessionProcessName);
        List<JSONObject> clusterHealthList = clusterHealthService.analysis(sessionProcessName);
        List<JSONObject> blockHealthList = blockHealthService.analysis(sessionProcessName);
        addHealthReport(jsonArray, ommHealthList);
        addHealthReport(jsonArray, deadLockHealthList);
        addHealthReport(jsonArray, getDataHealthList);
        addHealthReport(jsonArray, clusterHealthList);
        addHealthReport(jsonArray, blockHealthList);
        resultArr.addAll(jsonArray);
        this.title = "此堆栈存在以下的风险";
    }

    @Override
    public void filterResultJsonExecutor(String sessionProcessName, String resultJson, String threadStateFilter, String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {

    }

    private void addHealthReport(JSONArray jsonArray, List<JSONObject> healthList) {
        for (JSONObject health : healthList) {
            jsonArray.add(health);
        }
    }

    public String getResultJson(String sessionProcessName) {
        synchronized (this) {
            JSONArray jsonArray = new JSONArray();
            if (analysisMap.containsKey(sessionProcessName)) {
                return analysisMap.get(sessionProcessName);
            }
            resultJsonExecutor(sessionProcessName, jsonArray);
            Set<ThreadEntity> trueResultFromList = getTrueResultFromList(sessionProcessName, jsonArray);
            JSONObject jsonObject = buildStackString(trueResultFromList);
            if (jsonObject != null){
                jsonArray.add(jsonObject);
            }
            addTitleData();
            this.resultJsonObject.put("healthReport", jsonArray);
            String resultJsonString = this.resultJsonObject.toJSONString();
            analysisMap.put(sessionProcessName, resultJsonString);
            return resultJsonString;
        }
    }

    @Override
    public void resultJsonExecutor(String sessionProcessName) {

    }

    public JSONObject buildStackString(Set<ThreadEntity> trueResultFromList){
        String summaryInformation = "";
        JSONObject jsonObject = new JSONObject();
        for (ThreadEntity threadEntity : trueResultFromList) {
            summaryInformation += threadEntity.getSummaryInformation() + "\n";
        }
        if (StringUtils.isAnyEmpty(summaryInformation)){
            return null;
        }
        jsonObject.put("stackData", summaryInformation);
        jsonObject.put("tip", "可能是以下问题导致");
        return jsonObject;
    }

    public Set<ThreadEntity> getTrueResultFromList(String sessionProcessName, JSONArray jsonArray) {
        Set<String> problemList = new HashSet<>();
        collectProblemStack(problemList, jsonArray);
        Set<String> problemStackName = findProblemStackName(problemList);
        return matchProblemThread(sessionProcessName, problemStackName);
    }

    public void collectProblemStack(Set<String> problemList, JSONArray jsonArray){
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject stack = JSONObject.parseObject(jsonArray.get(i).toString());
            String stackData = stack.get("stackData").toString();
            if (!"".equals(getTrueResult(stackData)))
                problemList.add(getTrueResult(stackData));
        }
    }

    public String getTrueResult(String stackData){
        String[] split = stackData.split("<br/>");
        for (String s : split) {
            if (matchFrStack(s)) {
                return s;
            }
        }
        return "";
    }

    public boolean matchFrStack(String stackData){
        Pattern pattern = Pattern.compile("at com.fr.");
        Matcher matcher = pattern.matcher(stackData);
        return matcher.find();
    }

    public Set<String> findProblemStackName(Set<String> problemList){
        Iterator<String> iterator = problemList.iterator();
        Set<String> problemStack = new HashSet<>();
        while (iterator.hasNext()){
            String next = iterator.next();
            String[] substring;
            substring = next.contains("(")
                    ? next.substring(0, next.indexOf("(")).split("\\.")
                    : next.split("\\.");
            problemStack.add(substring[substring.length-2]);
        }
        return problemStack;
    }

    public Set<ThreadEntity> matchProblemThread(String sessionProcessName, Set<String> problemStackName){
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = processEntity.getThreadEntities();
        Set<ThreadEntity> threadEntitySet = new HashSet<>();
        Iterator<ThreadEntity> iterator = threadEntities.iterator();
        while (iterator.hasNext()){
            if (problemStackName.isEmpty()){
                break;
            }
            ThreadEntity next = iterator.next();
            if (!"RUNNABLE".equals(next.getState())){
                continue;
            }
            String name = next.getName();
            String matchedThread = "";
            for (String problem : problemStackName) {
                if (name.contains(problem)){
                    matchedThread = problem;
                    threadEntitySet.add(next);
                    break;
                }
            }
            problemStackName.remove(matchedThread);
        }
        return threadEntitySet;
    }
}
