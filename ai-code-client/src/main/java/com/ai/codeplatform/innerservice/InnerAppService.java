package com.ai.codeplatform.innerservice;

import com.ai.codeplatform.ai.model.entity.App;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "ai-code-app", path = "/api/inner/app")
public interface InnerAppService {
    // 更新应用表
    @PutMapping("/update")
    boolean updateApp(@RequestBody App app);
}
