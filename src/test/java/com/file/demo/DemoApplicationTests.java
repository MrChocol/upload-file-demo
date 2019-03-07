package com.file.demo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.http.HttpUtil;
import com.jcraft.jsch.ChannelSftp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoApplicationTests {

    @Test
    public void contextLoads() {
        Sftp administrator = JschUtil.createSftp("145.170.23.203", 22, "administrator", "P@ssw0rd");
        ChannelSftp client = administrator.getClient();
    }

    @Test
    public void resursionDir() {
        List<Object> result = CollUtil.newArrayList();
        showDirectory(new File("C:\\Users\\cllwy\\Desktop\\upload"), result);
        System.out.println(result);
    }

    private static int level = 0;

    private static void showDirectory(File f, List result) {
        if (f.isDirectory()) {
            for (int i = 0; i < level; i++) {
                System.out.print("\t");
            }
            result.add(f.getName());
            System.out.println("D-" + f.getName());
            level++;
            for (File temp : f.listFiles()) {
                showDirectory(temp, CollUtil.newArrayList());
            }
            level--;
        } else {
            for (int i = 0; i < level; i++) {
                System.out.print("\t");
            }
            result.add(f.getName());
            System.out.println("F-" + f.getName());
        }
    }

    public static void main(String[] args) {
        String proxyDirectory = "C:\\chenli\\IDEA\\Idea_WorkSpace\\filedemo\\download\\file/";
        String s1 = ReUtil.replaceAll(proxyDirectory, "\\\\", "/");
        System.out.println(proxyDirectory);
        System.out.println(s1);
    }

}
