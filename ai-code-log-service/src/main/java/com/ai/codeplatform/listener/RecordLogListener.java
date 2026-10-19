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

/**
 * 监听日志记录的消息。
 * <p>
 * 两个监听方法绑定同一个队列，属于「同一队列的多个消费者」，消息是竞争消费、不会重复入库，
 * 只是把消费并发度提到 2。
 */
@Component
@Slf4j
public class RecordLogListener {

    /**
     * operation 列的宽度（varchar(80)）。
     * <p>
     * 异常 message 可能远超此长度（例如 Redis 连接异常会带上 Netty pending tasks、完整命令参数，
     * 动辄上千字符），一旦超长会导致<b>整条日志写入失败</b>，把真实异常也一起掩盖掉 ——
     * 日志里只剩一条无意义的 "Data too long for column 'operation'"，反而查不到根因。
     * 因此入库前强制截断：完整堆栈仍在应用日志里，DB 这条只承担检索用途。
     */
    private static final int OPERATION_MAX_LENGTH = 80;

    @Resource
    private SysOperationLogService sysOperationLogService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "log.queue1", durable = "true"),
            exchange = @Exchange(name = "log.topic", type = ExchangeTypes.TOPIC),
            key = "log.record"
    ))
    public void recordLogListener1(SysOperationLog logInfo) {
        persist(logInfo, "MQ消费者1");
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "log.queue1", durable = "true"),
            exchange = @Exchange(name = "log.topic", type = ExchangeTypes.TOPIC),
            key = "log.record"
    ))
    public void recordLogListener2(SysOperationLog logInfo) {
        persist(logInfo, "MQ消费者2");
    }

    /**
     * 截断超长字段后入库
     */
    private void persist(SysOperationLog logInfo, String consumerName) {
        String operation = logInfo.getOperation();
        if (operation != null && operation.length() > OPERATION_MAX_LENGTH) {
            logInfo.setOperation(operation.substring(0, OPERATION_MAX_LENGTH));
        }
        boolean isSuccess = sysOperationLogService.save(logInfo);
        if (!isSuccess) {
            log.error("{}：接口调用日志写入数据库失败", consumerName);
        } else {
            log.info("{}：接口调用日志写入数据库成功", consumerName);
        }
    }
}
