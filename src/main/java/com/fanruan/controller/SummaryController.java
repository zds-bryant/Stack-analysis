package com.fanruan.controller;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.SummaryAnalysis;
import com.fanruan.common.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.UnmarshallerHandler;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;

/**
 * @author Henry.Wang
 * @description 概括分析controller
 * @date 2020/3/8
 */
@RestController
//@RestControllerAdvice
public class SummaryController {
    @Autowired
    SummaryAnalysis summaryAnalysis;

    @Autowired
    Tools tools;

    @RequestMapping(value = "/summary", method = RequestMethod.POST)
    public String summary(@RequestParam("processName") String processName,
                          @RequestParam("sessionId") String sessionId,
                          @RequestParam("threadStateFilter") String threadStateFilter,
                          @RequestParam("threadGroupNameFilter") String threadGroupNameFilter,
                          @RequestParam("stackPatternFilter") String stackPatternFilter,
                          @RequestParam("blockAllThreadFilter") String blockAllThreadFilterBase64Encode,
                          @RequestParam("blockFilter") String blockFilter) throws UnsupportedEncodingException {
        tools.printAnalysisRequestData("summary",processName, sessionId, threadStateFilter, threadGroupNameFilter, stackPatternFilter,blockFilter);
        String blockAllThreadFilterBase64Decode = new String(Base64.getDecoder().decode(blockAllThreadFilterBase64Encode), "UTF-8");
        List<String> blockAllThreadFilter = JSONObject.parseArray(blockAllThreadFilterBase64Decode, String.class);
        return summaryAnalysis.peculiarFilterResultJsonExecutor(tools.getSessionProcessName(sessionId, processName),
                threadStateFilter, threadGroupNameFilter, stackPatternFilter, blockAllThreadFilter);
    }

}
