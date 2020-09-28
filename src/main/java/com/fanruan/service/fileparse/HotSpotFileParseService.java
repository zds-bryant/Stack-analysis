package com.fanruan.service.fileparse;

import com.fanruan.common.Tools;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 解析HostSpot虚拟机生成的stack文件，文件的格式如下
 * 2020-03-04 16:13:08
 * Full thread dump Java HotSpot(TM) 64-Bit Server VM (25.171-b11 mixed mode):
 * <p>
 * "t3" #11 prio=5 os_prio=0 tid=0x00007f312c0d9000 nid=0x413 waiting for monitor entry [0x00007f310fbfa000]
 * java.lang.Thread.State: BLOCKED (on object monitor)
 * at SyncThread.run(ThreadDeadlock.java:38)
 * - waiting to lock <0x00000000d675d210> (a java.lang.Object)
 * - locked <0x00000000d675d230> (a java.lang.Object)
 * at java.lang.Thread.run(Thread.java:748)
 * <p>
 * Locked ownable synchronizers:
 * - None
 * <p>
 * "Signal Dispatcher" #4 daemon prio=9 os_prio=0 tid=0x00007f312c0b7800 nid=0x386 runnable [0x0000000000000000]
 * java.lang.Thread.State: RUNNABLE
 * <p>
 * Locked ownable synchronizers:
 * - None
 * <p>
 * "Finalizer" #3 daemon prio=8 os_prio=0 tid=0x00007f312c084800 nid=0x385 in Object.wait() [0x00007f311c6f5000]
 * java.lang.Thread.State: WAITING (on object monitor)
 * at java.lang.Object.wait(Native Method)
 * - waiting on <0x00000000d6708ed0> (a java.lang.ref.ReferenceQueue$Lock)
 * at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:143)
 * - locked <0x00000000d6708ed0> (a java.lang.ref.ReferenceQueue$Lock)
 * at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:164)
 * at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:212)
 * <p>
 * Locked ownable synchronizers:
 * - None
 * <p>
 * "Reference Handler" #2 daemon prio=10 os_prio=0 tid=0x00007f312c080000 nid=0x384 in Object.wait() [0x00007f311c7f6000]
 * java.lang.Thread.State: WAITING (on object monitor)
 * at java.lang.Object.wait(Native Method)
 * - waiting on <0x00000000d6706bf8> (a java.lang.ref.Reference$Lock)
 * at java.lang.Object.wait(Object.java:502)
 * at java.lang.ref.Reference.tryHandlePending(Reference.java:191)
 * - locked <0x00000000d6706bf8> (a java.lang.ref.Reference$Lock)
 * at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:153)
 * <p>
 * Locked ownable synchronizers:
 * - None
 * <p>
 * "VM Thread" os_prio=0 tid=0x00007f312c078000 nid=0x383 runnable
 * <p>
 * "GC task thread#0 (ParallelGC)" os_prio=0 tid=0x00007f312c01f800 nid=0x37f runnable
 * <p>
 * "GC task thread#1 (ParallelGC)" os_prio=0 tid=0x00007f312c021000 nid=0x380 runnable
 * <p>
 * "GC task thread#2 (ParallelGC)" os_prio=0 tid=0x00007f312c023000 nid=0x381 runnable
 * <p>
 * "GC task thread#3 (ParallelGC)" os_prio=0 tid=0x00007f312c024800 nid=0x382 runnable
 * <p>
 * "VM Periodic Task Thread" os_prio=0 tid=0x00007f312c0c5800 nid=0x38b waiting on condition
 * <p>
 * JNI global references: 5
 * <p>
 * <p>
 * Found one Java-level deadlock:
 * =============================
 * "t3":
 * waiting to lock monitor 0x00007f30fc0062c8 (object 0x00000000d675d210, a java.lang.Object),
 * which is held by "t1"
 * "t1":
 * waiting to lock monitor 0x00007f30fc002178 (object 0x00000000d675d220, a java.lang.Object),
 * which is held by "t2"
 * "t2":
 * waiting to lock monitor 0x00007f30fc006218 (object 0x00000000d675d230, a java.lang.Object),
 * which is held by "t3"
 * <p>
 * Java stack information for the threads listed above:
 * ===================================================
 * "t3":
 * at SyncThread.run(ThreadDeadlock.java:38)
 * - waiting to lock <0x00000000d675d210> (a java.lang.Object)
 * - locked <0x00000000d675d230> (a java.lang.Object)
 * at java.lang.Thread.run(Thread.java:748)
 * "t1":
 * at SyncThread.run(ThreadDeadlock.java:38)
 * - waiting to lock <0x00000000d675d220> (a java.lang.Object)
 * - locked <0x00000000d675d210> (a java.lang.Object)
 * at java.lang.Thread.run(Thread.java:748)
 * "t2":
 * at SyncThread.run(ThreadDeadlock.java:38)
 * - waiting to lock <0x00000000d675d230> (a java.lang.Object)
 * - locked <0x00000000d675d220> (a java.lang.Object)
 * at java.lang.Thread.run(Thread.java:748)
 * <p>
 * Found 1 deadlock.
 * @author: Henry.Wang
 * @create: 2020/03/12 10:30
 */
