package com.fanruan.analysis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.common.Tools;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BlockNewAnalysis extends Analysis {

    @Autowired
    BatchProcessEntity batchProcessEntity;
    @Autowired
    Tools tools;
    //锁被哪个线程拥有。这个要第一个被调用
    Map<String, Map<String, String>> sessionLockContainedByThread = new HashMap<>();
    //一把锁被哪些线程竞争
    Map<String, Map<String, List<String>>> sessionThreadsCompeteLock = new HashMap<>();
    //线程之间的锁依赖
    Map<String, Map<String, String>> sessionThreadLockDependence = new HashMap<>();
    //线程名称到线程实体的映射
    Map<String, Map<String, ThreadEntity>> sessionThreadNameToThreadEntityMap = new HashMap<>();
    //所有的堆栈信息(这个主要是在健康分析中用到的，不用传到前端)
    Map<String, JSONObject> sessionAllThreadStack = new HashMap<>();
    //死锁环
    Map<String, List<List<String>>> sessionAllDeadLockLoop = new HashMap<>();

    @Override
    public void filterResultJsonExecutor(String sessionProcessName, String resultJson, String threadStateFilter,
                                         String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        JSONObject resultJsonObject = (JSONObject) JSONObject.parse(resultJson);
        JSONArray treeSeriesData = resultJsonObject.getJSONArray("treeSeriesData");
        //过滤后的数据保存对象
        JSONArray filterTreeSeriesData = new JSONArray();
        filterTreeNode(sessionProcessName, treeSeriesData, filterTreeSeriesData, threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        //把树的一些信息保存在根节点上
        renderTreeRootNode(sessionProcessName, filterTreeSeriesData);
        //如果stackPatternFilter匹配树节点对应的堆栈，则把这个节点的名字显示为橙色
        renderTreeNodeName(sessionProcessName, filterTreeSeriesData, stackPatternFilter);
        //空线程的堆栈信息
        JSONObject nullThreadStack = getNullThreadStack(sessionProcessName, filterTreeSeriesData);
        //
        JSONObject histogramDistribution = getHistogramDistribution(filterTreeSeriesData);
        this.filterResultJsonObject.put("treeSeriesData", filterTreeSeriesData);
        this.filterResultJsonObject.put("nullThreadStack", nullThreadStack);
        this.filterResultJsonObject.put("histogramDistribution", histogramDistribution);
    }

    @Override
    public void resultJsonExecutor(String sessionProcessName) {
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        sessionAllDeadLockLoop.put(sessionProcessName, processEntity.getAllDeadLockLoop());
        //会产生新的空实体，原来的实体不会改变
        List<ThreadEntity> threadEntities = shiftThreadEntities(processEntity.getThreadEntities());
        initToolMap(sessionProcessName, threadEntities);
        //获取循环依赖并打破。这个一步threadEntities实体可能会被改变。
        JSONArray treeSeriesData = getTreeSeriesData(sessionProcessName);
        JSONObject nullThreadStack = getNullThreadStack(sessionProcessName, treeSeriesData);
        JSONObject histogramDistribution = getHistogramDistribution(treeSeriesData);

        this.resultJsonObject.put("treeSeriesData", treeSeriesData);
        this.resultJsonObject.put("nullThreadStack", nullThreadStack);
        this.resultJsonObject.put("histogramDistribution", histogramDistribution);
        sessionAllThreadStack.put(sessionProcessName, getAllThreadStack(threadEntities));
    }


    /**
     * @param filterTreeSeriesData
     * @param stackPatternFilter
     * @Description: //如果stackPatternFilter匹配树节点对应的堆栈，则把这个节点的名字显示为橙色
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/10 16:52
     */
    private void renderTreeNodeName(String sessionProcessName, JSONArray filterTreeSeriesData, String stackPatternFilter) {
        if ("".equals(stackPatternFilter)) {
            return;
        }
        for (int i = 0; i < filterTreeSeriesData.size(); i++) {
            JSONObject nodeJsonObject = filterTreeSeriesData.getJSONObject(i);
            String theadName = nodeJsonObject.getString("name");
            ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(theadName);
            String stackData = threadEntity.getSummaryInformation();
            Pattern pattern = Pattern.compile(stackPatternFilter);
            Matcher matcher = pattern.matcher(stackData);
            if (matcher.find()) {
                JSONObject labelJsonObject = new JSONObject();
                labelJsonObject.put("color", "coral");
                nodeJsonObject.put("label", labelJsonObject);
            }
            JSONArray children = nodeJsonObject.getJSONArray("children");
            renderTreeNodeName(sessionProcessName, children, stackPatternFilter);
        }
    }

    public boolean filterTreeNode(String sessionProcessName, JSONArray treeSeriesData, JSONArray filterTreeSeriesData, String threadStateFilter,
                                  String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        for (int i = 0; i < treeSeriesData.size(); i++) {
            JSONObject currentNode = treeSeriesData.getJSONObject(i);
            String threadName = currentNode.getString("name");
            JSONArray childrenJsonArray = currentNode.getJSONArray("children");
            JSONObject filterNode = new JSONObject();
            copyTreeNodeValue(filterNode, currentNode);
            if (filterTreeNodeRunner(sessionProcessName, threadName, threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter)) {
                filterNode.put("children", childrenJsonArray);
                filterTreeSeriesData.add(filterNode);
            } else {
                JSONArray filterChildrenJsonArray = new JSONArray();
                if (filterTreeNode(sessionProcessName, childrenJsonArray, filterChildrenJsonArray, threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter)) {
                    filterNode.put("children", filterChildrenJsonArray);
                    filterTreeSeriesData.add(filterNode);
                }
            }
        }
        if (filterTreeSeriesData.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * @param filterNode
     * @param currentNode
     * @Description: 复制普通节点的属性
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/10 15:14
     */
    private void copyTreeNodeValue(JSONObject filterNode, JSONObject currentNode) {
        filterNode.put("name", currentNode.getString("name"));
        if (currentNode.containsKey("value")) {
            filterNode.put("value", currentNode.getJSONObject("value"));
        }
        if (currentNode.containsKey("itemStyle")) {
            filterNode.put("itemStyle", currentNode.getJSONObject("itemStyle"));
        }
    }

    private boolean filterTreeNodeRunner(String sessionProcessName, String threadName, String threadStateFilter, String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        JSONArray allThreadNameJsonArray = new JSONArray();
        allThreadNameJsonArray.add(threadName);
        JSONArray filterThreadNameJsonArray = tools.filterThread(sessionThreadNameToThreadEntityMap.get(sessionProcessName), allThreadNameJsonArray,
                threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        if (filterThreadNameJsonArray.size() > 0)
            return true;
        return false;
    }

    private void initToolMap(String sessionProcessName, List<ThreadEntity> threadEntities) {
        sessionLockContainedByThread.put(sessionProcessName, tools.findLockContainedByThread(threadEntities));
        sessionThreadsCompeteLock.put(sessionProcessName, tools.getThreadsCompeteLock(threadEntities));
        sessionThreadLockDependence.put(sessionProcessName, tools.findThreadLockDependence(threadEntities, sessionLockContainedByThread.get(sessionProcessName)));
        sessionThreadNameToThreadEntityMap.put(sessionProcessName, tools.getThreadNameToThreadEntityMap(threadEntities));
    }

    private List<ThreadEntity> shiftThreadEntities(List<ThreadEntity> threadEntities) {
        //线程copy
        List<ThreadEntity> shiftThreadEntities = new ArrayList<>();
        for (ThreadEntity threadEntity : threadEntities) {
            ThreadEntity shiftThreadEntity = new ThreadEntity();
            shiftThreadEntity.setName(threadEntity.getName());
            shiftThreadEntity.setState(threadEntity.getState());
            shiftThreadEntity.setSummaryInformation(threadEntity.getSummaryInformation());
            shiftThreadEntity.setWaitingLock(threadEntity.getWaitingLock() == null ? threadEntity.getParkWaitLock() : threadEntity.getWaitingLock());
            shiftThreadEntity.getLocked().addAll(threadEntity.getParkLocked());
            shiftThreadEntity.getLocked().addAll(threadEntity.getLocked());
            shiftThreadEntities.add(shiftThreadEntity);
        }
        //为找不到线程的锁，添加空线程
        identifyLocksNotOwn(shiftThreadEntities);
        return shiftThreadEntities;
    }

    public JSONArray getTreeSeriesData(String sessionProcessName) {
        JSONArray jsonArray = new JSONArray();
        for (String lockName : sessionThreadsCompeteLock.get(sessionProcessName).keySet()) {
            //拥有这个锁的线程名称
            String holdThisLockThreadName = sessionLockContainedByThread.get(sessionProcessName).get(lockName);
            //如果该线程在等待其他锁则说明不是根线程
            //如果存在死循环的话，岂不是这个死循环就不显示了？其实这里是不存在死循环的，因为在getAllDeadAndBreakLockLoop函数中已经把死循环破环了
            if (sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(holdThisLockThreadName).getWaitingLock() != null) {
                continue;
            }
            //这个用来画一颗树的数据json
            JSONObject jsonObject = buildTreeSeriesData(sessionProcessName, holdThisLockThreadName);
            jsonArray.add(jsonObject);
        }
        //在根节点上添加关于这个树的信息
        renderTreeRootNode(sessionProcessName, jsonArray);
        return renderChildrenNodeCountSort(jsonArray);
    }


    /**
     * @param jsonArray
     * @Description: 把关于这可树的一些信息放在根节点中
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/1 9:54
     */
    private void renderTreeRootNode(String sessionProcessName, JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(jsonObject.getString("name"));
            //设置根节点的大小
            jsonObject.put("symbolSize", 32);
            //这颗树是否存在死循环，默认不存在
            jsonObject.put("deadLock", 0);
            //这个颗树的包含的所以线程名称
            JSONArray allTheadName = new JSONArray();
            getAllThreadNameFromTree(jsonObject, allTheadName);
            jsonObject.put("childrenNodeCount", allTheadName.size() - 1);
            jsonObject.put("allNodeName", allTheadName);
            //这个树根节点的属性
            jsonObject.put("value", makeNodeAttributes(sessionProcessName, jsonObject.getString("name"), sessionAllDeadLockLoop.get(sessionProcessName)));
            //如果是空线程，设置为方形，颜色为棕色
            if (threadEntity.isNullEntity()) {
                jsonObject.put("symbol", "square");
                JSONObject itemStyle = new JSONObject();
                itemStyle.put("color", "rgba(100,20,56,0.6)");
                itemStyle.put("borderColor", "rgba(100,20,56,0.6)");
                jsonObject.put("itemStyle", itemStyle);
            }
            //渲染死锁路径
            renderTreeDeadLock(jsonObject, sessionAllDeadLockLoop.get(sessionProcessName));
        }
    }

    /**
     * @param rootThreadName
     * @Description: 利用递归生成数据来画树状图
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/23 9:38
     */
    private JSONObject buildTreeSeriesData(String sessionProcessName, String rootThreadName) {
        ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(rootThreadName);
        //得到被阻塞的线程，最终把所有的线程名放在sumWantThisLockThread中
        List<String> rootHoldLocks = threadEntity.getLocked();
        List<String> sumWantThisLockThread = new ArrayList<>();
        for (String rootHoldLock : rootHoldLocks) {
            List<String> temp = sessionThreadsCompeteLock.get(sessionProcessName).get(rootHoldLock);
            if (temp == null) {
                continue;
            }
            sumWantThisLockThread.addAll(temp);
        }
        //节点信息jsonObject
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", rootThreadName);
        jsonObject.put("itemStyle", getStateItemStyle(threadEntity.getState()));
        JSONArray childrenJsonArray = new JSONArray();
        for (String wantThisLockThread : sumWantThisLockThread) {
            JSONObject childrenJsonObject = buildTreeSeriesData(sessionProcessName, wantThisLockThread);
            childrenJsonArray.add(childrenJsonObject);
        }
        jsonObject.put("value", makeNodeAttributes(sessionProcessName, rootThreadName, null));
        jsonObject.put("children", childrenJsonArray);
        return jsonObject;
    }

    private void getAllThreadNameFromTree(JSONObject tree, JSONArray allTheadName) {
        String threadName = tree.getString("name");
        allTheadName.add(threadName);
        JSONArray childrenJsonArray = tree.getJSONArray("children");
        for (int i = 0; i < childrenJsonArray.size(); i++) {
            JSONObject childrenJsonObject = childrenJsonArray.getJSONObject(i);
            getAllThreadNameFromTree(childrenJsonObject, allTheadName);
        }
    }

    /**
     * @param locked
     * @param childrenJsonArray
     * @Description: 一个锁如果没有被一个线程拥有我们会建一个空的线程实体来拥有这个锁，这个函数的作用是给这个线程实体添加堆栈
     * @Description: 堆栈的内容是多个等待该锁线程相同的堆栈
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/2 15:12
     */
    private String getNullThreadStackInfo(String sessionProcessName, JSONArray childrenJsonArray, String locked) {
        //多个子线程关于这个锁的相关堆栈
        List<List<String>> lockRelevantStackList = new ArrayList<>();
        for (int i = 0; i < childrenJsonArray.size(); i++) {
            JSONObject childrenJsonObject = (JSONObject) childrenJsonArray.get(i);
            String childrenThreadName = (String) childrenJsonObject.get("name");
            ThreadEntity childrenThreadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(childrenThreadName);
            List<String> lockRelevantStack = getLockHeadRelevantStack(childrenThreadEntity, locked);
            lockRelevantStackList.add(lockRelevantStack);
        }
        //在多个子线程堆栈中获得相同的堆栈
        List<String> similarLockHeadRelevantStack = getSimilarLockHeadRelevantStack(lockRelevantStackList);
        //为空线程实体设置堆栈信息
        String nullThreadSummaryInformation = "";
        for (String s : similarLockHeadRelevantStack) {
            nullThreadSummaryInformation = nullThreadSummaryInformation + s + "\n";
        }
        return nullThreadSummaryInformation;
    }

    /**
     * @param lockRelevantStackList
     * @Description: 在多个子线程堆栈中获得相同的堆栈
     * @Description:
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/2 15:26
     */
    private List<String> getSimilarLockHeadRelevantStack(List<List<String>> lockRelevantStackList) {
        if (lockRelevantStackList.size() == 1) {
            return lockRelevantStackList.get(0);
        }
        List<String> similarLockHeadRelevantStack = new ArrayList<>();
        int sum = lockRelevantStackList.size();
        int linePos = 0;
        while (true) {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < lockRelevantStackList.size() && linePos < lockRelevantStackList.get(i).size(); i++) {
                tools.increaseProgressiveMap(map, lockRelevantStackList.get(i).get(linePos));
            }
            List<Map.Entry<String, Integer>> sortList = new ArrayList<>(map.entrySet());
            Collections.sort(sortList, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    if (o1.getValue().equals(o2.getValue())) {
                        return 0;
                    }
                    return o1.getValue() > (o2.getValue()) ? -1 : 1;
                }
            });
            if (sortList.size() > 0 && sortList.get(0).getValue() > sum * 0.8) {
                similarLockHeadRelevantStack.add(sortList.get(0).getKey());
            } else {
                break;
            }
            linePos++;
        }
        return similarLockHeadRelevantStack;
    }

    /**
     * @param childrenThreadEntity
     * @param locked
     * @Description:首先在堆栈中找到等待locked所在的行，然后再向下遍历每一行，找到相关的堆栈，最终返回这个相关的堆栈
     * @Description:我们把堆栈分块，如果堆栈具有相同的包名就属于同一块。相关堆栈就是locked所在行下面的两块堆栈。
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/2 15:17
     */
    private List<String> getLockHeadRelevantStack(ThreadEntity childrenThreadEntity, String locked) {
        List<String> lockRelevantStack = new ArrayList<>();
        String summaryInformation = childrenThreadEntity.getSummaryInformation();
        String[] lines = summaryInformation.split("\n");
        for (int i = 0; i < lines.length; i++) {
            Pattern pattern = Pattern.compile("<" + locked + ">");
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {

                lockRelevantStack.add(lines[i]);
                for (int j = i + 1; j < lines.length; j++) {
                    lockRelevantStack.add(lines[j]);
                }
                break;
            }
        }
        return lockRelevantStack;
    }

    private JSONObject getStateItemStyle(String state) {
        JSONObject jsonObject = new JSONObject();
        if ("RUNNABLE".equals(state)) {
            jsonObject.put("color", "rgba(23,255,108,1)");
            jsonObject.put("borderColor", "rgba(23,255,108,1)");
        } else if ("BLOCKED".equals(state)) {
            jsonObject.put("color", "rgba(255,24,10,1)");
            jsonObject.put("borderColor", "rgba(255,24,10,1)");
        }
        return jsonObject;
    }

    /**
     * @param threadName
     * @Description: 数据节点的属性
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/23 9:39
     */
    private JSONObject makeNodeAttributes(String sessionProcessName, String threadName, List<List<String>> allDependenceList) {
        ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(threadName);
        JSONObject jsonObject = new JSONObject();
        if (allDependenceList == null) {
            jsonObject.put("name", threadEntity.isNullEntity() ? "-" : threadName);
            jsonObject.put("waitLock", threadEntity.getWaitingLock() == null ? "-" : threadEntity.getWaitingLock());
            jsonObject.put("dependenceName", sessionThreadLockDependence.get(sessionProcessName).containsKey(threadEntity.getName()) ?
                    sessionThreadLockDependence.get(sessionProcessName).get(threadEntity.getName()) : "-");
            jsonObject.put("lock", threadEntity.getLocked());
        } else {
            int flag = 0;
            for (List<String> dependenceList : allDependenceList) {
                if (threadName.equals(dependenceList.get(0))) {
                    jsonObject.put("name", threadEntity.isNullEntity() ? "-" : threadName);
                    jsonObject.put("waitLock", threadEntity.getWaitingLock() == null ? "-" : threadEntity.getWaitingLock());
                    jsonObject.put("dependenceName", dependenceList.get(dependenceList.size() - 1));
                    jsonObject.put("lock", threadEntity.getLocked());
                    flag = 1;
                    break;
                }
            }
            if (flag == 0) {
                jsonObject.put("name", threadEntity.isNullEntity() ? "-" : threadName);
                jsonObject.put("waitLock", threadEntity.getWaitingLock() == null ? "-" : threadEntity.getWaitingLock());
                jsonObject.put("dependenceName", sessionThreadLockDependence.get(sessionProcessName).containsKey(threadEntity.getName()) ?
                        sessionThreadLockDependence.get(sessionProcessName).get(threadEntity.getName()) : "-");
                jsonObject.put("lock", threadEntity.getLocked());
            }
        }

        return jsonObject;
    }

    /**
     * @param jsonArray
     * @Description: 根据阻塞的数据进行排序并取前五个
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/23 10:39
     */
    private JSONArray renderChildrenNodeCountSort(JSONArray jsonArray) {
        JSONArray resultJsonArray = new JSONArray();
        List<JSONObject> jsonObjectList = JSONObject.parseArray(jsonArray.toJSONString(), JSONObject.class);
        Collections.sort(jsonObjectList, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                if ((int) o1.get("childrenNodeCount") == (int) o2.get("childrenNodeCount"))
                    return 0;
                if ((int) o1.get("childrenNodeCount") > (int) o2.get("childrenNodeCount"))
                    return -1;
                return 1;
            }
        });
        for (JSONObject jsonObject : jsonObjectList) {
            resultJsonArray.add(jsonObject);
        }
        return resultJsonArray;
    }

    /**
     * @param jsonObject
     * @param allDependenceList
     * @Description: 渲染死循环
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/23 9:41
     */
    private void renderTreeDeadLock(JSONObject jsonObject, List<List<String>> allDependenceList) {
        JSONObject currentNodeJsonObject = jsonObject;
        for (List<String> dependenceList : allDependenceList) {

            if (!dependenceList.get(0).equals(jsonObject.get("name"))) {
                continue;
            }
            //修改属性，存在死循环
            jsonObject.put("deadLock", 1);
            for (int i = 1; i < dependenceList.size(); i++) {
                String threadName = dependenceList.get(i);
                JSONArray childrenArray = (JSONArray) currentNodeJsonObject.get("children");
                for (int j = 0; j < childrenArray.size(); j++) {
                    JSONObject childrenObject = (JSONObject) childrenArray.get(j);
                    if (threadName.equals(childrenObject.get("name"))) {
                        currentNodeJsonObject = childrenObject;
                        JSONObject lineStyle = new JSONObject();
                        lineStyle.put("color", "rgba(255,0,0,1)");
                        currentNodeJsonObject.put("lineStyle", lineStyle);
                        JSONObject itemStyle = new JSONObject();
                        itemStyle.put("color", "rgba(255,0,0,1)");
                        itemStyle.put("borderColor", "rgba(255,0,0,1)");
                        currentNodeJsonObject.put("itemStyle", itemStyle);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param treeSeriesData
     * @Description: 得到空线程的堆栈信息
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/23 9:50
     */
    public JSONObject getNullThreadStack(String sessionProcessName, JSONArray treeSeriesData) {
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < treeSeriesData.size(); i++) {
            String threadName = treeSeriesData.getJSONObject(i).getString("name");
            ThreadEntity threadEntity = sessionThreadNameToThreadEntityMap.get(sessionProcessName).get(threadName);
            if (threadEntity.isNullEntity()) {
                JSONArray childrenJsonArray = treeSeriesData.getJSONObject(i).getJSONArray("children");
                String nullThreadStackInfo = getNullThreadStackInfo(sessionProcessName, childrenJsonArray, threadEntity.getLocked().get(0));
                jsonObject.put(threadName, buildFormatData(nullThreadStackInfo));
            }
        }
        return jsonObject;
    }

    public JSONObject getAllThreadStack(List<ThreadEntity> threadEntities) {
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < threadEntities.size(); i++) {
            ThreadEntity threadEntity = threadEntities.get(i);
            String formatData = buildFormatData(threadEntity.getSummaryInformation());
            jsonObject.put(threadEntity.getName(), formatData);
        }
        return jsonObject;
    }

    /**
     * @param summaryInformation
     * @Description: 把原始堆栈信息格式化，方便在前端展示
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/23 9:51
     */
    private String buildFormatData(String summaryInformation) {
        String[] lines = summaryInformation.split("\n");
        String formatData = "";
        for (String line : lines) {
            if ("".equals(line)) {
                formatData = formatData + "<br/>";
                continue;
            }
            String tempLine = line.replace("\t", "    ").replace(" ", "&nbsp&nbsp") + "<br/>";
            Pattern pattern = Pattern.compile("(- waiting to lock <)|(- locked <)|(- parking to wait for  <)|(Locked ownable synchronizers:)");
            Matcher matcher = pattern.matcher(tempLine.replace("&nbsp&nbsp", " "));
            if (matcher.find()) {
                tempLine = "<font color='red'>" + tempLine + "</font>";
            }
            formatData = formatData + tempLine;
        }
        return formatData;
    }

    public JSONObject getHistogramDistribution(JSONArray treeSeriesData) {
        JSONObject jsonObject = new JSONObject();

        //key为线程的名称  value为该线程阻塞了多少线程
        Map<String, Integer> threadCount = new HashMap<>();
        for (int i = 0; i < treeSeriesData.size(); i++) {
            JSONObject tempJsonObject = (JSONObject) treeSeriesData.get(i);
            threadCount.put((String) tempJsonObject.get("name"), (int) tempJsonObject.get("childrenNodeCount")-1);
        }
        //转化为排序的list
        List<Map.Entry<String, Integer>> threadCountList = new ArrayList<>(threadCount.entrySet());
        Collections.sort(threadCountList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                if (o1.getValue() == o2.getValue()) {
                    return 0;
                }
                return o1.getValue() > o2.getValue() ? -1 : 1;
            }
        });

        JSONArray histogramXAxisData = new JSONArray();
        JSONArray histogramSeriesData = new JSONArray();
        for (int i = 0; i < threadCountList.size(); i++) {
            histogramXAxisData.add(threadCountList.get(i).getKey());
            histogramSeriesData.add(threadCountList.get(i).getValue());
        }
        jsonObject.put("histogramXAxisData", histogramXAxisData);
        jsonObject.put("histogramSeriesData", histogramSeriesData);
        return jsonObject;
    }

    public void identifyLocksNotOwn(List<ThreadEntity> threadEntities) {
        Map<String, String> lockContainedByThread = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            for (String lock : threadEntity.getLocked()) {
                lockContainedByThread.put(lock, threadEntity.getName());
            }
        }
        //如果一个锁没有被一个线程拥有（针对的是wait状态），就构造一个空的线程实体，且这个实体拥有这个锁
        List<ThreadEntity> nullThreadEntities = new ArrayList<>();
        for (ThreadEntity threadEntity : threadEntities) {
            String waitingLock = threadEntity.getWaitingLock();
            if (waitingLock == null) {
                continue;
            }
            if (!lockContainedByThread.containsKey(waitingLock)) {
                lockContainedByThread.put(waitingLock, waitingLock);
                ThreadEntity nullThreadEntity = new ThreadEntity();
                nullThreadEntity.setSummaryInformation("");
                nullThreadEntity.getLocked().add(waitingLock);
                nullThreadEntity.setName(waitingLock);
                nullThreadEntity.setNullEntity(true);
                nullThreadEntities.add(nullThreadEntity);
            }
        }
        threadEntities.addAll(nullThreadEntities);
    }

    public JSONObject getAllThreadStack(String sessionProcessName) {
        return sessionAllThreadStack.get(sessionProcessName);
    }
}

