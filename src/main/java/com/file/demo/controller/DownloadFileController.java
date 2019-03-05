package com.file.demo.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 文件下载
 */
@RestController
@RequestMapping(value = "file")
public class DownloadFileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${file.download.path}")
    private String downloadPath;
    @Autowired
    private Sftp sftp;

    /**
     * 本地下载
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<?> downloadFile(@PathVariable("fileName") final String fileName, HttpServletResponse response) {
        if (fileName != null) {
            //设置文件路径
            String filePath = downloadPath + File.separator + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                response.setContentType("application/force-download");// 设置强制下载不打开
                response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                    logger.info("文件:{}下载成功", fileName);
                } catch (Exception e) {
                    logger.error("DownloadFileController downloadFile:{}", e.getMessage());
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            logger.error("DownloadFileController downloadFile:{}", e.getMessage());
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            logger.error("DownloadFileController downloadFile:{}", e.getMessage());
                        }
                    }
                }
            } else
                return new ResponseEntity<>(MapUtil.builder("message", "The file you need does not exist").build(), HttpStatus.BAD_REQUEST);
        } else
            return new ResponseEntity<>(MapUtil.builder("message", "The filename cannot be empty").build(), HttpStatus.BAD_REQUEST);
        return null;
    }

    /**
     * 通过服务器下载
     *
     * @param fileName 指定文件名
     */
    @GetMapping("/downloadFtp/{fileName}")
    public ResponseEntity<?> downloadFileFromFtp(@PathVariable("fileName") final String fileName, HttpServletResponse response) {
        //判断fileName是否存在
        //存在则下载下来
        if (fileName != null && sftp.cd(downloadPath)) {
            if (sftp.ls(downloadPath).contains(fileName)) {
                InputStream inputStream = null;
                try {
                    inputStream = sftp.getClient().get(fileName);
                } catch (SftpException e) {
                    logger.error("文件:{}获取失败:{}", fileName, e.getMessage());
                }
                response.setContentType("application/force-download");// 设置强制下载不打开
                response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(inputStream);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                    logger.info("文件:{} 下载成功", fileName);
                } catch (Exception e) {
                    logger.error("DownloadFileController downloadFile:{}", e.getMessage());
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            logger.error("DownloadFileController downloadFile:{}", e.getMessage());
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            logger.error("DownloadFileController downloadFile:{}", e.getMessage());
                        }
                    }
                }
            } else
                return new ResponseEntity<>(MapUtil.builder("message", "The file you need does not exist").build(), HttpStatus.BAD_REQUEST);
        } else
            return new ResponseEntity<>(MapUtil.builder("message", "The file you need does not exist").build(), HttpStatus.BAD_REQUEST);
        return null;
    }
}
