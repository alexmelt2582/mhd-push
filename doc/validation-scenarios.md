# 验证场景说明

下面这些脚本都直接调用当前项目真实接口，统一遵循同一套参数模型：
1. 默认自动创建临时账号和临时模板。
2. 发送后轮询链路日志。
3. 场景完成后自动清理临时资源。
4. S9 例外，它只验证现有 DLQ 记录的人工补偿入口。

## 通用输入约定

### 1. 账号配置输入

1. `--account-config`：统一支持三种来源：
	- 直接传 JSON 字符串
	- `file:路径`
	- `base64:内容`
2. `--account-config-override`：覆盖局部字段，格式 `key=value`，支持 `a.b.c=value`。

邮箱场景的最简方式：

```powershell
python scripts/python_scenarios/scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json
```

跨 PowerShell、cmd、Linux 都推荐优先使用 `file:`，不要把大段 JSON 直接塞到命令行里。

如果确实要内联传递，推荐 `base64:`：

```powershell
python scripts/python_scenarios/scenario_s1_ordered.py --receiver 2@qq.com --account-config base64:eyJob3N0Ijoic210cC5uZXVzb2Z0LmNvbSIsInBvcnQiOjU4NywidXNlciI6InpoYW8taGFvZG9uZyIsInBhc3MiOiJaNCxkLjNbaiEiLCJmcm9tIjoiemhhby1oYW9kb25nQG5ldXNvZnQuY29tIiwic3RhcnR0bHNFbmFibGUiOmZhbHNlLCJhdXRoIjp0cnVlLCJzc2xFbmFibGUiOnRydWV9
```

### 2. 模板内容输入

1. `--template-content`：统一支持 JSON 字符串、`file:路径`、`base64:内容`。
2. `--template-content-override`：覆盖模板字段，格式 `key=value`，支持 `a.b.c=value`。

例如覆盖 S1 模板标题：

```powershell
python scripts/python_scenarios/scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json --template-content-override title='S1 custom title'
```

### 3. 通用元信息输入

如需切换渠道或模板元信息，可统一传：
1. `--send-channel`
2. `--id-type`
3. `--template-type`
4. `--msg-type`
5. `--shield-type`

## 场景清单

### 1. S1 有序业务链路

脚本：scripts/python_scenarios/scenario_s1_ordered.py

验证目标：
1. 同一业务链路内连续事件是否都能正常受理。
2. 每次发送后是否能查到对应链路记录。

### 2. S3/S4 无序业务方验证

脚本：scripts/python_scenarios/scenario_s3_unordered.py

验证目标：
1. 非有序业务方是否可以正常受理。
2. 不同业务方的链路记录是否正常写入。

### 3. S11 接口幂等验证

脚本：scripts/python_scenarios/scenario_s11_idempotency.py

验证目标：
1. 同一个 `idempotencyKey` 的重复请求是否被系统正确识别。
2. 首次发送成功后是否仍能查询链路。

### 4. S12 模板缓存与真实发送链路验证

脚本：scripts/python_scenarios/scenario_s12_template_cache.py

验证目标：
1. 模板更新后第一次发送的冷路径受理延迟。
2. 同一模板连续发送后的热路径受理延迟。
3. 模板正文变大后，当前项目真实接口的受理时延变化。

运行示例：

```powershell
python scripts/python_scenarios/scenario_s12_template_cache.py --receiver 2@qq.com --account-config file:./account-email.json --hot-runs 20 --body-sizes 256,4096
```

观察重点：
1. `cold_ms`：模板更新后的首次发送受理耗时。
2. `hot_avg_ms`：缓存预热后的平均受理耗时。
3. `hot_p95_ms`：缓存预热后高位延迟。

### 5. S13 大消息边界验证

脚本：scripts/python_scenarios/scenario_s13_payload.py

验证目标：
1. 接近阈值的请求是否还能正常受理。
2. 超限请求是否被系统拒绝。

### 6. S14 热点渠道压测

脚本：scripts/python_scenarios/scenario_s14_hotspot_acceptance.py

验证目标：
1. 热点 burst 下 `/send` 是否还能快速受理。
2. 并发压测下接口平均延迟、p95 延迟和成功率。
3. 输出抽样 `messageId`，便于再配合 `/trace/message` 继续追踪。

运行示例：

```powershell
python scripts/python_scenarios/scenario_s14_hotspot_acceptance.py --receiver 2@qq.com --account-config file:./account-email.json --requests 100 --concurrency 20
```

观察重点：
1. `success`：接口受理成功率。
2. `avg_ms` / `p95_ms`：热点场景下接口受理延迟。
3. `sample_message_ids`：用于继续调用 `/trace/message` 观察后续链路。

### 7. S6 MQ 失败注入

脚本：scripts/python_scenarios/scenario_s6_mq_failure.py

验证目标：
1. MQ 停止时 `/send` 是否按预期失败。
2. MQ 恢复后发送链路是否恢复正常。

### 8. S9 DLQ 人工补偿

脚本：scripts/python_scenarios/scenario_s9_dlq_compensate.py

验证目标：
1. 查询 DLQ 列表。
2. 获取最新一条记录明细。
3. 调用补偿接口重新投递。

## 敏感词刷新一致性验证

这一块当前没有单独的热刷新管理接口，所以更适合用“配置变更 + 实例日志 + Redis key 观察”来验证，而不是写成脱离服务的纯 Python 模拟。

建议验证方式：
1. 修改敏感词文件，等待定时刷新。
2. 观察服务日志里是否仍然复用相同 version，还是仅在内容变化时切新 version。
3. 用 Redis 客户端检查 `SENSITIVE_WORD_DICT_CURRENT_VERSION` 和对应集合 TTL 是否符合预期。