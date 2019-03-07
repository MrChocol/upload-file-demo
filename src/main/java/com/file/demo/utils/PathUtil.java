package com.file.demo.utils;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

public class PathUtil {
    public static String normalizeSourcePath(String path, boolean isWindows) {
        StringBuilder sb = new StringBuilder(path);
        if (!path.endsWith("[/|\\]")) {
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
        return StrUtil.replace(fileName, "[/|\\]", "");
    }

    public static void main(String[] args) {
        String string = "/opt/upload/file/windows/V2";
        System.out.println(normalizeSourcePath(string,false));
    }
}
