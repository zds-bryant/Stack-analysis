package com.fanruan.service.health;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.BlockNewAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 查看堆栈是否存在从缓存中获取数据缓慢
 * @author: Henry.Wang
 * @create: 2020/04/02 15:52
 */
@Service
public class GetDataHealthService implements HealthService {
    @Autowired
    BlockNewAnalysis blockNewAnalysis;

    @Override
    public List<JSONObject> analysis(String sessionProcessName) {
        List<JSONObject> conclusions = new ArrayList<>();
        String resultJsonString = blockNewAnalysis.getResultJson(sessionProcessName);
        JSONObject resultJsonObject = JSONObject.parseObject(resultJsonString);

        JSONObject conclusion0 = monitoringPoint0(sessionProcessName,resultJsonObject);
        if (conclusion0 != null) {
            conclusions.add(conclusion0);
        }
        JSONObject conclusion1 = monitoringPoint1(sessionProcessName,resultJsonObject);
        if (conclusion1 != null) {
            conclusions.add(conclusion1);
        }
        return conclusions;
    }

    /**
     * @param resultJsonObject
     * @Description: 检测是否有大量线程在等待ehcache缓存中的数据
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/2 16:19
     */
    private JSONObject monitoringPoint0(String sessionProcessName,JSONObject resultJsonObject) {
        JSONObject allThreadStack = blockNewAnalysis.getAllThreadStack(sessionProcessName);
        JSONObject histogramDistribution = (JSONObject) resultJsonObject.get("histogramDistribution");
        JSONArray histogramXAxisData = (JSONArray) histogramDistribution.get("histogramXAxisData");
        JSONArray histogramSeriesData = (JSONArray) histogramDistribution.get("histogramSeriesData");
        for (int i = 0; i < histogramXAxisData.size(); i++) {
            if ((int) histogramSeriesData.get(i) > 30) {
                String stackData = (String) allThreadStack.get((String) histogramXAxisData.get(i));
                Pattern pattern = Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantReadWriteLock\\$ReadLock\\.lock" +
                        "((.|\n)+?)" +
                        "com\\.fr\\.third\\.net\\.sf\\.ehcache\\.store\\.cachingtier\\.OnHeapCachingTier\\$Fault\\.get");
                Matcher matcher = pattern.matcher(stackData);
                if (matcher.find()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("tip","大量线程试图从ehcache缓存中读取数据，由于ehcache中没有准备好数据导致大量线程阻塞");
                    return jsonObject;
                }
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * @param blockResultJsonObject
     * @Description: 查询有没有出现下面的代码造成的阻塞
     * @Description: com.fr.web.core.TemplateSessionIDInfo.getParameterMap4Execute
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/7 17:19
     */
    private JSONObject monitoringPoint1(String sessionProcessName,JSONObject blockResultJsonObject) {
        JSONArray treeSeriesData = (JSONArray) blockResultJsonObject.get("treeSeriesData");
        for (int i = 0; i < treeSeriesData.size(); i++) {
            JSONObject jsonObject = (JSONObject) treeSeriesData.get(i);
            if (i == 0 || jsonObject.getInteger("childrenNodeCount") > 20) {
                String threadName = (String) jsonObject.get("name");
                JSONArray children = (JSONArray) jsonObject.get("children");
                int mask = monitoringPoint1Handler(sessionProcessName,threadName, children);
                if (mask == 1) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("tip","查询数据库时间过长，导致报表线程一直在等待查询结果");
                    return jsonObject1;
                }
                if (mask == 2) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("tip","大量线程在获取报表参数时阻塞");
                    return jsonObject1;
                }

            }
        }
        return null;
    }

    private int monitoringPoint1Handler(String sessionProcessName,String threadName, JSONArray children) {
        int mask = monitoringPoint1Checker(sessionProcessName,threadName, children);
        if (mask > 0) {
            return mask;
        } else {
            for (int i = 0; i < children.size(); i++) {
                JSONObject jsonObject = (JSONObject) children.get(i);
                int mask2 = monitoringPoint1Handler(sessionProcessName,(String) jsonObject.get("name"), (JSONArray) jsonObject.get("children"));
                if (mask2 > 0) {
                    return mask2;
                }
            }
        }
        return 0;
    }

    private int monitoringPoint1Checker(String sessionProcessName,String threadName, JSONArray children) {
        JSONObject allThreadStack = blockNewAnalysis.getAllThreadStack(sessionProcessName);
        String stackData = (String) allThreadStack.get(threadName);
        Pattern pattern;
        Matcher matcher;

        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            JSONObject childrenJsonObject = children.getJSONObject(i);
            String childrenThreadName = childrenJsonObject.getString("name");
            String childrenStackData = (String) allThreadStack.get(childrenThreadName);
            pattern = Pattern.compile("com\\.fr\\.web\\.core\\.TemplateSessionIDInfo\\.getParameterMap4Execute");
            matcher = pattern.matcher(childrenStackData);
            if (matcher.find()) {
                count++;
            }
        }
        if (count > children.size() * 0.7) {
            pattern = Pattern.compile("com\\.fr\\.data\\.impl\\.restriction\\.TimeoutExecutor\\.execute");
            matcher = pattern.matcher(stackData);
            if (matcher.find()) {
                return 1;
            }
            pattern = Pattern.compile("org\\.apache\\.coyote\\.http11\\.InternalNioOutputBuffer\\.addToBB");
            matcher = pattern.matcher(stackData);
            if(matcher.find()){
                return 2;
            }
        }
        return 0;
    }
}