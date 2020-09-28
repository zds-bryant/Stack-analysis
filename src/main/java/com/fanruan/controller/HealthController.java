package com.fanruan.controller;

import com.fanruan.analysis.HealthAnalysis;
import com.fanruan.common.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 健康报告controller
 * @author: Henry.Wang
 * @create: 2020/03/17 09:21
 */
@RestController
public class HealthController {

    @Autowired
    HealthAnalysis healthAnalysis;

    @Autowired
    Tools tools;

    @RequestMapping(value = "/health", method = RequestMethod.POST)
    public String health(@RequestParam("processName") String processName,
                         @RequestParam("sessionId") String sessionId,
                         @RequestParam("threadStateFilter") String threadStateFilter,
                         @RequestParam("threadGroupNameFilter") String threadGroupNameFilter,
                         @RequestParam("stackPatternFilter") String stackPatternFilter,
                         @RequestParam("blockFilter") String blockFilter) {
        tools.printAnalysisRequestData("health",processName, sessionId, threadStateFilter, threadGroupNameFilter, stackPatternFilter,blockFilter);
        return healthAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
    }
}
