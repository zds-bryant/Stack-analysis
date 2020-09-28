package com.fanruan.controller;

import com.fanruan.analysis.ThreadListAnalysis;
import com.fanruan.common.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;

/**
 * @description: 线程明细
 * @author: Henry.Wang
 * @create: 2020/04/08 20:06
 */
@RestController
public class ThreadListController {
    @Autowired
    ThreadListAnalysis threadListAnalysis;

    @Autowired
    Tools tools;
    @RequestMapping(value = "/threadList", method = RequestMethod.POST)
    public String threadList(@RequestParam("processName") String processName,
                        @RequestParam("sessionId") String sessionId,
                        @RequestParam("threadStateFilter") String threadStateFilter,
                        @RequestParam("threadGroupNameFilter") String threadGroupNameFilter,
                        @RequestParam("stackPatternFilter") String stackPatternFilter,
                        @RequestParam("blockAllThreadFilter") String blockAllThreadFilterBase64Encode,
                             @RequestParam("blockFilter") String blockFilter) throws UnsupportedEncodingException {
        tools.printAnalysisRequestData("threadList",processName, sessionId, threadStateFilter, threadGroupNameFilter, stackPatternFilter,blockFilter);
        String resultJson = threadListAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
        String filterResultJson = threadListAnalysis.getFilterResultJson(tools.getSessionProcessName(sessionId, processName),
                resultJson,threadStateFilter,threadGroupNameFilter,stackPatternFilter,blockAllThreadFilterBase64Encode);
        return filterResultJson;
    }
}