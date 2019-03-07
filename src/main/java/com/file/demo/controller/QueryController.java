package com.file.demo.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.json.JSONObject;
import com.file.demo.model.Constant;
import com.file.demo.model.FileInfo;
import com.file.demo.utils.ListBuilder;
import com.file.demo.utils.MapBuilder;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Vector;

@RestController
@RequestMapping(value = "/query")
public class QueryController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${file.path.target}")
    private String targetPath;
    @Autowired
    private Sftp sftp;

    // 懒加载指定地址的目录，先返回其实层的所有文件/文件夹
    // 前台点击某个文件夹时，发送该文件夹的路径过来，后台去指定文件夹去获取指定文件夹路径下的所有文件/文件夹
    @PostMapping("/fileTree")
    public ResponseEntity<?> queryAssignPointDirectory(@RequestBody JSONObject sourcePath) {
        String assignPoint = sourcePath.getJSONObject("sourcePath").getStr("sourcePath");
        if (StrUtil.isNotEmpty(assignPoint)) {
            Map result = MapBuilder.start().build();
            try {
                result.put("data", MapBuilder.start("fileTree", handleFtpLsResult(sftp.getClient().ls(assignPoint), assignPoint)).build());
                return new ResponseEntity<>(MapBuilder.start("message", "Query successful!").put("data", result).build(), HttpStatus.OK);
            } catch (Exception e) {
                logger.error("目录:{}不存在", assignPoint);
                return new ResponseEntity<>(MapUtil.builder("message", "The assignPoint can not be empty!").build(), HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(MapUtil.builder("message", "The assignPoint can not be empty!").build(), HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/masterDirectory")
    public ResponseEntity<?> queryMasterDirectory() {
        Map result = MapBuilder.start().build();
        try {
            result.put("data", MapBuilder.start("fileTree", handleFtpLsResult(sftp.getClient().ls(targetPath), targetPath)).build());
            return new ResponseEntity<>(MapBuilder.start("message", "Query successful!").put("data", result).build(), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("目录:{}不存在", targetPath);
            return new ResponseEntity<>(MapUtil.builder("message", "The assignPoint can not be empty!").build(), HttpStatus.BAD_REQUEST);
        }
    }

    private List<FileInfo> handleFtpLsResult(Vector<ChannelSftp.LsEntry> lsInfo, String filePath) {
        List<FileInfo> lsEntrys = ListBuilder.start().build();
        lsInfo.forEach(entry -> {
            String filename = entry.getFilename();
            if (!("..".equals(filename) || ".".equals(filename))) {
                SftpATTRS attrs = entry.getAttrs();
                FileInfo fileInfo = FileInfo.create();
                fileInfo.setSourceFileName(filename).setSize(attrs.getSize()).setCreateTime(attrs.getMTime()).setSourcePath(filePath);
                if (entry.getLongname().startsWith("-")) fileInfo.setDocumentType(Constant.File);
                else fileInfo.setDocumentType(Constant.Directory);
                lsEntrys.add(fileInfo);
            }
        });
        return lsEntrys;
    }
}