public class HotSpotFileParseService {
    public ProcessEntity parseProcessEntity(String processData) {
        ProcessEntity processEntity = new HotSpotProcessEntity(processData);
        processEntity.setFileFlag(0);
        return processEntity;
    }

    /**
     * @description: 线程实体
     * @author: Henry.Wang
     * @create: 2020/03/12 10:30
     */
    class HotSpotThreadEntity extends ThreadEntity {
        HotSpotThreadEntity(String summaryInformation) {
            initThreadEntity(summaryInformation);
        }

        private void initThreadEntity(String summaryInformation) {
            this.setSummaryInformation(summaryInformation);
            //把线程的信息分成一行一行的数组
            String[] lines = getLinesInformation(summaryInformation);
            //解析第一行数据，包含prio、os_prio、tid等信息
            parseFirstLine(lines[0]);
            //解析第二行数据，包含线程状态信息
            parseState(lines[1]);
            //解析锁信息
            parseLock(summaryInformation);
        }

        private String[] getLinesInformation(String summaryInformation) {
            return summaryInformation.split("\n");
        }

        private void parseLock(String data) {
            //内置锁
            Pattern pattern = Pattern.compile("waiting to lock <(.+?)>");
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                this.setWaitingLock(matcher.group(1));
            }

            pattern = Pattern.compile("locked <(.+?)>");
            matcher = pattern.matcher(data);
            while (matcher.find()) {
                this.getLocked().add(matcher.group(1));
            }

            //JUC提供的锁
            pattern = Pattern.compile("- parking to wait for  <(.+?)>");
            matcher = pattern.matcher(data);
            if (matcher.find()) {
                this.setParkWaitLock(matcher.group(1));
            }
            pattern = Pattern.compile("Locked ownable synchronizers:(.+)$");
            matcher = pattern.matcher(data.replaceAll("\n", ""));
            if (matcher.find()) {
                String temp = matcher.group(1);
                pattern = Pattern.compile("- <(.+?)>");
                matcher = pattern.matcher(temp);
                List<String> parkLockedList = new ArrayList<>();
                while (matcher.find()) {
                    parkLockedList.add(matcher.group(1));
                }
                this.setParkLocked(parkLockedList);
            }
        }

