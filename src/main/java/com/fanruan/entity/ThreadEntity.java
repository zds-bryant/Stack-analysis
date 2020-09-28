package com.fanruan.entity;



import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

/**
 * @author Henry.Wang
 * @description 线程实体
 * @date 2020/3/5
 */
public class ThreadEntity {
    private String name;
    private int id;
    private int prio;
    private int oSPrio;
    private String tid;
    private String nid;
    private String state;
    private String waitingLock;
    private List<String> locked = new ArrayList<>();
    private String summaryInformation;
    private boolean nullEntity = false;

    public boolean isNullEntity() {
        return nullEntity;
    }

    public void setNullEntity(boolean nullEntity) {
        this.nullEntity = nullEntity;
    }

    public String parkWaitLock;
    public List<String> parkLocked = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrio() {
        return prio;
    }

    public void setPrio(int prio) {
        this.prio = prio;
    }

    public int getoSPrio() {
        return oSPrio;
    }

    public void setoSPrio(int oSPrio) {
        this.oSPrio = oSPrio;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getWaitingLock() {
        return waitingLock;
    }

    public void setWaitingLock(String waitingLock) {
        this.waitingLock = waitingLock;
    }

    public List<String> getLocked() {
        return locked;
    }

    public void setLocked(List<String> locked) {
        this.locked = locked;
    }

    public String getSummaryInformation() {
        return summaryInformation;
    }

    public void setSummaryInformation(String summaryInformation) {
        this.summaryInformation = summaryInformation;
    }


    public String getParkWaitLock() {
        return parkWaitLock;
    }

    public void setParkWaitLock(String parkWaitLock) {
        this.parkWaitLock = parkWaitLock;
    }

    public List<String> getParkLocked() {
        return parkLocked;
    }

    public void setParkLocked(List<String> parkLocked) {
        this.parkLocked = parkLocked;
    }
}
