package com.file.demo.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.json.JSONObject;
import com.file.demo.model.Constant;
import com.file.demo.utils.OSUtil;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;


@RestController
@RequestMapping(value = "/file")
public class UploadFileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${file.path.nfs}")
    private String nfsPath;
    @Value("${file.path.download}")
    private String downloadPath;

    @Autowired
    private Sftp sftp;

    /**
     * 上传到本地
     */
    @PostMapping("/upload")
    public ResponseEntity<?> targetFile(String filePath, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return new ResponseEntity<>(MapUtil.builder("message", "The file cannot be empty").build(), HttpStatus.BAD_REQUEST);
            //获取下载路径
            String sourcePath = OSUtil.normalizeSourcePath(filePath, OSUtil.isWindows());
            // 获取文件名
            String fileName = file.getOriginalFilename();
            logger.info("上传路径:{},上传文件名：{}", sourcePath, fileName);
            // 获取文件的后缀名
            String suffixName = fileName.substring(fileName.lastIndexOf("."));
            // 设置文件存储路径
            String path = sourcePath + fileName;
            File dest = new File(path);
            // 检测是否存在目录
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();// 新建文件夹
            file.transferTo(dest);// 文件写入
            if (StrUtil.endWith(suffixName, OSUtil.FILE_COMPRESS_SUFFIX)) {
                // 解压缩
                try {
                    ZipUtil.unzip(dest, new File(sourcePath));
                } catch (IllegalArgumentException e) {
                    logger.info("压缩编码出错，切换为GBK");
                    ZipUtil.unzip(dest, new File(sourcePath), CharsetUtil.CHARSET_GBK);
                }
                dest.delete();
            }
            return new ResponseEntity<>(MapUtil.builder("message", "Upload successful").build(), HttpStatus.OK);
        } catch (IllegalStateException e) {
            logger.error("UploadFileController targetFile:{}", e.getMessage());
        } catch (IOException e) {
            logger.error("UploadFileController targetFile:{}", e.getMessage());
        }
        return new ResponseEntity<>(MapUtil.builder("message", "Upload failed").build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 上传到服务器
     */
    @RequestMapping("/uploadFtp")
    public ResponseEntity<?> targetFileToFtp(@RequestParam("file") MultipartFile file) {
        try {
            sftp.cd(nfsPath);
        } catch (Exception e) {
            logger.info("该文件夹不存在，自动创建");
            sftp.mkdir(nfsPath);
        }
        try {
            sftp.getClient().put(file.getInputStream(), nfsPath + file.getOriginalFilename());
        } catch (SftpException e) {
            logger.error("文件上传失败:{}", e.getMessage());
        } catch (IOException e) {
            logger.error("文件上传失败:{}", e.getMessage());
        }
        return new ResponseEntity<>(MapUtil.builder("message", "Upload successful").build(), HttpStatus.OK);
    }

    /**
     * 多文件上传到本地
     */
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
                            new File(nfsPath + file.getOriginalFilename())));//设置文件路径及名字
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
}
