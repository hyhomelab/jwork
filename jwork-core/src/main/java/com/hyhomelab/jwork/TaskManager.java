package com.hyhomelab.jwork;

import com.google.gson.Gson;
import com.hyhomelab.jwork.exception.RepoNotFoundException;
import com.hyhomelab.jwork.repo.TaskRepo;
import com.hyhomelab.jwork.trigger.RunAtTrigger;
import com.hyhomelab.jwork.value.TaskStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/6 15:53
 */
public class TaskManager {

    public static final int DEFAULT_CONCURRENT_NUM = 10;

    private final TaskRepo repo;
    private final Map<String, TaskQueue> queueMap = new HashMap<>();
    private final Object lock = new Object();
    private BiConsumer<Task, Throwable> failedHandler = (t, e) -> {};

    public TaskManager(TaskRepo repo) {
        if(repo == null){
            throw new RepoNotFoundException("task repository must not be null");
        }
        this.repo = repo;
    }

    public void regHandler(TaskHandler handler){
        synchronized (lock){
            var q = queueMap.computeIfAbsent(handler.queue(), k -> {
                var newQueue = new TaskQueue(k, repo, DEFAULT_CONCURRENT_NUM);
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
    public String addUnTriggerTask(String queue, String group,String taskId, Serializable data){
        var unReachableTimeTrigger = new RunAtTrigger(LocalDateTime.of(9999, 12, 30, 23, 59, 59).toInstant(ZoneOffset.UTC));
        return this.addTask(queue, group, data, unReachableTimeTrigger, TaskOption.withInitStatus(TaskStatus.NOT_TRIGGERED), TaskOption.withTaskId(taskId));
    }

    public String addTask(String queue, String group, Serializable data, Trigger trigger, TaskOption... options){
        // 设置个性化配置
        var taskCfg = new TaskConfig();
        for(var opt : options){
            opt.accept(taskCfg);
        }

        var task = new Task();
        task.setTaskId(taskCfg.getTaskId());
        task.setQueue(queue);
        task.setGroup(group);
        task.setStatus(taskCfg.getInitStatus());


        var jsonData = new Gson().toJson(data);
        task.setData(jsonData);
        task.setRetryTimes(0);
        task.setNextTimeSec(trigger.nextTimeSec());
        task.setTrigger(trigger);

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
