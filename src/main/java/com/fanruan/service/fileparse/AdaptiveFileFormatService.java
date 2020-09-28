package com.fanruan.service.fileparse;

import com.fanruan.entity.ProcessEntity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdaptiveFileFormatService {
    public static ProcessEntity build(String processData) {
        processData = processData.replaceAll("\r", "");
        int fileFormat = getFileFormat(processData);
        if (fileFormat == 0) {
            return new HotSpotFileParseService().parseProcessEntity(processData);
        } else if (fileFormat == 1) {
            return new UNNameOneFileParseService().parseProcessEntity(processData);
        }
        return new ProcessEntity();
    }

    private static int getFileFormat(String processData) {
        Pattern pattern = Pattern.compile("^.+?\nFull thread dump");
        Matcher matcher = pattern.matcher(processData);
        if (matcher.find()) {
            return 0;
        }
        pattern = Pattern.compile("^Attaching to process ID \\d+?, please wait\\.\\.\\.\nDebugger attached successfully\\.\nServer compiler detected\\.");
        matcher = pattern.matcher(processData);
        if (matcher.find()) {
            return 1;
        }
        return -1;
    }
}
