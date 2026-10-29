package com.zhiguang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * <p>
 * 定义队列、交换机、路由键：
 * - notification.queue：通知异步发送
 * - article.view.sync.queue：浏览量同步回写数据库
 * - ai.task.queue：AI 任务异步处理
 * - search.index.queue：搜索索引同步
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机 ==========

    /**
     * 通知交换机
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange("notification.exchange");
    }

    /**
     * 文章交换机
     */
    @Bean
    public DirectExchange articleExchange() {
        return new DirectExchange("article.exchange");
    }

    /**
     * AI 交换机
     */
    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange("ai.exchange");
    }

    /**
     * 搜索交换机
     */
    @Bean
    public DirectExchange searchExchange() {
        return new DirectExchange("search.exchange");
    }

    // ========== 队列 ==========
    //
    // 注意：为既有队列新增 x-dead-letter-exchange 参数属于声明变更，
    // 对已存在的旧队列 RabbitMQ 会拒绝（PRECONDITION_FAILED）。
    // 升级部署前需删除旧队列（管理台或 rabbitmqctl），或使用全新数据卷。

    /**
     * 死信交换机：消费重试耗尽或被拒绝的消息进入对应 DLQ，不再静默丢失
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange");
    }

    /**
     * 通知队列（超时未消费的死信转入 DLQ，60 秒 TTL 内未处理的通知不再无声蒸发）
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("notification.queue")
                .withArgument("x-message-ttl", 60000)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "notification.queue.dlq")
                .build();
    }

    /**
     * 浏览量同步队列
     */
    @Bean
    public Queue articleViewSyncQueue() {
        return QueueBuilder.durable("article.view.sync.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "article.view.sync.queue.dlq")
                .build();
    }

    /**
     * AI 任务队列（当前无消费者，消息按 TTL 过期后进 DLQ 留痕）
     */
    @Bean
    public Queue aiTaskQueue() {
        return QueueBuilder.durable("ai.task.queue")
                .withArgument("x-message-ttl", 300000)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "ai.task.queue.dlq")
                .build();
    }

    /**
     * 搜索索引同步队列
     */
    @Bean
    public Queue searchIndexQueue() {
        return QueueBuilder.durable("search.index.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "search.index.queue.dlq")
                .build();
    }

    // ========== 死信队列 ==========

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable("notification.queue.dlq").build();
    }

    @Bean
    public Queue articleViewSyncDlq() {
        return QueueBuilder.durable("article.view.sync.queue.dlq").build();
    }

    @Bean
    public Queue aiTaskDlq() {
        return QueueBuilder.durable("ai.task.queue.dlq").build();
    }

    @Bean
    public Queue searchIndexDlq() {
        return QueueBuilder.durable("search.index.queue.dlq").build();
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with("notification.queue.dlq");
    }

    @Bean
    public Binding articleViewSyncDlqBinding() {
        return BindingBuilder.bind(articleViewSyncDlq()).to(deadLetterExchange()).with("article.view.sync.queue.dlq");
    }

    @Bean
    public Binding aiTaskDlqBinding() {
        return BindingBuilder.bind(aiTaskDlq()).to(deadLetterExchange()).with("ai.task.queue.dlq");
    }

    @Bean
    public Binding searchIndexDlqBinding() {
        return BindingBuilder.bind(searchIndexDlq()).to(deadLetterExchange()).with("search.index.queue.dlq");
    }

    // ========== 绑定 ==========

    /**
     * 通知队列绑定
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with("notification.routing");
    }

    /**
     * 浏览量同步队列绑定
     */
    @Bean
    public Binding articleViewSyncBinding() {
        return BindingBuilder.bind(articleViewSyncQueue())
                .to(articleExchange())
                .with("article.view.sync.routing");
    }

    /**
     * AI 任务队列绑定
     */
    @Bean
    public Binding aiTaskBinding() {
        return BindingBuilder.bind(aiTaskQueue())
                .to(aiExchange())
                .with("ai.task.routing");
    }

    /**
     * 搜索索引队列绑定
     */
    @Bean
    public Binding searchIndexBinding() {
        return BindingBuilder.bind(searchIndexQueue())
                .to(searchExchange())
                .with("search.index.routing");
    }

    // ========== 配置 ==========

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
}