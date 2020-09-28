package com.fanruan.controller;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.BlockNewSameStackAnalysis;
import com.fanruan.analysis.SameSnippetAnalysis;
import com.fanruan.common.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;

/**
 * @description: 提取相同的片段
 * @author: Henry.Wang
 * @create: 2020/03/24 15:44
 */
@RestController
public class SameSnippetController {
    @Autowired
    SameSnippetAnalysis sameSnippetAnalysis;

    @Autowired
    Tools tools;

    @Autowired
    BlockNewSameStackAnalysis blockNewSameStackAnalysis;

    @RequestMapping(value = "/sameStack", method = RequestMethod.POST)
    public String sameSnippet(@RequestParam("processName") String processName,
                              @RequestParam("sessionId") String sessionId,
                              @RequestParam("threadStateFilter") String threadStateFilter,
                              @RequestParam("threadGroupNameFilter") String threadGroupNameFilter,
                              @RequestParam("stackPatternFilter") String stackPatternFilter,
                              @RequestParam("blockAllThreadFilter") String blockAllThreadFilterBase64Encode,
                              @RequestParam("blockFilter") String blockFilter) throws UnsupportedEncodingException {
        tools.printAnalysisRequestData("sameSnippet",processName, sessionId, threadStateFilter, threadGroupNameFilter, stackPatternFilter,blockFilter);
        String threadNamesBase64Decode = new String(Base64.getDecoder().decode(blockAllThreadFilterBase64Encode), "UTF-8");
        List<String> threadNameList = JSONObject.parseArray(threadNamesBase64Decode, String.class);
        String resultJson;
        if (threadNameList != null) {
            resultJson = blockNewSameStackAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName), threadNameList);
        } else {
            resultJson = sameSnippetAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
        }
        String filterResultJson = sameSnippetAnalysis.getFilterResultJson(tools.getSessionProcessName(sessionId, processName),
                resultJson, threadStateFilter, threadGroupNameFilter, stackPatternFilter, "");
        return filterResultJson;
    }

    @RequestMapping(value = "/sameStack/compare", method = RequestMethod.POST)
    public String sameSnippetCompare(@RequestParam("aStack") String aStack, @RequestParam("bStack") String bStack, @RequestParam("threadName") String threadName) {
        String resultJsonString = sameSnippetAnalysis.getStackCompare(aStack, bStack, threadName);
        return resultJsonString;
    }
}