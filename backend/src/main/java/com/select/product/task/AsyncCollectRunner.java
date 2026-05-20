package com.select.product.task;

import com.select.product.entity.CollectTask;
import com.select.product.mapper.CollectTaskMapper;
import com.select.product.service.impl.CollectServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * 异步采集任务执行器
 * 独立组件以确保 @Async 通过 Spring 代理生效（同类调用 @Async 无效）
 * 内置超时控制，防止 SDK 调用无限阻塞
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncCollectRunner {

    /** 单次采集任务最长执行时间（分钟） */
    private static final int TASK_TIMEOUT_MINUTES = 10;

    private final CollectServiceImpl collectService;
    private final CollectTaskMapper collectTaskMapper;

    @Async("taskExecutor")
    public void run(CollectTask task) {
        log.info("[AsyncCollectRunner] 任务开始: taskId={}, keyword={}, thread={}",
                task.getId(), task.getKeyword(), Thread.currentThread().getName());

        // 使用单独线程 + Future.get(timeout) 对 SDK 调用加超时控制
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "collect-sdk-" + task.getId());
            t.setDaemon(true);
            return t;
        });

        Future<?> future = executor.submit(() -> collectService.doCollect(task));

        try {
            future.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            log.info("[AsyncCollectRunner] 任务完成: taskId={}", task.getId());
        } catch (TimeoutException e) {
            future.cancel(true); // 尝试中断
            log.error("[AsyncCollectRunner] 任务超时({}min): taskId={}, 将由调度器标记失败",
                    TASK_TIMEOUT_MINUTES, task.getId());
            // 立即标记失败（不等调度器）
            try {
                CollectTask latest = collectTaskMapper.selectById(task.getId());
                if (latest != null && "RUNNING".equals(latest.getStatus())) {
                    latest.setStatus("FAILED");
                    latest.setErrorMsg("任务执行超时（超过 " + TASK_TIMEOUT_MINUTES + " 分钟），SDK调用无响应，请检查1688平台配置及网络");
                    latest.setEndTime(LocalDateTime.now());
                    collectTaskMapper.updateById(latest);
                }
            } catch (Exception ex) {
                log.error("[AsyncCollectRunner] 更新超时任务状态失败: taskId={}", task.getId(), ex);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[AsyncCollectRunner] 任务被中断: taskId={}", task.getId());
        } catch (ExecutionException e) {
            log.error("[AsyncCollectRunner] 任务执行异常: taskId={}", task.getId(), e.getCause());
            // doCollect 内部已处理异常并更新 DB，此处仅记录
        } finally {
            executor.shutdownNow();
        }
    }
}
