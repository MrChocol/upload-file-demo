package com.file.demo.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.file.demo.model.Constant;
import com.file.demo.model.FileInfo;
import com.file.demo.utils.ListBuilder;
import com.file.demo.utils.MapBuilder;
import com.file.demo.utils.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/query")
public class QueryController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${file.path.download}")
    private String downloadPath;

    // 懒加载指定地址的目录，先返回其实层的所有文件/文件夹
    // 前台点击某个文件夹时，发送该文件夹的路径过来，后台去指定文件夹去获取指定文件夹路径下的所有文件/文件夹
    // 如果sourcePath为空则返回主页
    @PostMapping("/fileTree")
    public ResponseEntity<?> queryAssignPointDirectory(@RequestBody JSONObject sourcePath) {
        String assignPoint = sourcePath.getJSONObject("sourcePath").getStr("sourcePath");
        Map result = MapBuilder.start().build();
        try {
            if (StrUtil.isEmpty(assignPoint)) {
                //返回主页
                result.put("data", MapBuilder.start("fileTree", handleLsResult(downloadPath)).build());
            } else {
                assignPoint = OSUtil.normalizeSourcePath(sourcePath.getJSONObject("sourcePath").getStr("sourcePath"), OSUtil.isWindows());
                result.put("data", MapBuilder.start("fileTree", handleLsResult(assignPoint)).build());
            }
            return new ResponseEntity<>(MapBuilder.start("message", "Query successful!").put("data", result).build(), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("目录:{}不存在", assignPoint);
            return new ResponseEntity<>(MapUtil.builder("message", "The assignPoint can not be empty!").build(), HttpStatus.BAD_REQUEST);
        }
    }

    private List<FileInfo> handleLsResult(String filePath) {
        List<FileInfo> lsEntrys = ListBuilder.start().build();
        Arrays.asList(FileUtil.ls(filePath)).forEach(file -> {
            String filename = file.getName();
            FileInfo fileInfo = FileInfo.create();
            fileInfo.setSourceFileName(filename).setSize(file.length()).setCreateTime(file.lastModified()).setSourcePath(downloadPath);
            if (file.isFile()) fileInfo.setDocumentType(Constant.FileType.FILE);
            else fileInfo.setDocumentType(Constant.FileType.DIRECTORY);
            lsEntrys.add(fileInfo);
        });
        return lsEntrys;
    }
}
