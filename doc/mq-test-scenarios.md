### MQ与发送链路测试场景

#### 一、业务方有序/无序路由

场景1：有序业务方A（order-center）
- 输入：businessOwner=order-center，bizId=ORDER-1001，连续发送“加购成功/下单成功/发货成功/收货成功”4条消息。
- 预期：4条消息进入有序topic；orderKey一致；消费顺序与发送顺序一致。

场景2：有序业务方B（pay-center）
- 输入：businessOwner=pay-center，bizId=PAY-2001，3条支付流程消息。
- 预期：进入有序topic；顺序保持。

场景3：无序业务方C（notice-center）
- 输入：businessOwner=notice-center，bizId=NOTICE-3001，批量消息。
- 预期：进入普通topic；不强制顺序；吞吐优先。

场景4：无序业务方D（growth-center）
- 输入：businessOwner=growth-center，bizId=GROWTH-4001，批量消息。
- 预期：进入普通topic；不受ORDERLY消费影响。

#### 二、跨渠道有序验证

场景5：同一业务链路跨渠道（短信+邮件）
- 输入：同businessOwner=order-center，同bizId=ORDER-5001，消息1发短信，消息2发短信+邮件，消息3发邮件。
- 预期：消息系统内仍按同一链路顺序投递；顺序键稳定；不同渠道不打乱链路顺序。

#### 三、重试与DLQ

场景6：发送侧重试
- 输入：模拟发送MQ前2次失败，第3次成功。
- 预期：发送动作重试3次后成功；请求最终成功。

场景7：消费重试后成功
- 输入：消费端前2次抛异常，第3次成功。
- 预期：MQ重试触发；不落DLQ记录。

场景8：消费重试耗尽进入DLQ记录
- 输入：消费端持续异常，超过maxReconsumeTimes。
- 预期：生成DLQ记录（messageId、payload、失败原因、重试次数）；可在DLQ接口查询到。

场景9：DLQ人工补偿
- 输入：对DLQ记录调用补偿接口。
- 预期：消息重投到原topic；状态从PENDING变更为COMPENSATED。

场景10：DLQ内部告警
- 输入：消费端持续失败达到阈值，开启DLQ内部告警（邮件）。
- 预期：系统向运维/值班邮件组发送告警，提示人工处理，不回调业务方。

#### 四、幂等

场景11：同幂等键重复请求
- 输入：同idempotencyKey重复调用/send。
- 预期：仅首次执行；后续直接返回缓存结果，不重复发送。

场景12：并发相同幂等键
- 输入：两个并发请求使用相同idempotencyKey。
- 预期：一个请求执行业务，另一个返回“处理中”。

#### 五、大消息与边界

场景13：消息体超限
- 输入：构造超出max-size-bytes的消息体。
- 预期：请求被拒绝，返回MESSAGE_PAYLOAD_TOO_LARGE，不入MQ。

场景14：边界值消息体
- 输入：消息体接近阈值（阈值-1B，阈值，阈值+1B）。
- 预期：前两者按配置判定；超过阈值必拒绝。

#### 六、运行建议

- 先执行单元测试：覆盖路由、重试、DLQ记录、补偿、幂等。
- 再做联调测试：接入真实RocketMQ，验证原生DLQ和应用侧DLQ记录一致性。
- 线上压测建议：无序业务高并发压测与有序业务串行延迟分开测。

#### 七、场景与测试方法映射

S1: `SendMqActionTest#shouldRouteToOrderlyTopicForConfiguredOwner`
S2: `SendMqActionTest#shouldUseCustomOrderKeyWhenProvided`
S3: `SendMqActionTest#shouldRouteToCommonTopicForUnorderedOwner`
S4: `SendMqActionTest#shouldRouteToCommonTopicForUnorderedOwner`
S5: `SendMqActionTest#shouldKeepStableOrderKeyAcrossChannelsForSameBiz`
S6: `SendMqActionTest#shouldRetryWhenSendFails`
S7: `RocketMqConsumeServiceTest#shouldConsumeNormallyWhenNoException`
S7: `RocketMqConsumeServiceTest#shouldSucceedAfterRetriesWithoutDlqRecord`
S8: `RocketMqConsumeServiceTest#shouldRecordDlqWhenRetryExhausted`
S8: `RocketMqConsumeServiceTest#shouldNotRecordDlqBeforeRetryThreshold`
S9: `DlqCompensationServiceTest#shouldCompensateSuccessfully`
S9: `DlqCompensationServiceTest#shouldReturnNotFoundWhenNoDlqRecord`
S9: `DlqCompensationServiceTest#shouldListRecordsByIndex`
S10: `RocketMqConsumeServiceTest#shouldAlertOpsWhenRetryExhausted`
S11: `RequestIdempotencyServiceTest#shouldReturnCachedResponseWhenResultExists`
S11: `RequestIdempotencyServiceTest#shouldCacheResultOnSuccess`
S11: `RequestIdempotencyServiceTest#shouldReleaseLockOnFail`
S12: `RequestIdempotencyServiceTest#shouldReturnInProgressWhenLockExists`
S12: `RequestIdempotencyServiceTest#shouldReturnInProgressForSecondConcurrentRequest`
S13: `SendMqActionTest#shouldFailWhenPayloadTooLarge`
S14: `SendMqActionTest#shouldAllowWhenPayloadSizeEqualsThreshold`

#### 八、真实MQ集成测试（生产链路模拟）

目标：
- 真实连接RocketMQ，通过发送端 + ORDERLY消费者验证场景1的“同业务链路有序”。

测试类：
- `RocketMqOrderlyIntegrationTest#shouldConsumeInOrderForScenarioOne`

测试位置：
- `push-web/src/test/java/com/mhd/push/web/mq/integration/RocketMqOrderlyIntegrationTest.java`

执行命令（本地RocketMQ默认127.0.0.1:9876）：
- `mvn -pl push-web -am "-Dtest=RocketMqOrderlyIntegrationTest" test`

执行命令（自定义NameServer）：
- `mvn -pl push-web -am "-Dtest=RocketMqOrderlyIntegrationTest" "-Dit.rocketmq.nameserver=192.168.1.10:9876" test`

验证点：
- 使用同一orderKey（`order-center:ORDER-1001`）连续发送4条业务事件。
- 消费端按ORDERLY模式消费，接收顺序必须与发送顺序完全一致。
- NameServer不可达时该测试自动跳过，避免CI环境误报。

#### 九、在线服务接口黑盒联调（推荐本地生产态）

当服务已经启动并连接真实MQ时，优先使用接口黑盒脚本批量验证多场景：
- 文档：`doc/mq-api-blackbox-test.md`
- 脚本：`scripts/python_scenarios/run_all_api_scenarios.py`

示例命令：
- `python scripts/python_scenarios/run_all_api_scenarios.py --template-id 910001 --send-account 1 --receiver ops@example.com`
- `python scripts/python_scenarios/run_all_api_scenarios.py --template-id 910001 --send-account 1 --receiver ops@example.com --include-mq-failure`
