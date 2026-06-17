package com.ai.codeplatform.service.impl;

import com.ai.codeplatform.ai.model.entity.App;
import com.ai.codeplatform.innerservice.InnerAppService;
import com.ai.codeplatform.service.AppService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class InnerAppServiceImpl implements InnerAppService {
    @Resource
    private AppService appService;
    /**
     * 更新app
     * @param app
     * @return
     */
    @Override
    public boolean updateApp(App app) {
        return appService.updateById(app);
    }
}
