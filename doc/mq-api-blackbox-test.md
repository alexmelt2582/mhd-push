### 接口黑盒联调（本地运行中的生产项目）

这套方案直接通过 HTTP 调用当前运行服务，验证“真实接口 -> MQ发送 -> 消费侧处理 -> DLQ人工补偿”链路中的关键行为。

#### 1. 前置条件

1. 服务已启动并可访问（默认 `http://127.0.0.1:8080`）。
2. RocketMQ 正常可用（NameServer/Broker 已启动）。
3. 数据库中存在可用的 `messageTemplateId`，且模板类型与 `receiver` 匹配。
4. 已存在本地 Python 脚本：
   1. [scripts/python_scenarios/run_all_api_scenarios.py](scripts/python_scenarios/run_all_api_scenarios.py)
   2. [scripts/python_scenarios/scenario_s1_ordered.py](scripts/python_scenarios/scenario_s1_ordered.py)
   3. [scripts/python_scenarios/scenario_s3_unordered.py](scripts/python_scenarios/scenario_s3_unordered.py)
   4. [scripts/python_scenarios/scenario_s11_idempotency.py](scripts/python_scenarios/scenario_s11_idempotency.py)
   5. [scripts/python_scenarios/scenario_s13_payload.py](scripts/python_scenarios/scenario_s13_payload.py)
   6. [scripts/python_scenarios/scenario_s9_dlq_compensate.py](scripts/python_scenarios/scenario_s9_dlq_compensate.py)
   7. [scripts/python_scenarios/scenario_s6_mq_failure.py](scripts/python_scenarios/scenario_s6_mq_failure.py)

说明：
1. 每个 Python 脚本都是完全自包含的，不依赖公共脚本文件。
2. 每个脚本文件头部都包含场景说明、执行步骤和关键注意事项。

#### 2. 一次性执行多场景

```bash
python scripts/python_scenarios/run_all_api_scenarios.py --template-id 910001 --send-account 1 --receiver ops@example.com
```

可选参数：
1. `--base-url`：服务地址，默认 `http://127.0.0.1:8080`。
2. `--template-id`：场景使用的模板ID；脚本会先检查模板，若不存在则自动调用 `/messageTemplate/add` 创建。
3. `--send-account`：模板发送账号ID，必填。
4. `--receiver`：接收者（建议邮箱，如 `ops@example.com`）。
5. `--large-payload-bytes`：大消息构造大小，默认 3300000。
6. `--include-mq-failure`：启用“停Broker制造发送失败”场景。
7. `--rocketmq-script-dir`：RocketMQ启停脚本目录，默认 `./doc/rocketmq`。

#### 3. 覆盖的业务场景

1. S1 有序业务方 `order-center`：同链路4事件连续调用。
2. S2 有序业务方 `pay-center`：支付链路事件。
3. S3/S4 无序业务方：`notice-center`、`growth-center`。
4. S5 显式 `orderKey` 场景：验证同链路稳定键传递。
5. S11 重复幂等键：同键重复调用 `/send`。
6. S12 并发幂等键：两个并发请求竞争同一个幂等键。
7. S13/S14 大消息边界：近阈值与超阈值。
8. S8/S9/S10 DLQ接口：`/mq/dlq/list`、`/mq/dlq/{messageId}`、`/mq/dlq/compensate/{messageId}`。

#### 4.1 单场景执行（每个场景一个脚本）

```bash
python scripts/python_scenarios/scenario_s1_ordered.py --template-id 910001 --send-account 1 --receiver ops@example.com
python scripts/python_scenarios/scenario_s3_unordered.py --template-id 910001 --send-account 1 --receiver ops@example.com
python scripts/python_scenarios/scenario_s11_idempotency.py --template-id 910001 --send-account 1 --receiver ops@example.com
python scripts/python_scenarios/scenario_s13_payload.py --template-id 910001 --send-account 1 --receiver ops@example.com
python scripts/python_scenarios/scenario_s6_mq_failure.py --template-id 910001 --send-account 1 --receiver ops@example.com --rocketmq-script-dir ./doc/rocketmq
python scripts/python_scenarios/scenario_s9_dlq_compensate.py
```

#### 4. 手动制造 MQ 发送失败（触发 S6）

直接在脚本里启用注入开关：

```bash
python scripts/python_scenarios/run_all_api_scenarios.py --template-id 910001 --send-account 1 --receiver ops@example.com --include-mq-failure
```

脚本行为：
1. 调用 `doc/rocketmq/stop-rocketmq.bat` 停止 MQ。
2. 立即调用 `/send`，预期返回发送失败（通常是 `B0001`）。
3. 调用 `doc/rocketmq/start-rocketmq.bat` 恢复 MQ。
4. 再次调用 `/send`，验证恢复后可正常发送。

#### 4.2 关键接口与参数（已按源码实现）

1. 发送接口：`POST /send`
   1. 请求体字段：`code`、`messageTemplateId`、`idempotencyKey`、`messageParam.bizId`、`messageParam.receiver`、`messageParam.variables`、`messageParam.extra`。
   2. 业务扩展字段：`messageParam.extra.businessOwner`、`messageParam.extra.orderKey`。
2. 模板接口：
   1. `GET /messageTemplate/query/{id}`：检查模板是否存在。
   2. `POST /messageTemplate/add`：模板不存在时自动创建。
3. DLQ人工补偿接口：
   1. `GET /mq/dlq/list`
   2. `GET /mq/dlq/{messageId}`
   3. `POST /mq/dlq/compensate/{messageId}`

#### 5. 结果判读建议

1. 发送成功通常为 `code=200`。
2. 大消息超限预期为 `A0006`。
3. 并发幂等中，通常一个请求成功，另一个为“处理中”(`A0008`)或命中重复。
4. DLQ列表为空时，先通过消费侧故障场景制造失败记录再重试查询。

#### 6. 生产态演练注意点

1. 建议先在低峰期演练 `-InjectMqFailure`，避免影响业务线程堆积。
2. 失败注入后观察日志与DLQ增长，再执行补偿接口闭环验证。
3. 注入场景结束后，确认 RocketMQ 进程已恢复，避免后续压测误差。
