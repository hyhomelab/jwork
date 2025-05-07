package com.hyhomelab.jwork;

import com.hyhomelab.jwork.value.TaskStatus;

import java.util.function.Consumer;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 16:53
*/
@FunctionalInterface
public interface Option extends Consumer<Config> {

    static Option withConcurrentNum(int concurrentNum){
        return cfg -> cfg.setConcurrentNum(concurrentNum);
    }

    static Option withScanIntervalSec(int sec){
        return cfg -> cfg.setScanIntervalSec(sec);
    }

};

