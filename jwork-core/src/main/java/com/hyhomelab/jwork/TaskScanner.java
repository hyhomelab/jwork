package com.hyhomelab.jwork;

import com.hyhomelab.jwork.repo.TaskRepo;
import com.hyhomelab.jwork.value.TaskStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/30 17:50
 */
@Slf4j
public class TaskScanner implements Runnable{

    private final TaskRepo repo;
    private final String name;
    private final BlockingQueue<Task> queue;
    private final String queueName;
    private final Integer scanNum;

    public TaskScanner(String queueName, TaskRepo repo, BlockingQueue<Task> queue, Integer scanNum) {
        this.queueName = queueName;
        this.name = queueName + "-scanner";
        this.repo = repo;
        this.queue = queue;
        this.scanNum = scanNum > 0 ? scanNum: 10;
    }


    @Override
    public void run() {
        log.info("[{}] run!", this.name);
        List<Task> tasks = repo.queryQueueTasksBefore(this.queueName, TaskStatus.PENDING, Instant.now().getEpochSecond(), scanNum);
        if(tasks != null && !tasks.isEmpty()){
            log.debug("[{}] found records: {}", this.name, tasks.size());
            for(var task : tasks){
                try {
                    queue.put(task);
                } catch (InterruptedException e) {
                    log.error("[%s] put task to queue err:%s".formatted(this.name, e.getMessage()));
                }
            }
        }
    }
}
