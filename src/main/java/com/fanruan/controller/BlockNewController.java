package com.fanruan.controller;

import com.fanruan.analysis.BlockNewAnalysis;
import com.fanruan.common.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;


@RestController
public class BlockNewController {
    @Autowired
    BlockNewAnalysis blockNewAnalysis;

    @Autowired
    Tools tools;

    @RequestMapping(value = "/blockNew", method = RequestMethod.POST)
    public String block(@RequestParam("processName") String processName,
                        @RequestParam("sessionId") String sessionId,
                        @RequestParam("threadStateFilter") String threadStateFilter,
                        @RequestParam("threadGroupNameFilter") String threadGroupNameFilter,
                        @RequestParam("stackPatternFilter") String stackPatternFilter,
                        @RequestParam("blockAllThreadFilter") String blockAllThreadFilterBase64Encode,
                        @RequestParam("blockFilter") String blockFilter) throws UnsupportedEncodingException {
        tools.printAnalysisRequestData("block", processName, sessionId, threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockFilter);
        String resultJson = blockNewAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
        String filterResultJson = blockNewAnalysis.getFilterResultJson(tools.getSessionProcessName(sessionId, processName),
                resultJson, threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilterBase64Encode);
        return filterResultJson;
    }
}