package com.fanruan.common;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @description: 拦截异常
 * @author: Henry.Wang
 * @create: 2020/04/19 17:58
 */
@RestControllerAdvice
class InterfaceExceptionHandler {

    @Autowired
    Tools tools;

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String exception(Exception e) {
        String message = e.toString();
        String stackTrace = getStackTraceInfo(e.getStackTrace());
        tools.getLogger().error(stackTrace);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message",message);
        jsonObject.put("stackTrace",stackTrace);
        return jsonObject.toJSONString();
    }

    private String getStackTraceInfo(StackTraceElement[] stackTraceElements) {
        String stackTraceInfo = "";
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTraceInfo = stackTraceInfo + stackTraceElement.toString() + "\n";
        }

        return stackTraceInfo;
    }
}
