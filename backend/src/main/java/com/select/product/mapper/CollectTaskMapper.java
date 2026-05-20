package com.select.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.select.product.entity.CollectTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CollectTaskMapper extends BaseMapper<CollectTask> {

    List<CollectTask> selectByStatus(@Param("status") String status);

    List<CollectTask> selectPendingTasks();

    /** 查询 RUNNING 状态但 start_time 超过指定时间点的"卡死"任务 */
    List<CollectTask> selectStuckTasks(@Param("deadline") LocalDateTime deadline);
}

