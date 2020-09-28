package com.fanruan.analysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.common.Tools;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;
import com.fanruan.service.fileparse.ShiftUploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Henry.Wang
 * @description 对进程进行概括
 * @date 2020/3/6
 */
@Service
public class SummaryAnalysis {

    @Autowired
    ShiftUploadFileService shiftUploadFile;

    @Autowired
    BatchProcessEntity batchProcessEntity;

    @Autowired
    Tools tools;

    String title;

    /**
     * @param sessionProcessName
     * @param blockAllThreadFilter
     * @Description: 特有的过滤执行器
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/14 19:37
     */
    public String peculiarFilterResultJsonExecutor(String sessionProcessName, String threadStateFilter, String threadGroupNameFilter,
                                                   String stackPatternFilter, List<String> blockAllThreadFilter) {
        JSONObject filterResultJsonObject = new JSONObject();
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = getFilterThreadEntity(processEntity.getThreadEntities(), threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        JSONObject stateRatio = getStateRatio(threadEntities);
        filterResultJsonObject.put("stateRatio", stateRatio);
        JSONObject groupStateRatio = getGroupStateRatio(threadEntities);
        filterResultJsonObject.put("groupStateRatio", groupStateRatio);
        filterResultJsonObject.put("titleData", title);
        return filterResultJsonObject.toJSONString();
    }


    /**
     * @param threadEntities
     * @param blockAllThreadFilter
     * @Description: 根据过滤条件过滤线程实体
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/14 19:40
     */
    public List<ThreadEntity> getFilterThreadEntity(List<ThreadEntity> threadEntities, String threadStateFilter,
                                                    String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        Map<String, ThreadEntity> threadNameToThreadEntityMap = tools.getThreadNameToThreadEntityMap(threadEntities);
        //获得所有的线程名称
        JSONArray allThreadNameJsonArray = new JSONArray();
        for (ThreadEntity threadEntity : threadEntities) {
            allThreadNameJsonArray.add(threadEntity.getName());
        }
        //根据条件过滤线程名称
        JSONArray filterThreadNameJsonArray = tools.filterThread(threadNameToThreadEntityMap, allThreadNameJsonArray,
                threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        //生成过滤后的线程实体
        List<ThreadEntity> filterThreadEntity = new ArrayList<>();
        for (int i = 0; i < filterThreadNameJsonArray.size(); i++) {
            filterThreadEntity.add(threadNameToThreadEntityMap.get(filterThreadNameJsonArray.getString(i)));
        }
        return filterThreadEntity;
    }

    private JSONObject getGroupStateRatio(List<ThreadEntity> threadEntities) {
        JSONObject jsonObject = new JSONObject();
        Map<String, Map<String, Integer>> threadGroupState = getThreadGroupState(threadEntities);
        if (threadGroupState.keySet().size() == 0) {
            jsonObject.put("jsonData", new JSONArray());
            return jsonObject;
        }
        Map<String, Map<String, Integer>> topThreadGroupState = filterTopThreadGroupState(threadGroupState);
        jsonObject.put("jsonData", buildJsonData(topThreadGroupState));
        return jsonObject;
    }

    private Map<String, Map<String, Integer>> getThreadGroupState(List<ThreadEntity> threadEntities) {
        Map<String, Map<String, Integer>> threadGroupState = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            String threadState = threadEntity.getState();
            Pattern pattern = Pattern.compile("^(.+?)-");
            Matcher matcher = pattern.matcher(threadEntity.getName());
            if (!matcher.find() || threadState == null) {
                continue;
            }
            String groupName = matcher.group(1);
            if (!threadGroupState.containsKey(groupName)) {
                threadGroupState.put(groupName, new HashMap<String, Integer>());
            }
            tools.increaseProgressiveMap(threadGroupState.get(groupName), threadState);
        }
        return threadGroupState;
    }

    /**
     * @param threadGroupState
     * @Description: 只要占比大于3%的分组
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/31 15:15
     */
    private Map<String, Map<String, Integer>> filterTopThreadGroupState(Map<String, Map<String, Integer>> threadGroupState) {
        Map<String, Long> groupCount = getGroupCount(threadGroupState);
        Double sumCount = groupCount.entrySet().stream().mapToDouble(e -> e.getValue()).sum();
        Map<String, Map<String, Integer>> topThreadGroupState = new HashMap<>();
        for (String groupName : threadGroupState.keySet()) {
            Map<String, Integer> groupData = threadGroupState.get(groupName);
            if (groupCount.get(groupName) > sumCount * 0.01) {
                topThreadGroupState.put(groupName, groupData);
            }
        }
        return topThreadGroupState;
    }

    private JSONArray buildJsonData(Map<String, Map<String, Integer>> threadGroupState) {
        Map<String, Long> groupCount = getGroupCount(threadGroupState);
        Long sumCount = groupCount.entrySet().stream().mapToLong(e -> e.getValue()).sum();
        JSONArray jsonArray = new JSONArray();
        for (String groupName : threadGroupState.keySet()) {
            Map<String, Integer> groupData = threadGroupState.get(groupName);
            JSONObject groupJsonData = buildGroupJsonData(groupName, groupData, sumCount, groupCount);
            jsonArray.add(groupJsonData);
        }
        return jsonArray;
    }

    private JSONObject buildGroupJsonData(String groupName, Map<String, Integer> groupData, long sumCount, Map<String, Long> groupCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", groupName);
        jsonObject.put("children", buildGroupChildrenJsonData(groupData, groupName, groupCount.get(groupName)));
        jsonObject.put("attributes", makeNodeAttributes(groupName, groupName, groupCount.get(groupName), sumCount));
        jsonObject.put("itemStyle", getItemStyle(null));
        return jsonObject;
    }

    private JSONArray buildGroupChildrenJsonData(Map<String, Integer> groupData, String groupName, long sumCount) {
        JSONArray jsonArray = new JSONArray();
        for (String state : groupData.keySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", state);
            jsonObject.put("value", groupData.get(state));
            jsonObject.put("itemStyle", getItemStyle(state));
            jsonObject.put("attributes", makeNodeAttributes(state, groupName, groupData.get(state), sumCount));
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private JSONObject makeNodeAttributes(String name, String group, double count, double sumCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("group", group);
        jsonObject.put("count", count);
        NumberFormat numberFormat = NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(2);
        jsonObject.put("proportion", numberFormat.format(count / sumCount));
        return jsonObject;
    }

    /**
     * @param threadGroupState
     * @Description: 获得每个分组的线程数
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/31 15:14
     */
    private Map<String, Long> getGroupCount(Map<String, Map<String, Integer>> threadGroupState) {
        Map<String, Long> groupCount = new HashMap<>();
        for (String groupName : threadGroupState.keySet()) {
            Map<String, Integer> groupData = threadGroupState.get(groupName);
            long count = groupData.entrySet().stream().mapToInt(e -> e.getValue()).sum();
            groupCount.put(groupName, count);
        }
        return groupCount;
    }

    private JSONObject getItemStyle(String state) {
        JSONObject jsonObject = new JSONObject();
        if (state != null) {
            Map<String, String> stateColorMap = getStateColorMap();
            jsonObject.put("color", stateColorMap.get(state));
        } else {
            jsonObject.put("color", "rgba(0,0,0,0.3)");
        }
        jsonObject.put("borderWidth", 2);
        return jsonObject;
    }


    private JSONObject getStateRatio(List<ThreadEntity> threadEntities) {
        Map<String, Integer> stateMap = new HashMap<String, Integer>();

        int threadCount = 0;
        for (ThreadEntity threadEntity : threadEntities) {
            threadCount = threadCount + 1;
            String state = threadEntity.getState();
            if (stateMap.containsKey(state)) {
                stateMap.put(state, stateMap.get(state) + 1);
            } else {
                stateMap.put(state, 1);
            }
        }
        JSONObject jsonObject = buildStateRatioOptionData(stateMap);
        this.title = tools.getLocText("SUMMARY_TITLE_PART_ONE") + threadCount + tools.getLocText("SUMMARY_TITLE_PART_TWO");
        return jsonObject;
    }

    private JSONObject buildStateRatioOptionData(Map<String, Integer> stateMap) {
        JSONObject jsonObject = new JSONObject();
        JSONArray legendData = new JSONArray();
        JSONArray seriesData = new JSONArray();
        JSONArray colorData = new JSONArray();
        Map<String, String> stateColorMap = getStateColorMap();
        for (String state : stateMap.keySet()) {
            legendData.add(state);
            if (!stateColorMap.containsKey(state)) {
                System.out.println(state);
            }
            colorData.add(stateColorMap.containsKey(state) ? stateColorMap.get(state) : "slateblue");
            JSONObject tempJsonObject = new JSONObject();
            tempJsonObject.put("name", state);
            tempJsonObject.put("value", stateMap.get(state));
            seriesData.add(tempJsonObject);
        }
        jsonObject.put("legendData", legendData);
        jsonObject.put("colorData", colorData);
        jsonObject.put("seriesData", seriesData);
        return jsonObject;
    }

    private Map<String, String> getStateColorMap() {
        Map<String, String> stateColorMap = new HashMap<>();
        stateColorMap.put("TIMED_WAITING", "rosybrown");
        stateColorMap.put("WAITING", "sandybrown");
        stateColorMap.put("BLOCKED", "red");
        stateColorMap.put("RUNNABLE", "green");
        return stateColorMap;
    }
}
