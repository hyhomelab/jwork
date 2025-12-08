package com.hyhomelab.jwork;

import com.hyhomelab.jwork.value.TaskStatus;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 16:52
 */
@Data
public class TaskConfig {
    private String taskId;
    private TaskStatus initStatus;
    private Map<String ,Object> meta;

    public TaskConfig() {
        taskId = UUID.randomUUID().toString();
        initStatus = TaskStatus.PENDING;
        meta = new HashMap<>();
    }
}