        private void parseState(String data) {
            Pattern pattern = Pattern.compile("java\\.lang\\.Thread\\.State: ([A-Z_]+)");
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                this.setState(matcher.group(1));
            }
        }

        private void parseFirstLine(String firstLine) {
            Pattern pattern = Pattern.compile("^\"(.+?)\" ");
            Matcher matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                String threadName = matcher.group(1);
                this.setName(threadName.replaceAll("jdbc:mysql://(.+)$", "")
                        .replaceAll("jdbc:sqlserver://(.+)$", ""));
            }
            pattern = Pattern.compile("\" #([0-9]+?) ");
            matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                this.setId(Integer.parseInt(matcher.group(1)));
            }

            pattern = Pattern.compile(" prio=([0-9]+?) ");
            matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                this.setPrio(Integer.parseInt(matcher.group(1)));
            }
            pattern = Pattern.compile(" os_prio=([0-9]+?) ");
            matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                this.setoSPrio(Integer.parseInt(matcher.group(1)));
            }

            pattern = Pattern.compile(" tid=([0-9a-zA-Z]+?) ");
            matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                this.setTid(matcher.group(1));
            }

            pattern = Pattern.compile(" nid=([0-9a-zA-Z]+?) ");
            matcher = pattern.matcher(firstLine);
            if (matcher.find()) {
                this.setNid(matcher.group(1));
            }
        }
    }

    class HotSpotProcessEntity extends ProcessEntity {
        HotSpotProcessEntity(String summaryInformation) {
            initProcessEntity(summaryInformation);
        }

        private void initProcessEntity(String summaryInformation) {
            this.setSummaryInformation(summaryInformation);
            String[] splitDatas = summaryInformation.split("\n");
            //解析时间
            parseCreateDate(splitDatas[0]);
            //解析版本
            parseVersion(splitDatas[1]);
            //线程数据列表
            List<String> threadDatas = new ArrayList<String>();
            String tempThreadData = "";
            for (int i = 2; i < splitDatas.length; i++) {
                if ("".equals(tempThreadData)) {
                    if (isTheadDataStartLine(splitDatas, i)) {
                        tempThreadData = tempThreadData + splitDatas[i] + "\n";
                    } else {
                        if (!("".equals(splitDatas[i]))) {
                            break;
                        }
                    }
                } else {
                    if (isTheadDataLine(splitDatas, i)) {
                        tempThreadData = tempThreadData + splitDatas[i] + "\n";
                    } else {
                        threadDatas.add(tempThreadData);
                        tempThreadData = "";
                    }
                }
            }
            //解析每个线程的堆栈
            parseThreadEntities(threadDatas);
            //解析死锁情况
            parseDeaDLockCount(summaryInformation);
            //解析死锁环
            parseDeaDLockLoop();
        }

        private void parseDeaDLockLoop() {
            List<ThreadEntity> threadEntities = getThreadEntities();
            List<List<String>> allDeadLockLoop =  getAllDeadAndBreakLockLoop(threadEntities);
            setAllDeadLockLoop(allDeadLockLoop);
        }

        /**
         * @param threadEntities
         * @Description: 获取所有的循环依赖并打破循环依赖
         * @return:
         * @Author: Henry.Wang
         * @date: 2020/3/30 15:20
         */
        public List<List<String>> getAllDeadAndBreakLockLoop(List<ThreadEntity> threadEntities) {
            Tools tools = new Tools();
            Map<String, ThreadEntity> threadNameToThreadEntityMap = tools.getThreadNameToThreadEntityMap(threadEntities);
            Map<String, String> lockContainedByThread = tools.findLockContainedByThread(threadEntities);
            List<List<String>> allDeadLockLoop = new ArrayList<>();
            for (ThreadEntity threadEntity : threadEntities) {
                List<String> dependenceList = getDeadLockDependenceList(threadNameToThreadEntityMap, threadEntities, threadEntity.getName(), lockContainedByThread);
                if (dependenceList != null) {
                    allDeadLockLoop.add(dependenceList);
                }
            }
            return allDeadLockLoop;
        }

        /**
         * @param threadEntities
         * @param threadName
         * @Description: 获取循环依赖并打破循环依赖
         * @return: String为线程名称
         * @Author: Henry.Wang
         * @date: 2020/3/15 11:42
         */
        private List<String> getDeadLockDependenceList(Map<String, ThreadEntity> threadNameToThreadEntityMap, List<ThreadEntity> threadEntities,
                                                       String threadName, Map<String, String> lockContainedByThread) {
            ThreadEntity threadEntity = threadNameToThreadEntityMap.get(threadName);
            String tempName = threadName;
            ThreadEntity tempThreadEntity = threadEntity;
            //循环依赖链
            List<String> lockList = new ArrayList<>();
            while (tempName != null) {
                if (lockList.contains(tempName)) {
                    lockList.add(tempName);
                    //打破循环依赖
                    for (ThreadEntity tempEntity : threadEntities) {
                        if (tempName.equals(tempEntity.getName())) {
                            tempEntity.setWaitingLock(null);
                            break;
                        }
                    }
                    lockList = lockList.subList(1, lockList.size());
                    Collections.reverse(lockList);
                    return lockList;
                }
                lockList.add(tempName);
                String lockByThreadName = lockContainedByThread.get(tempThreadEntity.getWaitingLock());
                if (lockByThreadName != null) {
                    tempThreadEntity = threadNameToThreadEntityMap.get(lockByThreadName);
                } else {
                    tempThreadEntity = null;
                }
                if (tempThreadEntity != null) {
                    tempName = tempThreadEntity.getName();
                } else {
                    tempName = null;
                }
            }
            return null;
        }

        private void parseDeaDLockCount(String summaryInformation) {
            String[] lines = summaryInformation.split("\n");
            for (int i = lines.length - 1; i > -1; i--) {
                if (lines[i].length() > 5) {
                    Pattern pattern = Pattern.compile("Found (\\d+?) deadlock.");
                    Matcher matcher = pattern.matcher(lines[i]);
                    if (matcher.find()) {
                        this.setDeadLockCount(Integer.parseInt(matcher.group(1)));
                    }
                    break;
                }
            }
        }


        private void parseCreateDate(String data) {
            this.setCreateDate(data);
        }

        private void parseVersion(String data) {
            this.setVersion(data);
        }

        private void parseThreadEntities(List<String> threadDatas) {
            for (String threadData : threadDatas) {
                if (checkFRStack(threadData)) {
                    ThreadEntity threadEntity = new HotSpotThreadEntity(threadData);
                    this.getThreadEntities().add(threadEntity);
                }
            }
        }

        private boolean checkFRStack(String threadData) {
            Pattern pattern = Pattern.compile("\n\\s+at\\s+com\\.fr");
            Matcher matcher = pattern.matcher(threadData);
            if (matcher.find()) {
                return true;
            } else {
                return false;
            }
        }

        private boolean isTheadDataStartLine(String[] splitdatas, int line) {
            if (line + 1 >= splitdatas.length) {
                return false;
            }
            Pattern pattern = Pattern.compile("java\\.lang\\.Thread\\.State");
            Matcher matcher = pattern.matcher(splitdatas[line + 1]);
            return matcher.find();
        }


        private boolean isTheadDataLine(String[] splitdatas, int line) {
            if ("".equals(splitdatas[line])) {
                if (line + 1 >= splitdatas.length) {
                    return false;
                } else {
                    Pattern pattern = Pattern.compile("^[\t| ]");
                    Matcher matcher = pattern.matcher(splitdatas[line + 1]);
                    return matcher.find();
                }
            }
            Pattern pattern = Pattern.compile("^[\t| ]");
            Matcher matcher = pattern.matcher(splitdatas[line]);
            return matcher.find();
        }
    }
}
