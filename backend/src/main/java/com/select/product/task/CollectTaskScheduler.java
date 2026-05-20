package com.select.product.task;

import com.select.product.dto.CollectTaskDTO;
import com.select.product.entity.CollectTask;
import com.select.product.entity.KeywordConfig;
import com.select.product.mapper.CollectTaskMapper;
import com.select.product.service.CollectService;
import com.select.product.service.KeywordConfigService;
import com.select.product.service.RankingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectTaskScheduler {

    /** 任务执行超时阈值（分钟）：超过此时间仍 RUNNING 则强制标记失败 */
    private static final int STUCK_TIMEOUT_MINUTES = 15;

    private final CollectTaskMapper collectTaskMapper;
    private final CollectService collectService;
    private final KeywordConfigService keywordConfigService;
    private final RankingHistoryService rankingHistoryService;

    /** 每30秒同步一次 RUNNING 状态的任务 */
    @Scheduled(fixedDelay = 30000)
    public void syncRunningTasks() {
        List<CollectTask> runningTasks = collectTaskMapper.selectByStatus("RUNNING");
        if (!runningTasks.isEmpty()) {
            log.info("同步运行中任务数量: {}", runningTasks.size());
            for (CollectTask task : runningTasks) {
                try {
                    collectService.syncTaskStatus(task.getId());
                } catch (Exception e) {
                    log.error("同步任务状态失败: taskId={}", task.getId(), e);
                }
            }
        }
    }

    /** 每60秒检测一次：RUNNING 超过 STUCK_TIMEOUT_MINUTES 分钟的任务强制标记失败 */
    @Scheduled(fixedDelay = 60000)
    public void detectAndFailStuckTasks() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(STUCK_TIMEOUT_MINUTES);
        List<CollectTask> stuckTasks = collectTaskMapper.selectStuckTasks(deadline);
        for (CollectTask task : stuckTasks) {
            log.warn("[StuckTask] 任务 {} ({}) 已执行 {}+ 分钟无响应，强制标记为失败",
                    task.getId(), task.getKeyword(), STUCK_TIMEOUT_MINUTES);
            task.setStatus("FAILED");
            task.setErrorMsg("任务执行超时（超过 " + STUCK_TIMEOUT_MINUTES + " 分钟无响应），请检查平台配置后重试");
            task.setEndTime(LocalDateTime.now());
            collectTaskMapper.updateById(task);
        }
    }

    /** 每日06:00 触发启用关键词的定时采集 */
    @Scheduled(cron = "0 0 6 * * ?")
    public void scheduledKeywordCollect() {
        List<KeywordConfig> configs = keywordConfigService.listEnabled();
        log.info("定时采集触发，启用关键词数量: {}", configs.size());
        for (KeywordConfig config : configs) {
            try {
                CollectTaskDTO dto = new CollectTaskDTO();
                dto.setTaskName("定时采集-" + config.getKeyword());
                dto.setPlatform(config.getPlatform());
                dto.setKeyword(config.getKeyword());
                dto.setMaxCount(config.getMaxCount() != null ? config.getMaxCount() : 50);
                CollectTask task = collectService.createTask(dto);
                collectService.executeTask(task.getId());
            } catch (Exception e) {
                log.error("定时采集失败: keyword={}", config.getKeyword(), e);
            }
        }
    }

    /** 每日03:00 记录排名快照 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void recordDailyRankingSnapshot() {
        log.info("开始记录每日排名快照");
        rankingHistoryService.recordSnapshot("TODAY");
        rankingHistoryService.recordSnapshot("WEEK");
        rankingHistoryService.recordSnapshot("MONTH");
    }
}
