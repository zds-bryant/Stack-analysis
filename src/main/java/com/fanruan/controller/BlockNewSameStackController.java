package com.fanruan.controller;

import com.alibaba.fastjson.JSONArray;
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
 * @description: 针对block的堆栈内存分析
 * @author: Henry.Wang
 * @create: 2020/04/01 10:19
 */
@RestController
public class BlockNewSameStackController {
    @Autowired
    BlockNewSameStackAnalysis blockNewSameStackAnalysis;

    @Autowired
    Tools tools;

    @RequestMapping(value = "/blockNewSameStack", method = RequestMethod.POST)
    public String sameStack(@RequestParam("sessionId") String sessionId, @RequestParam("processName") String processName, @RequestParam("allThreadName") String threadNamesBase64Encode) throws UnsupportedEncodingException {
        String threadNamesBase64Decode = new String(Base64.getDecoder().decode(threadNamesBase64Encode), "UTF-8");
        List<String> threadNameList = JSONObject.parseArray(threadNamesBase64Decode, String.class);
        return blockNewSameStackAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName), threadNameList);
    }
}