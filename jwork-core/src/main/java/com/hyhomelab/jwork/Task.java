package com.hyhomelab.jwork;

import com.hyhomelab.jwork.value.TaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 13:56
 */
@Data
@EqualsAndHashCode(of = {"taskId"})
public class Task {

    // 毒丸任务
    public static final Task PoisonTask = new Task(-1L, "poisonTask");

    public Task() {
    }

    public Task(long id, String taskId) {
        this.id = id;
        this.taskId = taskId;
    }

    private long id;
    private String taskId;
    private String queue;
    private String group;
    private TaskStatus status;
    private long nextTimeSec;
    private String data;
    private Trigger trigger;
    private String result;
    private Integer retryTimes;
    private Map<String, Object> meta;


}
