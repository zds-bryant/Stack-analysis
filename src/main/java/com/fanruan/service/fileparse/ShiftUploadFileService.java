package com.fanruan.service.fileparse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanruan.common.Tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Henry.Wang
 * @description 把从前端传过来的文件转化为字符串
 * @date 2020/3/6
 */
@Component
public class ShiftUploadFileService {
    @Autowired
    Tools tools;

    public Map<String, String> files = new HashMap<String, String>();

    public JSONObject run(MultipartFile file, String sessionId) {
        try {
            String fileName = file.getOriginalFilename();
            String sessionFileName = tools.getSessionProcessName(sessionId,fileName);
            byte[] fileBytes = file.getBytes();
            String fileData = new String(fileBytes);
            if (isTextFile(fileData)) {
                if (files.keySet().contains(sessionFileName)) {
                    return buildResultJson(new String[]{tools.getProcessNameFromSessionProcessName(sessionFileName)}, "up load file success");
                }
                return handTextFile(sessionFileName, fileData, sessionId);
            } else {
                return handZipFile(sessionFileName, fileData, sessionId);
            }
        } catch (Exception e) {
            return buildResultJson(new String[0], "up load file fail");
        }
    }

    private JSONObject handTextFile(String sessionFileName, String fileData, String sessionId) {
        if (isLegalFileName(sessionFileName)) {
            files.put(sessionFileName, fileData);
            return buildResultJson(new String[]{tools.getProcessNameFromSessionProcessName(sessionFileName)}, "up load file success");
        } else {
            return buildResultJson(new String[0], "uped load file ");
        }
    }

    private JSONObject handZipFile(String sessionFileName, String fileData, String sessionId) {
        return null;
    }

    private boolean isTextFile(String fileData) {
        return true;
    }

    private JSONObject buildResultJson(String[] fileNames, String info) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (String fileName : fileNames) {
            jsonArray.add(fileName);
        }
        jsonObject.put("filenames", jsonArray);
        jsonObject.put("info", info);
        jsonObject.put("endfilename", fileNames[fileNames.length - 1]);
        return jsonObject;
    }

    private boolean isLegalFileName(String fileName) {
        return !(files.keySet().contains(fileName));
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }
}
