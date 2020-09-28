package com.fanruan.analysis;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.common.Tools;


import java.io.UnsupportedEncodingException;
import java.util.*;

public abstract class Analysis {
    public Map<String, String> analysisMap = new HashMap<String, String>();

    public JSONObject resultJsonObject = new JSONObject();
    public JSONObject filterResultJsonObject = new JSONObject();

    public String title = "";

    public String getResultJson(String sessionProcessName) {
        synchronized (this) {
            if (analysisMap.containsKey(sessionProcessName)) {
                return analysisMap.get(sessionProcessName);
            }
            resultJsonExecutor(sessionProcessName);
            addTitleData();
            String resultJsonString = resultJsonObject.toJSONString();
            analysisMap.put(sessionProcessName, resultJsonString);
            return resultJsonString;
        }
    }

    public abstract void resultJsonExecutor(String sessionProcessName);

    public String getFilterResultJson(String sessionProcessName,String resultJson, String threadStateFilter, String threadGroupNameFilter,
                                      String stackPatternFilter, String blockAllThreadFilterBase64Encode) throws UnsupportedEncodingException {
        //base64解码
        String blockAllThreadFilterBase64Decode = new String(Base64.getDecoder().decode(blockAllThreadFilterBase64Encode), "UTF-8");
        List<String> blockAllThreadFilter = JSONObject.parseArray(blockAllThreadFilterBase64Decode, String.class);
        if ("".equals(threadStateFilter) && "".equals(threadGroupNameFilter) && "".equals(stackPatternFilter) && blockAllThreadFilter == null) {
            return resultJson;
        }
        filterResultJsonExecutor(sessionProcessName,resultJson, threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        addTitleData();
        String filterResultJsonString = filterResultJsonObject.toJSONString();
        return filterResultJsonString;
    }

    public abstract void filterResultJsonExecutor(String sessionProcessName,String resultJson, String threadStateFilter, String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter);


    public void addTitleData() {
        resultJsonObject.put("titleData", title);
    }

    public void removeSessionIdInMap(String sessionId) {
        Tools tools = new Tools();
        Iterator<String> iterator = analysisMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (tools.getSessionIdFromSessionProcessName(key).equals(sessionId)) {
                iterator.remove();
            }
        }
    }
}
