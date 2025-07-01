package com.hyhomelab.jwork;

import com.hyhomelab.jwork.exception.HandlerExistedException;
import com.hyhomelab.jwork.repo.TaskRepo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 14:22
 */
@Slf4j
public class TaskQueue {

    public static class Config{
        public int concurrentNum;
        public int maxRetryTimes;
    }

    private final Config cfg;
    private final String name;
    private final List<String> tags = new ArrayList<>();
    private final Map<String, TaskHandler> groupHandlerMap = new ConcurrentHashMap<>();
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final TaskRepo repo;
    private final Object lock = new Object();
    private final RunningController ctrl = new RunningController();
    private final TaskDispatcher dispatcher;
    private final ScheduledExecutorService scannerScheduler;

    public TaskQueue(String name, TaskRepo repo, int concurrentNum, int scanIntervalSec) {
        this.name = name + "-queue";
        this.repo = repo;
        this.cfg = defaultConfig();
        if(concurrentNum > 0){
            this.cfg.concurrentNum = concurrentNum;
        }
        // 任务调度器
        dispatcher = new TaskDispatcher(name,
                this.queue,  this.ctrl,
                this.groupHandlerMap, this.repo, this.cfg.maxRetryTimes,
                1, this.cfg.concurrentNum, 60L);
        dispatcher.start();
        // 任务扫描器
        TaskScanner scanner = new TaskScanner(name, this.repo, this.queue, this.cfg.concurrentNum * 2);
        scannerScheduler = Executors.newScheduledThreadPool(1);
        log.info("[{}] scanner start!", name);
        // 等查询任务结束后再延迟
        scannerScheduler.scheduleWithFixedDelay(scanner, 0, scanIntervalSec, TimeUnit.SECONDS);

    }

    public void OnFailed(BiConsumer<Task, Throwable> handler){
        this.dispatcher.setOnFailedHandler(handler);
    }

    public void shutdown(){
        synchronized (lock){
            this.queue.add(Task.PoisonTask);
            ctrl.stop();
            this.scannerScheduler.shutdown();
        }
    }

    private Config defaultConfig(){
        var cfg = new Config();
        cfg.concurrentNum = 10;
        cfg.maxRetryTimes = 3;
        return cfg;
    }

    public void regHandler(TaskHandler taskHandler){
        log.info("[{}] register task handler: {}, group={}", this.name,  taskHandler.getClass().getName(), taskHandler.group());
        if(groupHandlerMap.containsKey(taskHandler.group())){
            throw new HandlerExistedException(taskHandler.group());
        }
        groupHandlerMap.put(taskHandler.group(), taskHandler);
    }

}
