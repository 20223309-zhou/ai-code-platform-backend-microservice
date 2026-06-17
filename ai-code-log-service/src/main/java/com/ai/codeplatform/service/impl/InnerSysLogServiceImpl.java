package com.ai.codeplatform.service.impl;

import com.ai.codeplatform.ai.model.entity.SysOperationLog;
import com.ai.codeplatform.innerservice.InnerSysLogService;
import com.ai.codeplatform.mapper.SysOperationLogMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@Slf4j
@DubboService
public class InnerSysLogServiceImpl implements InnerSysLogService {
    @Resource
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 查询应用总数
     * @return
     */
    public Long countTotalApps(){
        return sysOperationLogMapper.selectCountByQuery(QueryWrapper.create()
                .eq("operation","开始生成代码"));
    }

    /**
     * 计算应用生成成功率
     * @return
     */
    public Double calculateSuccessRate(){
        Long totalCount = countTotalApps();
        if (totalCount == 0) {
            return 0.0;
        }

        QueryWrapper successWrapper = QueryWrapper.create()
                .eq("status", "SUCCESS")
                .eq("operation","开始生成代码");
        Long successCount = sysOperationLogMapper.selectCountByQuery(successWrapper);

        return (successCount.doubleValue() / totalCount.doubleValue()) * 100;
    }

    /**
     * 计算应用生成的平均耗时
     * @return
     */
    public Double calculateAvgDuration(){
        // 查询所有已完成的app，计算 createTime 到 completedTime 的平均耗时
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", "SUCCESS")
                .eq("operation","开始生成代码");
        try {
            List<SysOperationLog> completeApps = sysOperationLogMapper.selectListByQuery(queryWrapper);
            if (completeApps == null || completeApps.isEmpty()) {
                return 0.0;
            }
            // 计算每个应用生成的耗时（毫秒）
            return completeApps.stream()
                    .mapToDouble(log -> log.getDurationMs().doubleValue())
                    .average()
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("计算平均耗时失败", e);
        }
        return 0.0;
    }

}
