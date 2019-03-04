package com.file.demo.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件上传
 */
@RestController
@RequestMapping(value = "/file")
public class UploadFileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${file.upload.path}")
    private String uploadPath;
    @Value("${iot-ftp.host}")
    private String ftpHost;
    @Value("${iot-ftp.port}")
    private Integer ftpPort;
    @Value("${iot-ftp.username}")
    private String ftpUsername;
    @Value("${iot-ftp.password}")
    private String ftpPassword;

    @RequestMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return new ResponseEntity<>(MapUtil.builder("message", "The file cannot be empty").build(), HttpStatus.BAD_REQUEST);
            // 获取文件名
            String fileName = file.getOriginalFilename();
            logger.info("上传的文件名：" + fileName);
            // 获取文件的后缀名
            String suffixName = fileName.substring(fileName.lastIndexOf("."));
            // 设置文件存储路径
            String path = uploadPath + fileName + suffixName;
            File dest = new File(path);
            // 检测是否存在目录
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();// 新建文件夹
            file.transferTo(dest);// 文件写入
            return new ResponseEntity<>(MapUtil.builder("message", "Upload successful").build(), HttpStatus.OK);
        } catch (IllegalStateException e) {
            logger.error("UploadFileController uploadFile:{}", e.getMessage());
        } catch (IOException e) {
            logger.error("UploadFileController uploadFile:{}", e.getMessage());
        }
        return new ResponseEntity<>(MapUtil.builder("message", "Upload failed").build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/batchUpload")
    public ResponseEntity<?> batchFileUpload(HttpServletRequest request) {
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        MultipartFile file;
        BufferedOutputStream stream;
        Map result = MapUtil.builder("message", "Upload successful").build();
        ArrayList<Map> faileds = CollectionUtil.newArrayList();
        for (int i = 0; i < files.size(); ++i) {
            file = files.get(i);
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();
                    stream = new BufferedOutputStream(new FileOutputStream(
                            new File(uploadPath + file.getOriginalFilename())));//设置文件路径及名字
                    stream.write(bytes);// 写入
                    stream.close();
                } catch (Exception e) {
                    faileds.add(MapUtil.builder("文件" + file.getOriginalFilename() + "上传失败", "原因: " + e.getMessage()).build());
                    logger.error("第:{}个文件上传失败:{}", i, e.getMessage());
                }
            } else {
                faileds.add(MapUtil.builder("文件" + file.getOriginalFilename() + "上传失败", "原因: 文件为空").build());
                logger.error("第:{}个文件因为文件为空导致上传失败", i);
            }
        }
        result.put("data", faileds);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping("/uploadFtp")
    public ResponseEntity<?> uploadFileToFtp(@RequestParam("file") MultipartFile file) {
        Sftp sftp = JschUtil.createSftp(ftpHost, ftpPort, ftpUsername, ftpPassword);
        File target = new File(uploadPath + System.lineSeparator() + file.getOriginalFilename());
        try {
            file.transferTo(target);
            sftp.upload(file.getOriginalFilename(), target);
        } catch (IOException e) {
            logger.error("文件转换失败:{}", e.getMessage());
        } finally {
            target.delete();
        }
        return new ResponseEntity<>(MapUtil.builder("message", "Upload failed").build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
