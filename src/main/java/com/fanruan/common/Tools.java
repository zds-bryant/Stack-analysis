package com.fanruan.common;


import com.alibaba.fastjson.JSONArray;
import com.fanruan.entity.ThreadEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Henry.Wang
 * @description 工具
 * @date 2020/3/8
 */
@Component
public class Tools {
    @Autowired
    private MessageSource messageSource;

    private Logger logger = LoggerFactory.getLogger("myLogger");

    public String getSessionProcessName(String sessionId, String processName) {
        return sessionId + "_" + processName;
    }

    public String getProcessNameFromSessionProcessName(String sessionProcessName) {
        Pattern pattern = Pattern.compile("[0-9]_(.*)");
        Matcher matcher = pattern.matcher(sessionProcessName);
        matcher.find();
        return matcher.group(1);
    }

    public Logger getLogger() {
        return logger;
    }

    public String getSessionIdFromSessionProcessName(String sessionProcessName) {
        Pattern pattern = Pattern.compile("(\\d+?)_(.+?)");
        Matcher matcher = pattern.matcher(sessionProcessName);
        matcher.find();
        return matcher.group(1);
    }

    public void increaseProgressiveMap(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }

    /**
     * @param threadEntities
     * @Description: 线程名称到线程实体的映射
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/15 11:31
     */
    public Map<String, ThreadEntity> getThreadNameToThreadEntityMap(List<ThreadEntity> threadEntities) {
        Map<String, ThreadEntity> threadNameToThreadEntityMap = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            threadNameToThreadEntityMap.put(threadEntity.getName(), threadEntity);
        }
        return threadNameToThreadEntityMap;
    }

    /**
     * @param threadEntities
     * @Description: 锁被哪个线程拥有
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/12 11:03
     */
    public Map<String, String> findLockContainedByThread(List<ThreadEntity> threadEntities) {
        Map<String, String> lockContainedByThread = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            for (String lock : threadEntity.getLocked()) {
                lockContainedByThread.put(lock, threadEntity.getName());
            }
        }
        return lockContainedByThread;
    }

    /**
     * @param threadEntities
     * @Description: 一把锁被哪些线程竞争
     * @return: key为锁的，value为竞争该锁的线程
     * @Author: Henry.Wang
     * @date: 2020/3/16 9:30
     */
    public Map<String, List<String>> getThreadsCompeteLock(List<ThreadEntity> threadEntities) {
        Map<String, List<String>> threadsCompeteLock = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            String threadName = threadEntity.getName();
            String competeLock = threadEntity.getWaitingLock();
            if (competeLock == null) {
                continue;
            }
            if (threadsCompeteLock.containsKey(competeLock)) {
                threadsCompeteLock.get(competeLock).add(threadName);
            } else {
                List<String> threadList = new ArrayList<>();
                threadList.add(threadName);
                threadsCompeteLock.put(competeLock, threadList);
            }
        }
        return threadsCompeteLock;
    }

    public String getLocText(String messageKey) {
        String message = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        return message;
    }

    /**
     * @param threadEntities
     * @param lockContainedByThread
     * @Description: 线程之间的锁依赖
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/12 11:11
     */
    public Map<String, String> findThreadLockDependence(List<ThreadEntity> threadEntities, Map<String, String> lockContainedByThread) {
        Map<String, String> threadLockDependence = new HashMap<>();
        for (ThreadEntity threadEntity : threadEntities) {
            String watingLock = threadEntity.getWaitingLock();
            if (watingLock != null) {
                threadLockDependence.put(threadEntity.getName(), lockContainedByThread.get(watingLock));
            }
        }
        return threadLockDependence;
    }

    /**
     * @param threadNameToThreadEntityMap
     * @param allThreadNameJsonArray      要过滤的线程名称
     * @param threadStateFilter
     * @param threadGroupNameFilter
     * @param stackPatternFilter
     * @Description: 过滤线程
     * @return: 过滤后的线程名
     * @Author: Henry.Wang
     * @date: 2020/4/13 14:21
     */
    public JSONArray filterThread(Map<String, ThreadEntity> threadNameToThreadEntityMap, JSONArray allThreadNameJsonArray,
                                  String threadStateFilter, String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        JSONArray filterThreadNameJsonArray = new JSONArray();
        for (int i = 0; i < allThreadNameJsonArray.size(); i++) {
            String threadName = (String) allThreadNameJsonArray.get(i);
            if (blockAllThreadFilter != null && !blockAllThreadFilter.contains(threadName))
                continue;
            ThreadEntity threadEntity = threadNameToThreadEntityMap.get(threadName);
            if(threadEntity==null){
                int jl=0;
            }
            String state = threadEntity.getState();
            String threadSummaryInformation = threadEntity.getSummaryInformation();

            if (filterThreadRunner(threadName, state, threadSummaryInformation,
                    threadStateFilter, threadGroupNameFilter, stackPatternFilter)) {
                filterThreadNameJsonArray.add(threadName);
            }
        }
        return filterThreadNameJsonArray;
    }

    public boolean filterThreadRunner(String threadName, String state, String threadSummaryInformation,
                                      String threadStateFilter, String threadGroupNameFilter, String stackPatternFilter) {
        if (!"".equals(threadStateFilter) && !threadStateFilter.equals(state)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^(.+?)-");
        Matcher matcher = pattern.matcher(threadName);
        if (!"".equals(threadGroupNameFilter) && (!matcher.find() || !threadGroupNameFilter.equals(matcher.group(1)))) {
            return false;
        }
        pattern = Pattern.compile(stackPatternFilter);
        matcher = pattern.matcher(threadSummaryInformation);
        if (!matcher.find()) {
            return false;
        }
        return true;
    }

    public void printAnalysisRequestData(String analysisName, String processName, String sessionId, String threadStateFilter,
                                         String threadGroupNameFilter, String stackPatternFilter, String blockFilter) {

        logger.info(analysisName +
                "|" + processName +
                "|" + sessionId +
                "|" + threadStateFilter +
                "|" + threadGroupNameFilter +
                "|" + stackPatternFilter +
                "|" + blockFilter);
    }
}