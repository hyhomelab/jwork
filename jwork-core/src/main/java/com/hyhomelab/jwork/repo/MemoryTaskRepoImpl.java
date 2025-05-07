package com.hyhomelab.jwork.repo;

import com.hyhomelab.jwork.Task;
import com.hyhomelab.jwork.Trigger;
import com.hyhomelab.jwork.exception.TaskExistedException;
import com.hyhomelab.jwork.value.TaskStatus;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/6 11:12
 */
public class MemoryTaskRepoImpl implements TaskRepo{

    private final List<Task> storage = new LinkedList<>();

    private final Object lock = new Object();

    @Override
    public void create(Task task) throws TaskExistedException {
        synchronized (lock){
            if(storage.stream().anyMatch(e -> e.getTaskId().equals(task.getTaskId()))){
                throw new TaskExistedException("task existed! taskId = %s".formatted(task.getTaskId()));
            }
            task.setId(storage.size());
            storage.add(task);
        }
    }

    @Override
    public boolean swapToRunning(String taskId, TaskStatus currentStatus, TaskStatus runningStatus, int maxRetryTimes) {
        synchronized (lock){
            var task = storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst().orElseThrow(() -> new RuntimeException("task not found: %s".formatted(taskId)));
            if(task.getStatus() == currentStatus){
                task.setStatus(runningStatus);
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveSuccessResult(String taskId, TaskStatus taskStatus, String msg) {
        synchronized (lock){
            var task = storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst();
            if(task.isPresent()){
                var trigger = task.get().getTrigger();
                if(trigger != null && trigger.nextTimeSec() < Instant.now().getEpochSecond()){
                    // 内存模式直接移除掉
                    task.ifPresent(storage::remove);
                }
            }
        }
    }

    @Override
    public void changeStatusTo(String taskId, TaskStatus taskStatus) {
        synchronized (lock){
            var task = storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst().orElseThrow(() -> new RuntimeException("task not found: %s".formatted(taskId)));
            task.setStatus(taskStatus);
        }
    }

    @Override
    public List<Task> queryQueueTasksBefore(String queueName, TaskStatus taskStatus, long beforeSeconds, Integer scanLimitNum) {
        return storage.stream().filter(e -> e.getQueue().equals(queueName) && e.getStatus() == taskStatus && e.getNextTimeSec() < beforeSeconds).limit(scanLimitNum).toList();
    }

    @Override
    public void saveFailedResult(String taskId, TaskStatus taskStatus, Integer retryTimes, String message) {
        synchronized (lock){
            var task = storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst();
            task.ifPresent(value -> {
                value.setRetryTimes(retryTimes + 1);
                value.setResult(message);
            });
        }
    }

    @Override
    public Task getByTaskId(String taskId) {
        return storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst().orElseThrow(() -> new RuntimeException("task not found: %s".formatted(taskId)));
    }

    @Override
    public void triggerToPending(String taskId, TaskStatus taskStatus, long nextTime, Trigger trigger) {
        synchronized (lock){
            var task = storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst();
            task.ifPresent(value -> {
                value.setStatus(taskStatus);
                value.setNextTimeSec(nextTime);
                value.setTrigger(trigger);
            });
        }
    }

    @Override
    public void resetTo(String taskId, TaskStatus taskStatus, long nextTimeSec, int resetRetryTimes) {
        synchronized (lock){
            var task = storage.stream().filter(e -> e.getTaskId().equals(taskId)).findFirst();
            task.ifPresent(value -> {
                value.setStatus(taskStatus);
                value.setNextTimeSec(nextTimeSec);
                value.setRetryTimes(resetRetryTimes);
            });
        }
    }
}
