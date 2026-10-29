package com.zhiguang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步与定时任务线程池配置。
 * <p>
 * 此前依赖默认设施：@Async 用 SimpleIOExecutor 风格的无界队列执行器堆积
 * 60 秒级 AI embedding 任务；@Scheduled 只有单线程，SSE 心跳一旦被慢客户端
 * 阻塞，浏览量落库等全部定时任务停摆。
 */
@Configuration
public class AsyncExecutorConfig {

    /** @Async 默认执行器：有界队列 + CallerRuns 反压（当前异步任务是 embedding 等 AI 慢调用） */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    /** 定时任务调度器：多线程，避免心跳阻塞拖垮其他 @Scheduled 任务 */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    /**
     * 同步 AI 端点的 worker 池：Controller 以 DeferredResult 立即释放 Tomcat 线程，
     * LLM 调用（10-60s）在本池执行，池满快速拒绝（503）而非压垮 Servlet 线程。
     * 与 AiService 内部的 LLM 并发信号量（48）配合：本池决定"占不占 Tomcat 线程"，
     * 信号量决定"多少调用真正打到 LLM"。
     */
    @Bean(name = "aiWorkerExecutor")
    public ThreadPoolTaskExecutor aiWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
