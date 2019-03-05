package com.file.demo.config;

import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SftpConfig {
    @Value("${iot-ftp.host}")
    private String ftpHost;
    @Value("${iot-ftp.port}")
    private Integer ftpPort;
    @Value("${iot-ftp.username}")
    private String ftpUsername;
    @Value("${iot-ftp.password}")
    private String ftpPassword;

    @Bean
    public Sftp getSftp() {
        return JschUtil.createSftp(ftpHost, ftpPort, ftpUsername, ftpPassword);
    }
}
