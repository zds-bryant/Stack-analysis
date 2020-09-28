package com.fanruan.controller;

import com.alibaba.fastjson.JSONObject;
import com.fanruan.analysis.BlockNewAnalysis;
import com.fanruan.analysis.HealthAnalysis;
import com.fanruan.analysis.SameSnippetAnalysis;
import com.fanruan.analysis.SummaryAnalysis;
import com.fanruan.common.Tools;
import com.fanruan.entity.BatchProcessEntity;
import com.fanruan.service.fileparse.ShiftUploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Henry.Wang
 * @description 接受上传的文件
 * @date 2020/3/5
 */
@RestController
public class UploadFileController {
    @Autowired
    ShiftUploadFileService shiftUploadFile;
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
    @Autowired
    BatchProcessEntity batchProcessEntity;

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public String upLoadFile(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) {

        JSONObject resultJson = new JSONObject();
        JSONObject fileList = shiftUploadFile.run(file, sessionId);
        String processName = (String) fileList.get("endfilename");
        tools.getLogger().info(sessionId + "_" + processName + " enter");
        //提前加载分析模块
        advanceLoadAnalysis(sessionId, processName);
        JSONObject processEntityJsonObject = batchProcessEntity.processEntityJsonObjectBuild(tools.getSessionProcessName(sessionId, processName));
        resultJson.put("fileList", fileList);
        resultJson.put("processEntityJsonObject", processEntityJsonObject);
        return resultJson.toJSONString();
    }

    private void advanceLoadAnalysis(String sessionId, String processName) {
        new Thread() {
            @Override
            public void run() {
                blockNewAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
                sameSnippetAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
                healthAnalysis.getResultJson(tools.getSessionProcessName(sessionId, processName));
            }
        }.start();
    }

    @RequestMapping(value = "/allStackData", method = RequestMethod.POST)
    public String allStackData(@RequestParam("processName") String processName, @RequestParam("sessionId") String sessionId) {
        JSONObject processEntityJsonObject = batchProcessEntity.processEntityJsonObjectBuild(tools.getSessionProcessName(sessionId, processName));
        return processEntityJsonObject.toJSONString();
    }
}
