package com.fanruan.analysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.entity.ProcessEntity;
import com.fanruan.entity.ThreadEntity;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @description: 针对block模块的相似内存分析
 * @author: Henry.Wang
 * @create: 2020/04/01 10:21
 */
@Service
public class BlockNewSameStackAnalysis extends SameSnippetAnalysis {
    int similarStackLengthThreshold = 1;

    public String getResultJson(String sessionProcessName, List<String> threadNameList) throws UnsupportedEncodingException {
        resultJsonExecutor(sessionProcessName, threadNameList);
        addTitleData();
        String resultJsonString = resultJsonObject.toJSONString();
        return resultJsonString;
    }

    public void resultJsonExecutor(String sessionProcessName, List<String> threadNameList) throws UnsupportedEncodingException {
        ProcessEntity processEntity = batchProcessEntity.processEntityBuild(sessionProcessName);
        List<ThreadEntity> threadEntities = processEntity.getThreadEntities();
        initTools(sessionProcessName, threadEntities);
        List<PreprocessThreadStack> preprocessThreadStacks = getPreprocessThreadEntity(threadEntities, threadNameList);
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

    private List<PreprocessThreadStack> getPreprocessThreadEntity(List<ThreadEntity> threadEntities, List<String> threadNameList) throws UnsupportedEncodingException {
        List<PreprocessThreadStack> preprocessThreadStackList = new ArrayList<>();
        for (ThreadEntity threadEntity : threadEntities) {
            if (threadNameList.contains(threadEntity.getName()) && !threadEntity.isNullEntity()) {
                preprocessThreadStackList.add(new PreprocessThreadStack(threadEntity));
            }
        }
        return preprocessThreadStackList;
    }

    public int getSimilarStackLengthThreshold() {
        return this.similarStackLengthThreshold;
    }
}
