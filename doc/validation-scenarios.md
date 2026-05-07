# 验证场景说明

下面所有脚本都直接调用当前项目真实接口，模拟的是线上调用方实际会做的三件事：
1. 先创建临时发送账号。
2. 再创建临时模板。
3. 再调用真实的 /send、/trace、/mq/dlq 等接口验证行为。

统一约定：
1. messageId 是唯一的全链路追踪 ID，后续所有校验都优先用它。
2. bizId 只是业务关联键，例如订单号、账单号、活动批次号，不作为统一追踪主键。
3. 每个脚本都自带默认账号 JSON 和默认模板 JSON，也允许调用方自行覆盖。
4. 每个脚本相互隔离，都会创建独立账号、独立模板、独立业务数据。
5. S9 是运维脚本，不负责造数据，只操作现有 DLQ 记录。

## 通用输入约定

### 1. 账号配置输入

1. --account-config：支持三种来源。
2. 直接传 JSON 字符串。
3. file:路径。
4. base64:内容。
5. --account-config-override：覆盖局部字段，格式 key=value，支持 a.b.c=value。

邮箱场景最简示例：

```powershell
python scripts/python_scenarios/scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json
```

飞书机器人示例：

```powershell
python scripts/python_scenarios/scenario_s3_unordered.py --receiver open_id_1,open_id_2 --send-channel 24 --id-type 10 --account-config '{"webhook":"https://open.feishu.cn/open-apis/bot/v2/hook/replace-me"}'
```

### 2. 模板内容输入

1. --template-content：支持 JSON 字符串、file:路径、base64:内容。
2. --template-content-override：覆盖模板字段，格式 key=value，支持 a.b.c=value。
3. 不传时，每个脚本都会使用自己内置的一份真实业务模板 JSON。

例如覆盖 S1 标题：

```powershell
python scripts/python_scenarios/scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json --template-content-override title='订单节点通知 - 自定义标题'
```

### 3. 通用元信息输入

如需切换渠道或模板元信息，可统一传：
1. --send-channel
2. --id-type
3. --template-type
4. --msg-type
5. --shield-type

## 场景一：S1 有序业务链路

脚本：scripts/python_scenarios/scenario_s1_ordered.py

要测试的场景：
1. 同一个订单链路内，购物车锁单、支付成功、仓库发货、用户签收四个事件能否按同一 orderKey 投递。
2. 每次发送是否都返回独立 messageId。
3. 每个 messageId 是否都能查到自己的链路日志。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s1_ordered.py --receiver buyer1@example.com,buyer2@example.com --account-config file:./account-email.json
```

执行之后怎么验证：
1. 脚本输出 4 次发送响应，每次响应都必须 code=200。
2. 每次响应都应返回新的 messageId。
3. 脚本会自动调用 /trace/message 轮询，每个 messageId 都应该拿到非空 items。
4. 如需人工复核，拿脚本打印出的 messageId 调用 /trace/message，再核对 detail 中是否能看到对应节点。

## 场景二：S3/S4 无序业务方验证

脚本：scripts/python_scenarios/scenario_s3_unordered.py

要测试的场景：
1. 两个不同业务方 notice-center、growth-center 的消息能否独立受理。
2. 无序业务方不会被错误地强制走同一顺序链路。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s3_unordered.py --receiver 2@qq.com --account-config file:./account-email.json
```

执行之后怎么验证：
1. 两次发送都必须 code=200。
2. 两次响应都必须返回自己的 messageId。
3. 每个 messageId 的 /trace/message 结果都必须非空。
4. 两个业务方的 bizId 不同，messageId 也不同，互不干扰。

## 场景三：S11 接口幂等验证

脚本：scripts/python_scenarios/scenario_s11_idempotency.py

要测试的场景：
1. 同一个 idempotencyKey、同一个 bizId、同一个变量集重复提交两次。
2. 系统是否只受理一次真实发送。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s11_idempotency.py --receiver 2@qq.com --account-config file:./account-email.json
```

执行之后怎么验证：
1. 第一次请求必须成功。
2. 第二次请求允许返回命中缓存、处理中或幂等已存在，但不能再次触发一条新的真实发送。
3. 该脚本两次请求使用的是完全相同的 payload，不再出现“随机变量不同导致幂等失真”的问题。
4. 用第一次返回的 messageId 调 /trace/message，必须能查到链路。

## 场景四：S12 模板缓存与真实发送链路验证

脚本：scripts/python_scenarios/scenario_s12_template_cache.py

要测试的场景：
1. 模板更新后的第一次发送冷路径耗时。
2. 同一模板在相同 bizId 下连续发送的热路径耗时。
3. 正文规模变化后，接口受理延迟如何变化。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s12_template_cache.py --receiver 2@qq.com --account-config file:./account-email.json --hot-runs 20 --body-sizes 256,4096
```

