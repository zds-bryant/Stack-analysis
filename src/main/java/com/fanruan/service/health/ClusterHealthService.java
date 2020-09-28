package com.fanruan.service.health;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.SameSnippetAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 集群健康状态检查
 * @author: Henry.Wang
 * @create: 2020/04/08 15:40
 */
@Service
public class ClusterHealthService implements HealthService {
    @Autowired
    SameSnippetAnalysis sameSnippetAnalysis;

    @Override
    public List<JSONObject> analysis(String sessionProcessName) {
        String sameSnippetResultJsonString = sameSnippetAnalysis.getResultJson(sessionProcessName);
        JSONObject sameSnippetResultJson = (JSONObject) JSONObject.parseObject(sameSnippetResultJsonString);
        List<JSONObject> conclusions = new ArrayList<>();
        JSONObject conclusion0 = monitoringPoint0(sameSnippetResultJson);
        if (conclusion0 != null) {
            conclusions.add(conclusion0);
        }

        return conclusions;
    }

    /**
     * @Description: 检测是否有大量的线程存在如下的代码片段，如果有则说明向集群收集日志等待
     * @Description: com.fr.swift.cluster.service.ClusterAnalyseServiceImpl.getRemoteQueryResult
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/8 15:47
     */
    private JSONObject monitoringPoint0(JSONObject sameSnippetResultJson) {
        JSONObject conclusion0 = new JSONObject();
        JSONArray similarStackJsonArray = sameSnippetResultJson.getJSONArray("similarStack");
        for (int i = 0; i < similarStackJsonArray.size(); i++) {
            String similarStack = (String)similarStackJsonArray.get(i);
            Pattern pattern = Pattern.compile("com\\.fr\\.swift\\.cluster\\.service\\.ClusterAnalyseServiceImpl\\.getRemoteQueryResult");
            Matcher matcher = pattern.matcher(similarStack);
            if(matcher.find()){
                conclusion0.put("tip","多个线程正在向集群中的机器收集日志，等待返回结果，导致这些线程等待");
                return conclusion0;
            }
        }
        return null;
    }
}
