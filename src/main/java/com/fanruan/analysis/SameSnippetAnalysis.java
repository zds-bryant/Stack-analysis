package com.fanruan.analysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.common.Tools;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;
import com.fanruan.service.StackCompareService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.parseObject;

/**
 * @description: 相同片段分析
 * @author: Henry.Wang
 * @create: 2020/03/24 15:45
 */
@Service
public class SameSnippetAnalysis extends Analysis {

    @Autowired
    BatchProcessEntity batchProcessEntity;

    @Autowired
    Tools tools;

    Map<String, Map<String, String>> sessionLockContainedByThread = new HashMap<>();
    Map<String, Map<String, List<String>>> sessionThreadsCompeteLock = new HashMap<>();
    Map<String, Map<String, String>> sessionThreadLockDependence = new HashMap<>();
    Map<String, Map<String, ThreadEntity>> sessionThreadNameToThreadEntityMap = new HashMap<>();

    int similarStackLengthThreshold = 8;

    @Override
    public void filterResultJsonExecutor(String sessionProcessName, String resultJson, String threadStateFilter, String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        //原始的结果
        JSONObject resultJsonObject = (JSONObject) JSONObject.parse(resultJson);
        //原始的相同片段
        JSONArray similarStackJsonArray = resultJsonObject.getJSONArray("similarStack");
        //原始相同片段对应的线程信息
        JSONArray similarStackThreadDataJsonArray = resultJsonObject.getJSONArray("similarStackThreadData");
        JSONArray filterSimilarStackJsonArray = new JSONArray();
        JSONArray filterSimilarStackThreadDataJsonArray = new JSONArray();
        for (int i = 0; i < similarStackJsonArray.size(); i++) {
            //得到每一个相同片段过滤后的线程名称
            JSONArray filterThreadNameJsonArray = threadFilterRunner(sessionProcessName,similarStackJsonArray.getString(i), similarStackThreadDataJsonArray.getJSONArray(i),
                    threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
            if (filterThreadNameJsonArray.size() > 0) {
                //添加相同片段文本
                filterSimilarStackJsonArray.add(similarStackJsonArray.get(i));
                //添加过滤后相同片段对应的线程信息
                JSONArray similarStackThreadData = similarStackThreadDataJsonArray.getJSONArray(i);
                JSONArray filterSimilarStackThreadData = new JSONArray();
                for (int j = 0; j < similarStackThreadData.size(); j++) {
                    JSONObject eachStackJsonObject = similarStackThreadData.getJSONObject(j);
                    String threadName = eachStackJsonObject.getString("threadName");
                    if (filterThreadNameJsonArray.contains(threadName)) {
                        filterSimilarStackThreadData.add(eachStackJsonObject);
                    }
                }
                filterSimilarStackThreadDataJsonArray.add(filterSimilarStackThreadData);
            }
        }
        this.filterResultJsonObject.put("similarStack", filterSimilarStackJsonArray);
        this.filterResultJsonObject.put("similarStackThreadData", filterSimilarStackThreadDataJsonArray);
    }

    /**
     * @param similarStack
     * @param similarStackThreadData
     * @param threadStateFilter
     * @param threadGroupNameFilter
     * @param stackPatternFilter
     * @Description: 有两部分要过滤，一个是相似片段，一个是相似片段对应的线程
     * @return: 过滤后相似片段对应的线程，如果相似片段都没有匹配上就返回空的数组
     * @Author: Henry.Wang
     * @date: 2020/4/13 14:24
     */
    public JSONArray threadFilterRunner(String sessionProcessName, String similarStack, JSONArray similarStackThreadData, String threadStateFilter,
                                        String threadGroupNameFilter, String stackPatternFilter, List<String> blockAllThreadFilter) {
        JSONArray filterThreadNameJsonArray = new JSONArray();
        //筛选相似片段
        Pattern pattern = Pattern.compile("java\\.lang\\.Thread\\.State: ([A-Z_]+)");
        Matcher matcher = pattern.matcher(similarStack);
        if (matcher.find()) {
            if (!tools.filterThreadRunner("", matcher.group(1), similarStack,
                    threadStateFilter, "", stackPatternFilter)) {
                return filterThreadNameJsonArray;
            }
        }
        //筛选相似片段对应的线程
        JSONArray allThreadNameJsonArray = new JSONArray();
        for (int i = 0; i < similarStackThreadData.size(); i++) {
            JSONObject eachStackJsonObject = similarStackThreadData.getJSONObject(i);
            String threadName = eachStackJsonObject.getString("threadName");
            allThreadNameJsonArray.add(threadName);
        }
        filterThreadNameJsonArray = tools.filterThread(sessionThreadNameToThreadEntityMap.get(sessionProcessName), allThreadNameJsonArray,
                threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
        return filterThreadNameJsonArray;
    }

    @Override
    public void resultJsonExecutor(String sessionProcessName) {
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = processEntity.getThreadEntities();
        initTools(sessionProcessName,threadEntities);
        List<PreprocessThreadStack> preprocessThreadStacks = getPreprocessThreadEntity(threadEntities);
        List<List<PreprocessThreadStack>> similarStackList = getSimilarStack(preprocessThreadStacks);
        Collections.sort(similarStackList, new Comparator<List<PreprocessThreadStack>>() {
            @Override
            public int compare(List<PreprocessThreadStack> o1, List<PreprocessThreadStack> o2) {
                int len1 = o1.size();
                int len2 = o2.size();
                if (len1 == len2)
                    return 0;
                return len1 > len2 ? -1 : 1;
            }
        });
        buildDisplayData(similarStackList);
    }

    public String getStackCompare(String aStack, String bStack, String threadName) {
        List<String> aStackList = Arrays.asList(aStack.split("<br/>"));
        List<String> bStackList = Arrays.asList(bStack.split("<br/>"));
        StackCompareService stackCompareService = new StackCompareService(aStackList, bStackList);
        String aResultStackList = stackCompareService.getAResultStack();
        String bResultStackList = stackCompareService.getBResultStack();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("aResultStackList", aResultStackList);
        jsonObject.put("bResultStackList", bResultStackList);
        jsonObject.put("threadName", threadName);
        return jsonObject.toJSONString();
    }

    public void buildDisplayData(List<List<PreprocessThreadStack>> similarStackList) {
        JSONArray similarStackJsonArray = new JSONArray();
        JSONArray similarStackThreadDataJsonArray = new JSONArray();
        for (List<PreprocessThreadStack> similarStack : similarStackList) {
            PreprocessThreadStack maxSimilarStack = getMaxSimilarStack(similarStack);
            makeSimilarStack(similarStackJsonArray, maxSimilarStack);
            makeSimilarStackThreadName(maxSimilarStack, similarStackThreadDataJsonArray, similarStack);
        }
        this.resultJsonObject.put("similarStack", similarStackJsonArray);
        this.resultJsonObject.put("similarStackThreadData", similarStackThreadDataJsonArray);
    }

    private void makeSimilarStackThreadName(PreprocessThreadStack aStack, JSONArray similarStackThreadDataJsonArray, List<PreprocessThreadStack> similarStack) {
        Map<PreprocessThreadStack, Double> cosDistanceMap = new HashMap<>();
        for (PreprocessThreadStack bStack : similarStack) {
            cosDistanceMap.put(bStack, getCosDistance(aStack, bStack));
        }
        List<Map.Entry<PreprocessThreadStack, Double>> sortCosDistanceList = new ArrayList<>(cosDistanceMap.entrySet());
        Collections.sort(sortCosDistanceList, new Comparator<Map.Entry<PreprocessThreadStack, Double>>() {
            @Override
            public int compare(Map.Entry<PreprocessThreadStack, Double> o1, Map.Entry<PreprocessThreadStack, Double> o2) {
                if (o1.getValue().equals(o2.getValue()))
                    return 0;
                return o1.getValue() > o2.getValue() ? -1 : 1;
            }
        });
        JSONArray stackValueJSONArray = new JSONArray();
        for (Map.Entry<PreprocessThreadStack, Double> eachStack : sortCosDistanceList) {
            JSONObject eachStackJsonObject = new JSONObject();
            eachStackJsonObject.put("threadName", eachStack.getKey().threadName);
            //之所以屏蔽，是因为前端存储了所有的堆栈信息，这样就节约很大的带宽
            //eachStackJsonObject.put("originalStack", eachStack.getKey().getOriginalStack().replaceAll("\n", "<br/>"));
            DecimalFormat df = new DecimalFormat("#.00");
            eachStackJsonObject.put("cosDistance", df.format(eachStack.getValue()));
            eachStackJsonObject.put("state", eachStack.getKey().getState());
            stackValueJSONArray.add(eachStackJsonObject);
        }
        similarStackThreadDataJsonArray.add(stackValueJSONArray);
    }

    private void makeSimilarStack(JSONArray similarStackJsonArray, PreprocessThreadStack maxSimilarStack) {
        String originalStack = maxSimilarStack.getOriginalStack();
        similarStackJsonArray.add(originalStack.replaceAll("\n", "<br/>"));
    }

    /**
     * @param similarStackList
     * @Description: 从相似的堆栈中找到最有代表的堆栈
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/4/3 15:49
     */
    private PreprocessThreadStack getMaxSimilarStack(List<PreprocessThreadStack> similarStackList) {
        PreprocessThreadStack maxSimilarStack = null;
        double maxCosDistance = 0;
        for (PreprocessThreadStack aStack : similarStackList) {
            double sumCosDistance = 0;
            for (PreprocessThreadStack bStack : similarStackList) {
                double cosDistance = getCosDistance(aStack, bStack);
                sumCosDistance = sumCosDistance + cosDistance;
            }
            if (sumCosDistance > maxCosDistance) {
                maxCosDistance = sumCosDistance;
                maxSimilarStack = aStack;
            }
        }
        return maxSimilarStack;
    }

    public List<List<PreprocessThreadStack>> getSimilarStack(List<PreprocessThreadStack> preprocessThreadStacks) {
        List<List<PreprocessThreadStack>> similarStackList = new ArrayList<>();
        List<Integer> travelPos = new ArrayList<>();
        for (int i = 0; i < preprocessThreadStacks.size(); i++) {
            if (travelPos.contains(i)) {
                continue;
            }
            PreprocessThreadStack aStack = preprocessThreadStacks.get(i);
            List<PreprocessThreadStack> tempList = new ArrayList<>();
            for (int j = i; j < preprocessThreadStacks.size(); j++) {
                if (travelPos.contains(j)) {
                    continue;
                }
                PreprocessThreadStack bStack = preprocessThreadStacks.get(j);
                double cosDistance = getCosDistance(aStack, bStack);
                if (cosDistance > 0.80) {
                    travelPos.add(j);
                }
                if (cosDistance > getCosDistanceThreshold(aStack, bStack)) {
                    tempList.add(bStack);
                }
            }
            if (tempList.size() > getSimilarStackLengthThreshold()) {
                similarStackList.add(tempList);
            }
        }
        return similarStackList;
    }

    private double getCosDistanceThreshold(PreprocessThreadStack aStack, PreprocessThreadStack bStack) {
        //对于很长的栈，值域是0.4
        //对于很短的栈，值域最大 为0.95；
        double sumCount = Math.max(Math.min(aStack.lineCount + bStack.lineCount, 20), 8);
        return 8 / sumCount - 0.05;
    }

    /**
     * @param threadEntities
     * @Description:对堆栈的内容预处理一下
     * @return:
     * @Author: Henry.Wang
     * @date: 2020/3/31 15:39
     */
    private List<PreprocessThreadStack> getPreprocessThreadEntity(List<ThreadEntity> threadEntities) {
        List<PreprocessThreadStack> preprocessThreadStackList = new ArrayList<>();
        for (ThreadEntity threadEntity : threadEntities) {
            preprocessThreadStackList.add(new PreprocessThreadStack(threadEntity));
        }
        return preprocessThreadStackList;
    }

    private double getCosDistance(PreprocessThreadStack aStack, PreprocessThreadStack bStack) {
        Map<String, Integer> lineDict = getLineDict(aStack, bStack);
        int[] aVector = getStackVector(lineDict, aStack);
        int[] bVector = getStackVector(lineDict, bStack);
        double cosDistance = getCosDistanceRunner(aVector, bVector);
        return cosDistance;
    }

    private double getCosDistanceRunner(int[] a, int[] b) {
        double aa = Math.sqrt(Arrays.stream(a).map(e -> e * e).sum());
        double bb = Math.sqrt(Arrays.stream(b).map(e -> e * e).sum());
        double ab = 0;
        for (int i = 0; i < a.length; i++) {
            ab = ab + a[i] * b[i];
        }
        return ab / (aa * bb);
    }

    private int[] getStackVector(Map<String, Integer> lineDict, PreprocessThreadStack stack) {
        int[] vector = new int[lineDict.keySet().size()];

        List<String> lines = stack.getStackLines();
        for (String line : lines) {
            int mark = lineDict.get(line);
            vector[mark] = vector[mark] + 1;
        }
        return vector;
    }

    private Map<String, Integer> getLineDict(PreprocessThreadStack aStack, PreprocessThreadStack bStack) {
        Map<String, Integer> lineCount = new HashMap<>();

        for (String line : aStack.getStackLines()) {
            tools.increaseProgressiveMap(lineCount, line);
        }
        for (String line : bStack.getStackLines()) {
            tools.increaseProgressiveMap(lineCount, line);
        }

        Map<String, Integer> lineDict = new HashMap<>();
        int mark = 0;
        for (String line : lineCount.keySet()) {
            // if (lineCount.get(line) < preprocessThreadEntity.size() * 0.2) {
            lineDict.put(line, mark);
            mark = mark + 1;
            // }
        }
        return lineDict;
    }

    public void initTools(String sessionProcessName, List<ThreadEntity> threadEntities) {
        sessionLockContainedByThread.put(sessionProcessName, tools.findLockContainedByThread(threadEntities));
        sessionThreadsCompeteLock.put(sessionProcessName, tools.getThreadsCompeteLock(threadEntities));
        sessionThreadLockDependence.put(sessionProcessName, tools.findThreadLockDependence(threadEntities, sessionLockContainedByThread.get(sessionProcessName)));
        sessionThreadNameToThreadEntityMap.put(sessionProcessName, tools.getThreadNameToThreadEntityMap(threadEntities));
    }

    public int getSimilarStackLengthThreshold() {
        return this.similarStackLengthThreshold;
    }

    class PreprocessThreadStack {
        //线程的名称
        private String threadName;
        //把栈信息转化为List对象，对每一行处理的时候会屏蔽掉锁信息
        private List<String> stackLines = new ArrayList<>();
        //栈一共多少行
        private int lineCount;
        //原始的栈信息
        private String originalStack = "";
        //线程状态
        private String state;

        PreprocessThreadStack(ThreadEntity threadEntity) {
            String threadName = threadEntity.getName();
            this.threadName = threadName;
            String stackData = threadEntity.getSummaryInformation();
            String[] lines = stackData.split("\n");
            lineCount = Math.max(0, lines.length - 1);
            this.state = threadEntity.getState();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                originalStack = originalStack + line + "\n";
                line = line.replaceAll("(\\s+-\\s+waiting\\s+to\\s+lock\\s+<.+?>)|(\\s+-\\s+locked\\s+<.+?>)|(\\s+-\\s+parking\\s+to\\s+wait\\s+for\\s+<.+?>)", "lock.line");
                stackLines.add(line);
            }
        }

        public String getThreadName() {
            return threadName;
        }

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }

        public List<String> getStackLines() {
            return stackLines;
        }

        public void setStackLines(List<String> stackLines) {
            this.stackLines = stackLines;
        }

        public int getLineCount() {
            return lineCount;
        }

        public void setLineCount(int lineCount) {
            this.lineCount = lineCount;
        }

        public String getOriginalStack() {
            return originalStack;
        }

        public void setOriginalStack(String originalStack) {
            this.originalStack = originalStack;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }
}
