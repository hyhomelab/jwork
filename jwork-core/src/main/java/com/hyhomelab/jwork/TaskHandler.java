package com.hyhomelab.jwork;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 14:33
 */
public interface TaskHandler {


    String queue();

    String group();

    void execute(TaskContext ctx);
}
