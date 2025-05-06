package com.hyhomelab.jwork;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 22:26
 */
public class RunningController {
    private volatile boolean running = true;

    public boolean isRunning(){
        return running;
    }

    public void stop(){
        running = false;
    }
}
