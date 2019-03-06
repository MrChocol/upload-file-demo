package com.file.demo.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.Session;
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
@RequestMapping(value = "/file")
public class DownloadFileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${file.path.upload}")
    private String uploadPath;
    @Value("${file.path.download}")
    private String downloadPath;
    private String packagePath;
    private String packTemp;   //zip -r ../package/xxxx.zip test1

    @Value("${file.path.package}")
    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
        packTemp = "zip -r " + packagePath + "{} " + uploadPath + "{}";
    }

    @Autowired
    private Sftp sftp;
    @Autowired
    private Session session;

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
        if (isExists(uploadPath, fileName)) {
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
        return null;
    }

    //接收下载请求，下载指定的文件夹/文件.如果是文件就直接下载下来，如果是文件夹发送指令过去进行打包在拉
    @GetMapping("/downloadFromFtp/{fileName}")
    public ResponseEntity<?> downloadFileFromFtp(@PathVariable("fileName") final String fileName) {
        //先判断目标服务器指定文件夹是否存在，y下一步，n返回结果
        if (isExists(uploadPath, fileName)) {
            //判断目标文件是否是文件夹，n返回文件，y下一步
            String temporaryPackageName = null;
            if (isDirectory(fileName)) {
                //判断指定文件夹是否为空，y返回结果，n下一步
                if (sftp.ls(fileName).size() == 0)
                    return new ResponseEntity<>(MapUtil.builder("message", "The file is directory and this directory is null!").build(), HttpStatus.OK);
                else {
                    if (!isExistsForPath(packagePath)) sftp.mkdir(packagePath);
                    temporaryPackageName = fileName + "-" + DateUtil.format(DateUtil.date(), DatePattern.PURE_DATETIME_FORMAT) + ".zip";
                    // 发送exec指令进行打包，统一后缀，然后下载过来
                    String exec = JschUtil.exec(session, StrUtil.format(packTemp, temporaryPackageName, fileName), null);
                    logger.info("执行结果:{}", exec);
                }
            }
            try {
                if (!FileUtil.exist(downloadPath)) FileUtil.newFile(downloadPath);
                if (temporaryPackageName != null) {
                    File localFile = FileUtil.writeFromStream(sftp.getClient().get(packagePath + temporaryPackageName), downloadPath + temporaryPackageName);
                    // 删除远端的压缩包
                    sftp.delFile(packagePath + temporaryPackageName);
                    // 解压本地文件
                    ZipUtil.unzip(localFile);
                    localFile.delete();
                } else
                    FileUtil.writeFromStream(sftp.getClient().get(fileName), downloadPath + fileName);
                return new ResponseEntity<>(MapUtil.builder("message", "Download successful").build(), HttpStatus.OK);
            } catch (SftpException e) {
                logger.error("文件:{}获取失败:{}", fileName, e.getMessage());
            }
        }
        return new ResponseEntity<>(MapUtil.builder("message", "The file you need does not exist").build(), HttpStatus.BAD_REQUEST);
    }

    //判断文件夹或者文件是否存在
    private boolean isExists(String path, String fileName) {
        if (isExistsForPath(path)) {
            if (StrUtil.isNotEmpty(fileName) && sftp.ls(uploadPath).contains(fileName))
                return true;
        }
        return false;
    }

    private boolean isExistsForPath(String path) {
        try {
            if (sftp.cd(path)) return true;
        } catch (Exception e) {
            logger.info("该文件夹不存在，自动创建");
            sftp.mkdir(uploadPath);   //没有则创建
        }
        return false;
    }

    //判断是否为文件夹
    private boolean isDirectory(String fileName) {
        if (sftp.lsDirs(uploadPath).contains(fileName)) return true;
        else
            return false;
    }
}
