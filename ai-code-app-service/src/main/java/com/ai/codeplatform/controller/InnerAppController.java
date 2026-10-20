package com.ai.codeplatform.controller;

import com.ai.codeplatform.ai.model.entity.App;
import com.ai.codeplatform.annotation.InnerApi;
import com.ai.codeplatform.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@InnerApi
@RestController
@RequestMapping("/inner/app")
public class InnerAppController{
    @Resource
    private AppService appService;
    /**
     * 更新app
     * @param app
     * @return
     */
    @PutMapping("/update")
    public boolean updateApp(@RequestBody App app) {
        return appService.updateById(app);
    }
}
