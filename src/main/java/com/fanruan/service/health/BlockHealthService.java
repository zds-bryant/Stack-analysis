package com.fanruan.service.health;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.BlockNewAnalysis;
import com.fanruan.analysis.SameSnippetAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 阻塞健康分析
 * @author: Henry.Wang
 * @create: 2020/04/17 09:36
 */
@Service
public class BlockHealthService implements HealthService {
    @Autowired
    BlockNewAnalysis blockNewAnalysis;

    @Autowired
    SameSnippetAnalysis sameSnippetAnalysis;

    @Override
    public List<JSONObject> analysis(String sessionProcessName) {
        String blockNewAnalysisResultJsonString = blockNewAnalysis.getResultJson(sessionProcessName);
        JSONObject blockNewAnalysisResultJson = (JSONObject) JSONObject.parseObject(blockNewAnalysisResultJsonString);

        String sameSnippetAnalysisResultJsonString = sameSnippetAnalysis.getResultJson(sessionProcessName);
        JSONObject sameSnippetAnalysisResultJson = (JSONObject) JSONObject.parseObject(sameSnippetAnalysisResultJsonString);


        List<JSONObject> conclusions = new ArrayList<>();
        List<JSONObject> conclusion0 = monitoringPoint0(sessionProcessName,blockNewAnalysisResultJson);
        if (conclusion0 != null && conclusion0.size() > 0) {
            conclusions.addAll(conclusion0);
        }
        if (conclusion0.size() == 0) {
            List<JSONObject> conclusion1 = monitoringPoint1(sameSnippetAnalysisResultJson);
            if (conclusion1 != null && conclusion1.size() > 0) {
                conclusions.addAll(conclusion1);
            }
        }
        return conclusions;
    }

    public List<JSONObject> monitoringPoint0(String sessionProcessName,JSONObject blockNewAnalysisResultJson) {
        List<JSONObject> jsonObjectList = new ArrayList<>();
        JSONObject allThreadStack = blockNewAnalysis.getAllThreadStack(sessionProcessName);
        JSONArray treeSeriesData = blockNewAnalysisResultJson.getJSONArray("treeSeriesData");
        JSONObject nullThreadStack = blockNewAnalysisResultJson.getJSONObject("nullThreadStack");
        for (int i = 0; i < treeSeriesData.size(); i++) {
            JSONObject jsonObject = treeSeriesData.getJSONObject(i);
            int blockCount = jsonObject.getInteger("childrenNodeCount");
            if (blockCount > 5) {
                JSONObject conclusion0 = new JSONObject();
                String threadName = jsonObject.getString("name");
                conclusion0.put("tip", threadName + "阻塞了" + blockCount + "个线程,堆栈信息如下：");
                if (nullThreadStack.containsKey(threadName)) {
                    conclusion0.put("stackData", nullThreadStack.getString(threadName).replaceAll("((<font color='red'>)|(<br/></font>)|(<br/>))(&nbsp)+", "$1"));
                } else {
                    conclusion0.put("stackData", allThreadStack.getString(threadName).replaceAll("((<font color='red'>)|(<br/></font>)|(<br/>))(&nbsp)+", "$1"));
                }
                jsonObjectList.add(conclusion0);
            }
        }
        return jsonObjectList;
    }

    public List<JSONObject> monitoringPoint1(JSONObject sameSnippetAnalysisResultJson) {
        List<JSONObject> jsonObjectList = new ArrayList<>();
        //原始的相同片段
        JSONArray similarStackJsonArray = sameSnippetAnalysisResultJson.getJSONArray("similarStack");
        //原始相同片段对应的线程信息
        JSONArray similarStackThreadDataJsonArray = sameSnippetAnalysisResultJson.getJSONArray("similarStackThreadData");
        for (int i = 0; i < similarStackJsonArray.size(); i++) {
            String similarStack = similarStackJsonArray.getString(i);
            JSONArray similarStackThreadData = similarStackThreadDataJsonArray.getJSONArray(i);
            Pattern pattern = Pattern.compile("java\\.lang\\.Thread\\.State: ((TIMED_WAITING)|(WAITING)|(BLOCKED))");
            Matcher matcher = pattern.matcher(similarStack);
            if (matcher.find() && similarStack.split("<br/>").length > 10 && similarStackThreadData.size() > 5) {
                JSONObject conclusion1 = new JSONObject();
                conclusion1.put("tip", "以下堆栈阻塞了" + similarStackThreadData.size() + "个线程");
                conclusion1.put("stackData", similarStack.replaceAll("((<font color='red'>)|(<br/></font>)|(<br/>))(&nbsp)+", "$1"));
                jsonObjectList.add(conclusion1);
                break;
            }
        }
        return jsonObjectList;
    }
}
