package com.hyhomelab.jwork;

import com.hyhomelab.jwork.value.TaskStatus;

import java.util.function.Consumer;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 16:53
*/
@FunctionalInterface
public interface TaskOption extends Consumer<TaskConfig> {

    public static TaskOption withTaskId(String taskId){
        return cfg -> cfg.setTaskId(taskId);
    }

    public static TaskOption withInitStatus(TaskStatus status){
        return cfg -> cfg.setInitStatus(status);
    }

};

