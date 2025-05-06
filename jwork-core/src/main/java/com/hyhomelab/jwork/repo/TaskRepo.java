package com.hyhomelab.jwork.repo;

import com.hyhomelab.jwork.Task;
import com.hyhomelab.jwork.Trigger;
import com.hyhomelab.jwork.value.TaskStatus;

import java.util.List;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 17:16
 */
public interface TaskRepo {
    void create(Task task);

    boolean swapToRunning(String taskId, TaskStatus currentStatus, TaskStatus runningStatus, int maxRetryTimes);

    void saveSuccessResult(String taskId, TaskStatus taskStatus, String msg);

    void changeStatusTo(String taskId, TaskStatus taskStatus);

    List<Task> queryQueueTasksBefore(String queueName, TaskStatus taskStatus, long beforeSeconds, Integer scanLimitNum);

    void saveFailedResult(String taskId, TaskStatus taskStatus, Integer retryTimes, String message);

    Task getByTaskId(String taskId);

    void triggerToPending(String taskId, TaskStatus taskStatus, long nextTime, Trigger trigger);
}
