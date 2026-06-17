package com.ai.codeplatform.listener;

import com.ai.codeplatform.ai.model.entity.SysOperationLog;
import com.ai.codeplatform.service.SysOperationLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RecordLogListener {
    @Resource
    private SysOperationLogService sysOperationLogService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "log.queue1", durable = "true"),
            exchange = @Exchange(name = "log.topic", type = ExchangeTypes.TOPIC),
            key = "log.record"
    ))
    public void recordLogListener1(SysOperationLog logInfo){
        boolean isSuccess = sysOperationLogService.save(logInfo);
        if (!isSuccess){
            log.error("MQ消费者1：接口调用日志写入数据库失败");
        }else{
            log.info("MQ消费者1：接口调用日志写入数据库成功");
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "log.queue1", durable = "true"),
            exchange = @Exchange(name = "log.topic", type = ExchangeTypes.TOPIC),
            key = "log.record"
    ))
    public void recordLogListener2(SysOperationLog logInfo){
        boolean isSuccess = sysOperationLogService.save(logInfo);
        if (!isSuccess){
            log.error("MQ消费者2：接口调用日志写入数据库失败");
        }else{
            log.info("MQ消费者2：接口调用日志写入数据库成功");
        }
    }

}
