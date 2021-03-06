package com.file.demo.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.json.JSONObject;
import com.file.demo.config.SftpConfig;
import com.file.demo.model.Constant;
import com.file.demo.utils.MapBuilder;
import com.file.demo.utils.OSUtil;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 文件下载
 */
@RestController
@RequestMapping(value = "/back")
public class BackupsFileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${file.path.backups}")
    private String backupsPath;
    @Value("${file.path.download}")
    private String downloadPath;
    private String packagePath;
    private String packTemp;   //zip -r ../package/xxxx.zip test1
    private String unpackTemp = "unzip {}";   //unzip ../package/xxxx.zip
    private String netuseTemp = "net use {} \\\\\\{}\\\\{} {} /user:{}";   // net use Z: \\145.170.29.52\chenli xzli123 /user:xzli
    @Autowired
    private Sftp sftp;
    @Autowired
    private Session session;

    @Value("${file.path.package}")
    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
        packTemp = "zip -rj " + packagePath + "{} " + "{}";
    }

    /**
     * 将当前服务器指定的资源上传到指定搞得服务器的文件夹下
     */
    @PostMapping("/backToServer")
    public ResponseEntity<?> backToServer(@RequestBody JSONObject requestJson) {
        JSONObject sourcePath = requestJson.getJSONObject("sourcePath");
        String filePath = OSUtil.normalizeSourcePath(sourcePath.getStr("sourcePath"), OSUtil.isWindows());
        String fileName = OSUtil.normalizeFileName(sourcePath.getStr("fileName"));
        String serverAddress = sourcePath.getStr("address");    //指定服务器
//        sftp = SftpConfig.createSftp()
        //如果是linux服务器则通过ftp进行操作
        //存放在目标服务器的文件夹路径=filePath-downloadPath+backupsPath
        if (Constant.OSType.LINUX.contains(sourcePath.getStr("osType"))) {
            if (!backupsPath.endsWith("/")) backupsPath = backupsPath + "/";
            String targetPath = OSUtil.normalizeSourcePath(backupsPath + StrUtil.subAfter(filePath, downloadPath, false), false);
            // todo 通过serverAddress去连接linux服务器
            //判断目标文件是文件夹还是文件
            File targetFile = new File(filePath + fileName);
            try {
                FileInputStream fileInputStream;
                File zip = null;
                if (targetFile.isFile()) {
                    //文件--》直接传输
                    fileInputStream = new FileInputStream(targetFile);
                    sftp.getClient().put(fileInputStream, targetPath + fileName);
                } else {
                    //文件夹--》压缩
                    zip = ZipUtil.zip(targetFile, CharsetUtil.CHARSET_UTF_8);
                    //进入目标目录
                    try {
                        sftp.cd(targetPath);
                    } catch (Exception e) {
                        logger.debug("该文件夹不存在，自动创建:{}", targetPath);
                        sftp.mkdir(targetPath);
                    }
                    fileInputStream = new FileInputStream(zip);
                    sftp.getClient().put(fileInputStream, targetPath + zip.getName());
                    String exec = JschUtil.exec(session, StrUtil.format(unpackTemp, targetPath + zip.getName()), null);
                    logger.debug("执行结果:{}", exec);
                    sftp.delFile(targetPath + zip.getName());
                }
                fileInputStream.close();
                if (zip != null) zip.delete();
            } catch (Exception e) {
                logger.error("BackupsFileController backToServer error:{}", e.getMessage());
            }
        } else {
            //如果是windows服务器则通过net use
            try {
                Runtime.getRuntime().exec(StrUtil.format(netuseTemp, "Z:", "145.170.29.52", "chenli", "xzli123", "xzli"));
//                Runtime.getRuntime().exec("net use Z: \\\\145.170.29.52\\chenli xzli123 /user:xzli");
                String targetPath = OSUtil.normalizeSourcePath(backupsPath + StrUtil.subAfter(filePath, downloadPath, false), true);
                File targetFile = new File(filePath + fileName);
                FileUtil.copy(targetFile, new File(targetPath), true);
            } catch (IOException e) {
                logger.error("BackupsFileController backToServer error:{}", e.getMessage());
            }
        }
        return new ResponseEntity<>(MapBuilder.start("message", "Backups successful").build(), HttpStatus.OK);
    }

    /**
     * 本地下载
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<?> proxyFile(@PathVariable("fileName") final String fileName, HttpServletResponse response) {
        if (fileName != null) {
            //设置文件路径
            String filePath = downloadPath + File.separator + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                response.setContentType("application/force-proxy");// 设置强制下载不打开
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
                    logger.error("DownloadFileController proxyFile:{}", e.getMessage());
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            logger.error("DownloadFileController proxyFile:{}", e.getMessage());
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            logger.error("DownloadFileController proxyFile:{}", e.getMessage());
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
    public ResponseEntity<?> proxyFileFromFtp(@PathVariable("fileName") final String fileName, HttpServletResponse response) {
        //判断fileName是否存在
        //存在则下载下来
        if (isExists(backupsPath, fileName)) {
            InputStream inputStream = null;
            try {
                inputStream = sftp.getClient().get(backupsPath + fileName);
            } catch (SftpException e) {
                logger.error("文件:{}获取失败:{}", fileName, e.getMessage());
            }
            response.setContentType("application/force-proxy");// 设置强制下载不打开
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
                logger.error("DownloadFileController proxyFile:{}", e.getMessage());
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        logger.error("DownloadFileController proxyFile:{}", e.getMessage());
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        logger.error("DownloadFileController proxyFile:{}", e.getMessage());
                    }
                }
            }
        } else
            return new ResponseEntity<>(MapUtil.builder("message", "The file you need does not exist").build(), HttpStatus.BAD_REQUEST);
        return null;
    }

    //接收下载请求，下载指定的文件夹/文件.如果是文件就直接下载下来，如果是文件夹发送指令过去进行打包在拉
    @PostMapping("/downloadFromFtp")
    public ResponseEntity<?> proxyFileFromFtp(@RequestBody JSONObject requestJson) {
        JSONObject sourcePath = requestJson.getJSONObject("sourcePath");
        String filePath = OSUtil.normalizeSourcePath(sourcePath.getStr("sourcePath"), OSUtil.isWindows());
        String fileName = OSUtil.normalizeFileName(sourcePath.getStr("fileName"));
        String serverAddress = sourcePath.getStr("serverIp");    //指定服务器
        //先判断目标服务器指定文件夹是否存在，y下一步，n返回结果
        if (isExists(filePath, fileName)) {
            //判断目标文件是否是文件夹，n返回文件，y下一步
            String temporaryPackageName = null;
            if (sftp.lsDirs(filePath).contains(fileName)) {
                //判断指定文件夹是否为空，y返回结果，n下一步
                if (sftp.ls(fileName).size() == 0)
                    return new ResponseEntity<>(MapUtil.builder("message", "The file is directory and this directory is null!").build(), HttpStatus.OK);
                else {
                    if (!isExistsForPath(packagePath)) sftp.mkdir(packagePath);
                    temporaryPackageName = fileName + ".zip";
                    String exec = JschUtil.exec(session, StrUtil.format(packTemp, temporaryPackageName, filePath + fileName), null);
                    logger.info("执行结果:{}", exec);
                }
            }
            try {
                if (!FileUtil.exist(downloadPath)) FileUtil.newFile(downloadPath);
                //把压缩包下到指定的文件夹，若指定文件夹不存在则创建
                String proxyDirectory = downloadPath + StrUtil.subAfter(filePath, backupsPath, false);
                if (OSUtil.isWindows())
                    proxyDirectory = ReUtil.replaceAll(proxyDirectory, "/", "\\");
                else proxyDirectory = ReUtil.replaceAll(proxyDirectory, "\\\\", "/");
                if (!isExistsForPath(proxyDirectory)) new File(proxyDirectory);
                if (temporaryPackageName != null) {
                    File localFile = FileUtil.writeFromStream(sftp.getClient().get(packagePath + temporaryPackageName), proxyDirectory + temporaryPackageName);
                    // 删除远端的压缩包
                    sftp.delFile(packagePath + temporaryPackageName);
                    // 解压本地文件
                    ZipUtil.unzip(localFile);
                    localFile.delete();
                } else
                    FileUtil.writeFromStream(sftp.getClient().get(fileName), proxyDirectory + fileName);
                return new ResponseEntity<>(MapBuilder.start("message", "Download successful").build(), HttpStatus.OK);
            } catch (SftpException e) {
                logger.error("文件:{}获取失败:{}", fileName, e.getMessage());
            }
        }
        return new ResponseEntity<>(MapBuilder.start("message", "The file you need does not exist").build(), HttpStatus.BAD_REQUEST);
    }

    //判断文件夹或者文件是否存在
    private boolean isExists(String path, String fileName) {
        if (isExistsForPath(path))
            return StrUtil.isNotEmpty(fileName) && sftp.ls(path).contains(fileName);
        else
            return false;
    }

    private boolean isExistsForPath(String path) {
        try {
            return sftp.cd(path);
        } catch (Exception e) {
            logger.info("该文件夹不存在：{}", path);
        }
        return false;
    }

}
