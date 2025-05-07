package com.hyhomelab.jwork.trigger;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.hyhomelab.jwork.Trigger;
import lombok.Data;

import java.time.ZonedDateTime;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 15:53
 */
@Data
public class CronTrigger implements Trigger {

    private String cron;

    public CronTrigger(String cron){
        parse(cron);
        this.cron = cron;
    }

    private Cron parse(String cron){
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        return parser.parse(cron).validate();
    }

    @Override
    public long nextTimeSec() {
        Cron cron = parse(this.cron);
        ExecutionTime execTime = ExecutionTime.forCron(cron);
        return execTime.nextExecution(ZonedDateTime.now()).get().toEpochSecond();
    }

}
