package com.file.demo.config;

import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SftpConfig {
    @Value("${ftp.host}")
    private String ftpHost;
    @Value("${ftp.port}")
    private Integer ftpPort;
    @Value("${ftp.username}")
    private String ftpUsername;
    @Value("${ftp.password}")
    private String ftpPassword;

    @Bean
    public Sftp getSftp() {
        return JschUtil.createSftp(ftpHost, ftpPort, ftpUsername, ftpPassword);
    }
    @Bean
    public Session getSession() {
        return JschUtil.getSession(ftpHost, ftpPort, ftpUsername, ftpPassword);
    }
}
