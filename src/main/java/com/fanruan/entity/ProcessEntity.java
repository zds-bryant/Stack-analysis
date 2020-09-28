package com.fanruan.entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Henry.Wang
 * @description 一个进程的实体
 * @date 2020/3/5
 */
public class ProcessEntity {
    public String createDate;
    public String version;
    public List<ThreadEntity> threadEntities = new ArrayList<ThreadEntity>();
    public String summaryInformation;
    public int fileFlag;
    private int deadLockCount = 0;
    private List<List<String>> allDeadLockLoop = new ArrayList<>();

    public List<List<String>> getAllDeadLockLoop() {
        return allDeadLockLoop;
    }

    public void setAllDeadLockLoop(List<List<String>> allDeadLockLoop) {
        this.allDeadLockLoop = allDeadLockLoop;
    }

    public int getFileFlag() {
        return fileFlag;
    }

    public void setFileFlag(int fileFlag) {
        this.fileFlag = fileFlag;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ThreadEntity> getThreadEntities() {
        return threadEntities;
    }

    public void setThreadEntities(List<ThreadEntity> threadEntities) {
        this.threadEntities = threadEntities;
    }

    public String getSummaryInformation() {
        return summaryInformation;
    }

    public void setSummaryInformation(String summaryInformation) {
        this.summaryInformation = summaryInformation;
    }

    public int getDeadLockCount() {
        return deadLockCount;
    }

    public void setDeadLockCount(int deadLockCount) {
        this.deadLockCount = deadLockCount;
    }
}
