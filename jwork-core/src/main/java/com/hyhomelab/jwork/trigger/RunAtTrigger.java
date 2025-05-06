package com.hyhomelab.jwork.trigger;

import com.hyhomelab.jwork.Trigger;

import java.time.Instant;
import java.util.Date;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 15:46
 */
public class RunAtTrigger implements Trigger {

    private final long runAtSeconds;

    public RunAtTrigger(Date time) {
        this.runAtSeconds = time.toInstant().getEpochSecond();
    }

    public RunAtTrigger(Instant ins) {
        this.runAtSeconds = ins.getEpochSecond();
    }

    @Override
    public long nextTime() {
        return this.runAtSeconds;
    }
}
