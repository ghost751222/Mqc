package com.mqc.componet;

import com.macaron.mqcsdk.QcClerk;
import com.mqc.service.RtpReceiveListenerImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Slf4j
public class QcComponent {



    @Autowired
    @Getter
    private QcClerk qcClerk;

    @Autowired
    @Qualifier("executor")
    TaskExecutor taskExecutor;


    private boolean serverStatus = false;



    @PostConstruct
    public void startQcClerk() {
        taskExecutor.execute(() -> {
            try {
                serverStatus = true;
                qcClerk.Start();
            } catch (Exception e) {
                serverStatus = false;
                log.error(e.toString());
            }
        });
    }

    @PreDestroy
    public void stopQcClerk() {
        serverStatus = false;
        qcClerk.Stop();
    }


    public boolean getServerStatus() {
        return serverStatus;
    }




}
