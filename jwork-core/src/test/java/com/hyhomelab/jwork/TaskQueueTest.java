package com.hyhomelab.jwork;

import com.google.gson.Gson;
import com.hyhomelab.jwork.repo.MemoryTaskRepoImpl;
import com.hyhomelab.jwork.trigger.RunAtTrigger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/6 13:53
 */
@Slf4j
public class TaskQueueTest {

    @Data
    public static class TestTaskData implements Serializable {
        private String orderSn;
        private BigDecimal amount;

        public TestTaskData() {
        }

        public TestTaskData(String orderSn, BigDecimal amount) {
            this.orderSn = orderSn;
            this.amount = amount;
        }
    }

    @Slf4j
    public static class TestTaskHandler implements TaskHandler {

        @Override
        public String queue() {
            return "test";
        }

        @Override
        public String group() {
            return "order";
        }

        @Override
        public void execute(TaskContext ctx) {
            var data = new Gson().fromJson(ctx.getTask().getData(), TestTaskData.class);
            if(ctx.getTask().getId() == 0){
                throw new RuntimeException("我出错啦");
            }
            log.info("orderSn={}", data.getOrderSn());
        }
    }

    @Slf4j
    public static class PringOrderAmountTaskHandler implements TaskHandler {

        @Override
        public String queue() {
            return "order";
        }

        @Override
        public String group() {
            return "default";
        }

        @Override
        public void execute(TaskContext ctx) {
            var data = new Gson().fromJson(ctx.getTask().getData(), TestTaskData.class);
            log.info("order amount ={}", data.getAmount());
        }
    }

    @Test
    public void testRun() throws InterruptedException {

        var repo = new MemoryTaskRepoImpl();
        var manager = new TaskManager(repo);
        manager.onFailed((t, e) -> log.error("task[{}] err!, e=", t.getTaskId(), e));

        manager.regHandler(new TestTaskHandler());
        manager.regHandler(new PringOrderAmountTaskHandler());

        for(var i=0;i<10;i++){
            manager.addTask("test", "order",
                    new TestTaskData(String.valueOf(i), new BigDecimal("100.1")),
                    new RunAtTrigger(Instant.now().plus(1L, ChronoUnit.SECONDS))
            );
        }
        for(var i=0;i<10;i++){
            manager.addTask("order", "default",
                    new TestTaskData(String.valueOf(i), new BigDecimal(100+i)),
                    new RunAtTrigger(Instant.now().plus(1L, ChronoUnit.SECONDS))
            );
        }
        Thread.sleep(Duration.ofSeconds(10L).toMillis());
        manager.shutdown();
        Thread.sleep(Duration.ofSeconds(1L).toMillis());
        System.out.println("over");
    }

}