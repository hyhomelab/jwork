package com.hyhomelab.jwork;

import com.google.gson.Gson;
import com.hyhomelab.jwork.exception.RepoNotFoundException;
import com.hyhomelab.jwork.exception.TaskExistedException;
import com.hyhomelab.jwork.repo.TaskRepo;
import com.hyhomelab.jwork.trigger.RunAtTrigger;
import com.hyhomelab.jwork.value.TaskStatus;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/6 15:53
 */
public class TaskManager {

    private final TaskRepo repo;
    private final Map<String, TaskQueue> queueMap = new HashMap<>();
    private final Object lock = new Object();
    private BiConsumer<Task, Throwable> failedHandler = (t, e) -> {};
    private final Config cfg;

    public TaskManager(TaskRepo repo, Option...options) {
        if(repo == null){
            throw new RepoNotFoundException("task repository must not be null");
        }
        this.repo = repo;

        this.cfg = defaultConfig();
        Optional.ofNullable(options).ifPresent( opts -> {
            for(var opt : options){
                opt.accept(this.cfg);
            }
        });

    }

    private Config defaultConfig() {
        var cfg = new Config();
        cfg.setScanIntervalSec(2);
        cfg.setConcurrentNum(10);
        cfg.setOnlyAllowQueues(List.of());
        return cfg;
    }

    private String getQueueName(String name){
        var queueName = name;
        if(this.cfg.getPrefix() != null && !this.cfg.getPrefix().trim().isEmpty()){
            queueName = this.cfg.getPrefix() + "-" + name;
        }
        return queueName;
    }

    public void regHandler(TaskHandler handler){
        synchronized (lock){
            var q = queueMap.computeIfAbsent(handler.queue(), k -> {
                var queueName = this.getQueueName(k);
                boolean allowStart = true;
                if(!this.cfg.getOnlyAllowQueues().isEmpty()){
                    allowStart = this.cfg.getOnlyAllowQueues().contains(k);
                }
                var newQueue = new TaskQueue(queueName, repo, this.cfg.getConcurrentNum(), this.cfg.getScanIntervalSec(), allowStart);
                newQueue.OnFailed(this.failedHandler);
                return newQueue;
            });
            q.regHandler(handler);
        }
    }

    /**
     * trigger task to pending status
     * @param taskId
     * @param trigger
     */
    public void triggerTask(String taskId, Trigger trigger){
        var task = this.repo.getByTaskId(taskId);
        if(task != null && task.getStatus() == TaskStatus.NOT_TRIGGERED){
            this.repo.triggerToPending(taskId, TaskStatus.PENDING, trigger.nextTimeSec(), trigger);
        }
    }

    public void onFailed(BiConsumer<Task, Throwable> handler){
        this.failedHandler = handler;
        this.queueMap.forEach((name, queue) -> {
            queue.OnFailed(handler);
        });
    }

    /**
     * add a not trigger task, on trigger when call triggerTask method  change task to pending status
     * @param queue
     * @param group
     * @param taskId
     * @param data
     * @return taskId
     */
    public String addUnTriggerTask(String queue, String group,String taskId, Serializable data) throws TaskExistedException {
        var unReachableTimeTrigger = new RunAtTrigger(LocalDateTime.of(9999, 12, 30, 23, 59, 59).toInstant(ZoneOffset.UTC));
        return this.addTask(queue, group, data, unReachableTimeTrigger, TaskOption.withInitStatus(TaskStatus.NOT_TRIGGERED), TaskOption.withTaskId(taskId));
    }

    /**
     * retry failed task
     * @param taskId
     */
    public void retry(String taskId){
        var task = repo.getByTaskId(taskId);
        if(task.getStatus() == TaskStatus.FAILED){
            repo.resetTo(taskId, TaskStatus.PENDING, Instant.now().getEpochSecond(), 0);
        }
    }

    public String addTask(String queue, String group, Serializable data, Trigger trigger, TaskOption... options) throws TaskExistedException {
        // 设置个性化配置
        var taskCfg = new TaskConfig();
        for(var opt : options){
            opt.accept(taskCfg);
        }

        var task = new Task();
        task.setTaskId(taskCfg.getTaskId());
        var queueName = this.getQueueName(queue);
        task.setQueue(queueName);
        task.setGroup(group);
        task.setStatus(taskCfg.getInitStatus());


        var jsonData = new Gson().toJson(data);
        task.setData(jsonData);
        task.setRetryTimes(0);
        task.setNextTimeSec(trigger.nextTimeSec());
        task.setTrigger(trigger);
        task.setMeta(taskCfg.getMeta());

        // 持久化
        repo.create(task);
        return task.getTaskId();
    }

    public void shutdown() {
        synchronized (lock){
            this.queueMap.forEach((name, queue) -> queue.shutdown());
        }
    }
}
