package com.file.demo.utils;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OSUtil {

    public static String FILE_COMPRESS_SUFFIX;

    @Value("${file.compress.suffix}")
    public void setFileCompressSuffix(String fileCompressSuffix) {
        FILE_COMPRESS_SUFFIX = fileCompressSuffix;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;
    }

    public static String normalizeSourcePath(String path, boolean isWindows) {
        StringBuilder sb = new StringBuilder(path);
        if (!(path.endsWith("/") || path.endsWith("\\"))) {
            if (isWindows == false) {
                sb.append("/");
                sb = StrUtil.builder(ReUtil.replaceAll(sb.toString(), "\\\\", "/"));
            } else {
                sb.append("\\");
                sb = StrUtil.builder(ReUtil.replaceAll(sb.toString(), "/", "\\"));
            }
        }
        return sb.toString();
    }

    public static String normalizeFileName(String fileName) {
        return StrUtil.replace(fileName, "/", "").replaceAll("\\\\", "");
    }
}
