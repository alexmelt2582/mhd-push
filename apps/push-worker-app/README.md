*可部署应用*

异步消费与真正执行发送的工作进程。

职责：

- 消费 RocketMQ 等消息
- 执行去重、限流、敏感词、屏蔽、路由、渠道发送
- 处理失败重试、死信、回执更新、待确认状态
- 可按消费类型拆线程池与消费组


部署特点：

- 与 API 进程彻底分离
- 按 MQ 堆积量和发送吞吐独立扩容
- 可以按普通消息、顺序消息、回执处理再细分 worker 实例组
- 故障不会直接拖垮接口服务

生产部署建议：

- 按 topic lag 和发送量扩容
- 普通消息与顺序消息建议逻辑隔离
- 可以进一步拆成 worker-send、worker-recall、worker-receipt

来源映射：

当前 push-handler 大部分业务能力
尤其是 receiver 下的消费者
执行内核来自 ConsumeServiceImpl.java