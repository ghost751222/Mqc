package com.mqc.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
@Slf4j
public class TaskQueueService {
    ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    @Autowired
    @Qualifier("executorService")
    @Lazy
    private ExecutorService executorService;

    private boolean isExecuteTask = false;

    Future<?> future;

    @Async
    public void addToQueue(Runnable runnable) {
        queue.add(runnable);
    }

    @Async
    public Runnable getFromQueue() {
        return queue.poll();
    }
    //@Scheduled(fixedDelay = 30000)
    private void scanTask(){
        log.info("size ={} ,isExecuteTask ={}", queue.size(), isExecuteTask);
    }

    @Scheduled(fixedDelay = 100)
    private void executeTask() {
        try {
            if (!queue.isEmpty() && !isExecuteTask) {
                isExecuteTask = true;
                Runnable runnable = this.getFromQueue();
                if (runnable != null) {
                    future = executorService.submit(runnable);
                    while (!future.isDone()) {
                        if (future.isDone()) break;
                    }
                }
                isExecuteTask = false;
            }

        } catch (Exception e) {
            log.error(e.toString());
            isExecuteTask = false;
        }

    }
}