执行之后怎么验证：
1. 观察脚本输出的 cold_ms、hot_avg_ms、hot_p95_ms。
2. 同一个 body_size 下，冷路径一般高于热路径。
3. 脚本使用固定 bizId 进行冷/热对比，结果更容易复现。
4. 脚本会对最后一个 messageId 自动查 /trace/message，确认链路正常。

## 场景五：S13 大消息边界验证

脚本：scripts/python_scenarios/scenario_s13_payload.py

要测试的场景：
1. 接近限制阈值的正文是否还能正常受理。
2. 超过阈值的正文是否被系统拒绝。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s13_payload.py --receiver 2@qq.com --account-config file:./account-email.json --large-payload-bytes 3300000
```

执行之后怎么验证：
1. near-limit 请求应返回成功。
2. over-limit 请求应返回 A0006 或等价的超限错误。
3. near-limit 返回的 messageId 应能查到 /trace/message。

## 场景六：S14 热点渠道压测

脚本：scripts/python_scenarios/scenario_s14_hotspot_acceptance.py

要测试的场景：
1. 热点 burst 下 /send 的受理能力。
2. 并发情况下接口平均延迟、p95 延迟和成功率。
3. 抽样 messageId 后继续查链路。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s14_hotspot_acceptance.py --receiver 2@qq.com --account-config file:./account-email.json --requests 100 --concurrency 20
```

执行之后怎么验证：
1. 关注 success、avg_ms、p95_ms、max_ms。
2. success 越接近 100%，说明热点下受理越稳定。
3. sample_message_ids 可继续调用 /trace/message 做抽样复核。

## 场景七：S15 多层次 QPS 基准

脚本：scripts/python_scenarios/scenario_s15_qps_benchmark.py

要测试的场景：
1. ingress 模式下入口受理能力。
2. unordered 模式下无序吞吐能力。
3. ordered 模式下带顺序键时的受理能力。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s15_qps_benchmark.py --receiver 2@qq.com --account-config file:./account-email.json --mode ingress --requests 200 --concurrency 40
python scripts/python_scenarios/scenario_s15_qps_benchmark.py --receiver 2@qq.com --account-config file:./account-email.json --mode unordered --requests 200 --concurrency 40
python scripts/python_scenarios/scenario_s15_qps_benchmark.py --receiver 2@qq.com --account-config file:./account-email.json --mode ordered --requests 200 --concurrency 20
```

执行之后怎么验证：
1. 关注 qps、avg_ms、p95_ms、p99_ms、max_ms。
2. ordered 模式通常吞吐会低于 unordered，这是正常现象。
3. 抽样 messageId 继续查 /trace/message，确认在高压下链路仍可追踪。

## 场景八：S6 MQ 失败注入

脚本：scripts/python_scenarios/scenario_s6_mq_failure.py

要测试的场景：
1. RocketMQ 停机时，/send 是否按预期返回失败。
2. RocketMQ 恢复后，同一个 bizId 是否可以重新受理。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s6_mq_failure.py --receiver 2@qq.com --account-config file:./account-email.json --rocketmq-script-dir ./doc/rocketmq
```

执行之后怎么验证：
1. MQ 停止阶段的请求必须失败，不能返回假成功。
2. MQ 恢复后，使用同一个 bizId 的恢复请求必须成功。
3. 恢复请求返回的 messageId 应能查到 /trace/message。

## 场景九：S9 DLQ 人工补偿

脚本：scripts/python_scenarios/scenario_s9_dlq_compensate.py

要测试的场景：
1. 查询当前 DLQ 列表。
2. 查看最新一条 DLQ 记录明细。
3. 手工触发补偿。

怎么执行：

```powershell
python scripts/python_scenarios/scenario_s9_dlq_compensate.py --base-url http://127.0.0.1:8080
```

执行之后怎么验证：
1. /mq/dlq/list 能返回数据。
2. 能查询到最新一条 DLQ 的详情。
3. 补偿接口调用成功后，观察 DLQ 状态变化及后续链路。

## 敏感词刷新一致性验证

这一块更适合通过配置变更和运行态观察来验证，而不是脱离服务的纯 Python 模拟。

建议做法：
1. 修改敏感词文件，等待定时刷新。
2. 观察服务日志中 version 是否只在内容变化时切换。
3. 用 Redis 客户端检查 SENSITIVE_WORD_DICT_CURRENT_VERSION 和对应集合 TTL 是否符合预期。