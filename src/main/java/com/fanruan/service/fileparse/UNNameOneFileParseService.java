package com.fanruan.service.fileparse;

import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 堆栈文件解析（因为不知道该怎么取名字，就命名为了UNName）,文件的格式如下
 * Attaching to process ID 35995, please wait...
 * Debugger attached successfully.
 * Server compiler detected.
 * JVM version is 25.191-b12
 * Deadlock Detection:
 * <p>
 * No deadlocks found.
 * <p>
 * Thread 82763: (state = BLOCKED)
 * - sun.misc.Unsafe.park(boolean, long) @bci=0 (Compiled frame; information may be imprecise)
 * - java.util.concurrent.locks.LockSupport.parkNanos(java.lang.Object, long) @bci=20, line=215 (Compiled frame)
 * - java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(java.util.concurrent.SynchronousQueue$TransferStack$SNode, boolean, long) @bci=160, line=460 (Compiled frame)
 * - java.util.concurrent.SynchronousQueue$TransferStack.transfer(java.lang.Object, boolean, long) @bci=102, line=362 (Compiled frame)
 * - java.util.concurrent.SynchronousQueue.poll(long, java.util.concurrent.TimeUnit) @bci=11, line=941 (Compiled frame)
 * - java.lang.Thread.run() @bci=11, line=748 (Compiled frame)
 * <p>
 * <p>
 * Thread 82623: (state = BLOCKED)
 * - sun.misc.Unsafe.park(boolean, long) @bci=0 (Compiled frame; information may be imprecise)
 * - java.util.concurrent.locks.LockSupport.parkNanos(java.lang.Object, long) @bci=20, line=215 (Compiled frame)
 * - java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(java.util.concurrent.SynchronousQueue$TransferStack$SNode, boolean, long) @bci=160, line=460 (Compiled frame)
 * - java.lang.Thread.run() @bci=11, line=748 (Compiled frame)
 * @author: Henry.Wang
 * @create: 2020/03/31 13:54
 */
public class UNNameOneFileParseService {
    public ProcessEntity parseProcessEntity(String processData) {
        ProcessEntity processEntity = new UNNameOneProcessEntity(processData);
        processEntity.setFileFlag(1);
        return processEntity;
    }

    /**
     * @description: 线程实体
     * @author: Henry.Wang
     * @create: 2020/03/12 10:30
     */
    class UNNameOneThreadEntity extends ThreadEntity {
        UNNameOneThreadEntity(String summaryInformation) {
            initThreadEntity(summaryInformation);
        }

        private void initThreadEntity(String summaryInformation) {
            this.setSummaryInformation(summaryInformation);
            parseStateAndName(summaryInformation);
            parseLock(summaryInformation);
        }
        /**
         * @Description:这种格式的文件还没有遇到有锁的情况，所以暂时没有解析
         * @param data
         * @return:
         * @Author: Henry.Wang
         * @date: 2020/4/1 16:31
         */
        private void parseLock(String data) {

        }

        private void parseStateAndName(String data) {
            Pattern pattern = Pattern.compile("^Thread (\\d+?): \\(state = (\\S+?)\\)");
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                this.setName(matcher.group(1));
                this.setState(matcher.group(2));
            }
        }
    }

    class UNNameOneProcessEntity extends ProcessEntity {
        UNNameOneProcessEntity(String summaryInformation) {
            initProcessEntity(summaryInformation);
        }

        private void initProcessEntity(String summaryInformation) {
            this.setSummaryInformation(summaryInformation);
            parseSummaryInformation(summaryInformation);
        }

        private void parseSummaryInformation(String summaryInformation) {
            Matcher threadStackDataMatcher = patternThreadStackData(summaryInformation);
            createThreadEntity(threadStackDataMatcher);
        }

        private Matcher patternThreadStackData(String summaryInformation) {
            Pattern pattern = Pattern.compile("Thread \\d+?: \\(state = \\S+?\\)\n( - .+?\n)*");
            Matcher matcher = pattern.matcher(summaryInformation);
            return matcher;
        }

        private void createThreadEntity(Matcher threadStackDataMatcher) {
            while (threadStackDataMatcher.find()) {
                String stackData = threadStackDataMatcher.group();
                if (checkFRStack(stackData)) {
                    ThreadEntity threadEntity = new UNNameOneThreadEntity(stackData);
                    this.getThreadEntities().add(threadEntity);
                }
            }
        }

        private boolean checkFRStack(String threadData) {
            Pattern pattern = Pattern.compile(" - com\\.fr");
            Matcher matcher = pattern.matcher(threadData);
            if (matcher.find()) {
                return true;
            } else {
                return false;
            }
        }
    }
}
