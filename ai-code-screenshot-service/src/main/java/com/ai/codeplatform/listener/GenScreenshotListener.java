package com.ai.codeplatform.listener;

import com.ai.codeplatform.ai.model.entity.App;
import com.ai.codeplatform.exception.BusinessException;
import com.ai.codeplatform.exception.ErrorCode;
import com.ai.codeplatform.innerservice.InnerAppService;
import com.ai.codeplatform.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.List;
/**
 * 监听生成截图的消息
 */
@Component
public class GenScreenshotListener {
    @Resource
    private ScreenshotService screenshotService;
    @DubboReference
    private InnerAppService innerAppService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "screenshot.queue1",durable = "true"),
            exchange = @Exchange(name = "screenshot.topic", type = ExchangeTypes.TOPIC),
            key = "screenshot.generate"
    ) )
    public void genScreenshotListener1(List<String> params) {
        saveScreenshot(params);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "screenshot.queue1",durable = "true"),
            exchange = @Exchange(name = "screenshot.topic", type = ExchangeTypes.TOPIC),
            key = "screenshot.generate"
    ) )
    public void genScreenshotListener2(List<String> params) {
        saveScreenshot(params);
    }

    private void saveScreenshot(List<String> params) {
        Long appId = Long.valueOf(params.getFirst());
        String appDeployUrl = params.getLast();
        String screenshotUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
        // 更新应用封面字段
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCover(screenshotUrl);
        boolean updated = innerAppService.updateApp(updateApp);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        }
    }
}
