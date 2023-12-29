package com.youzu.dc.simplefileserver.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StartupApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 服务可用后打印访问地址
        WebServer webServer = webServerAppCtxt.getWebServer();
        int port = webServer.getPort();
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "Unknown";
        }
        log.info("Application is running at http://" + ip + ":" + port);
    }
}