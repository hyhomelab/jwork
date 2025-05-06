package com.hyhomelab.jwork;

import com.hyhomelab.jwork.repo.TaskRepo;
import com.hyhomelab.jwork.value.TaskStatus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 22:18
 */
@Slf4j
public class TaskDispatcher extends Thread{

    private final Queue<Task> taskQueue;
    private final RunningController runningController;
    private final ThreadPoolExecutor executor;
    private final Map<String, TaskHandler> groupHandlerMap;
    private final TaskRepo repo;
    private final int maxRetryTimes;

    @Setter
    private BiConsumer<Task, Throwable> onFailedHandler = (t, e) -> {};

    public static class BlockWhenReject implements RejectedExecutionHandler{

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public TaskDispatcher(String name, Queue<Task> queue, RunningController controller,
                          Map<String, TaskHandler> groupHandlerMap,
                          TaskRepo repo, int maxRetryTimes,
                          int idleNum, int concurrentNum, long idleSecs){
        super(name + "-dispatcher");
        this.taskQueue = queue;
        this.runningController = controller;
        this.groupHandlerMap = groupHandlerMap;
        this.repo = repo;
        this.maxRetryTimes = maxRetryTimes;

        // 自定义线程工厂
        var threadFactory = new ThreadFactory(){
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("%s-thread-%d".formatted(name, threadNumber.getAndIncrement()));
                return t;
            }
        };

        // 弹性线程池
        executor = new ThreadPoolExecutor(
                idleNum, concurrentNum,
                idleSecs, TimeUnit.MINUTES,
                new SynchronousQueue<>(),
                threadFactory,
                new BlockWhenReject()
        );

    }

    @Override
    public void run() {
        log.info("[{}] start!", this.getName());
        while(runningController.isRunning() && !isInterrupted()){

            Task task = taskQueue.poll();
            if(task != null){
                log.debug("[{}] get task! taskId={}", this.getName(), task.getTaskId());
                if(task.getId() == -1){
                    log.info("[{}] get poison task!!!", this.getName());
                    break;
                }
                var handler = groupHandlerMap.get(task.getGroup());
                if(handler == null){
                    log.warn("[{}] handler(group={}) not found, skip task：{}",this.getName(), task.getGroup(), task.getTaskId());
                    continue;
                }

                runTask(task, handler);
            }
        }
        executor.shutdown();
        log.info("[{}] shutdown", this.getName());
        try {
            if(!executor.awaitTermination(120, TimeUnit.SECONDS)){
                List<Runnable> dropRunnableList = executor.shutdownNow();
                log.error("[{}] force shutdown, drops threads: {}", this.getName(), dropRunnableList.size());
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }


    }

    private void runTask(Task task, TaskHandler taskHandler) {
        final var ctx = new TaskContext();
        ctx.setTask(task);
        executor.submit(() -> {
            if(repo.swapToRunning(ctx.getTask().getTaskId(), ctx.getTask().getStatus(), TaskStatus.RUNNING, maxRetryTimes)){
                try{
                    taskHandler.execute(ctx);
                    repo.saveSuccessResult(ctx.getTask().getTaskId(), TaskStatus.SUCCESS, "success");
                    log.debug("[{}] execute task={} success!",this.getName(), ctx.getTask().getTaskId());

                } catch(Exception e){
                    repo.saveFailedResult(ctx.getTask().getTaskId(), TaskStatus.FAILED, task.getRetryTimes(), e.getMessage());
                    log.error("[{}] execute task={} failed!, cause {}",this.getName(), ctx.getTask().getTaskId(), e.getMessage());
                    if(ctx.getTask().getRetryTimes() + 1 <= maxRetryTimes){
                        repo.changeStatusTo(ctx.getTask().getTaskId(), TaskStatus.PENDING);
                    }else{
                        onFailedHandler.accept(ctx.getTask(), e);
                    }
                }
            }
        });
    }
}
