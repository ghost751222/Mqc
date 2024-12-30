package com.mqc.config;


import com.macaron.mqcsdk.QcClerk;
import com.mqc.service.RtpReceiveListenerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class QcConfig {


    @Value("${device.mac}")
    private String deviceMac;


    @Value("${device.stream.size:320}")
    private int streamSize;

    @Value("${device.stream.silence.seconds:1}")
    private long silentSeconds;

    @Value("${device.stream.silence.threshold:35}")
    private long silenceThreshold;

    @Value("${com.mqc.debug.sip:false}")
    private boolean debugSip;

    @Value("${com.mqc.debug.rtp:false}")
    private boolean debugRtp;

    @Value("${com.mqc.debug.party.info:false}")
    private boolean debugPartyInfo;

    @Autowired
    private RtpReceiveListenerImpl rtpReceiveListener;

    @Bean
    public QcClerk qcClerk() {
        QcClerk qcClerk = new QcClerk(deviceMac, silenceThreshold, streamSize);
        qcClerk.setToDebugSIP(debugSip);
        qcClerk.setToDebugPartyInfo(debugPartyInfo);
        qcClerk.setToDebugRTP(debugRtp);
        qcClerk.setRtpReceiveListener(rtpReceiveListener);
        qcClerk.setSilenceSeconds(silentSeconds);
        return qcClerk;
    }

    @Bean(name = "executor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(30);
        taskExecutor.setMaxPoolSize(80);

        return taskExecutor;
    }

    @Bean(name = "executorService")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(50);
    }
}
