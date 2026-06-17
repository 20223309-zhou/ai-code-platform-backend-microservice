package com.ai.codeplatform.innerservice;

public interface InnerSysLogService {
    /**
     * 获取应用总数
     * @return Long
     */
     Long countTotalApps();

    /**
     * 计算应用生成成功率
     * @return Double
     */
     Double calculateSuccessRate();

    /**
     * 计算应用生成的平均耗时
     * @return Double
     */
     Double calculateAvgDuration();
}
