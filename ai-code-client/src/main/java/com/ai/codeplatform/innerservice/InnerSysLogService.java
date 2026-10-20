package com.ai.codeplatform.innerservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "ai-code-log", path = "/api/inner/sysLog")
public interface InnerSysLogService {
    /**
     * 获取应用总数
     * @return Long
     */
    @GetMapping("/countTotalApps")
    Long countTotalApps();

    /**
     * 计算应用生成成功率
     * @return Double
     */
    @GetMapping("/calculateSuccessRate")
    Double calculateSuccessRate();

    /**
     * 计算应用生成的平均耗时
     * @return Double
     */
    @GetMapping("/calculateAvgDuration")
    Double calculateAvgDuration();
}
