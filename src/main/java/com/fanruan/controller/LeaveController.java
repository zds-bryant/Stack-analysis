package com.fanruan.controller;

import com.fanruan.analysis.BlockNewAnalysis;
import com.fanruan.analysis.HealthAnalysis;
import com.fanruan.analysis.SameSnippetAnalysis;
import com.fanruan.analysis.SummaryAnalysis;
import com.fanruan.common.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 页面关闭触发事件
 * @author: Henry.Wang
 * @create: 2020/04/02 10:31
 */
@RestController
public class LeaveController {
    @Autowired
    SummaryAnalysis summaryAnalysis;
    @Autowired
    BlockNewAnalysis blockNewAnalysis;
    @Autowired
    SameSnippetAnalysis sameSnippetAnalysis;
    @Autowired
    HealthAnalysis healthAnalysis;
    @Autowired
    Tools tools;


    @RequestMapping(value = "/leave", method = RequestMethod.POST)
    public String block(@RequestParam("sessionId") String sessionId) {
        tools.getLogger().info(sessionId + " leave");
        removeDataFromMap(sessionId);
        return null;
    }

    public void removeDataFromMap(String sessionId) {
        blockNewAnalysis.removeSessionIdInMap(sessionId);
        sameSnippetAnalysis.removeSessionIdInMap(sessionId);
        healthAnalysis.removeSessionIdInMap(sessionId);
    }
}
