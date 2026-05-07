# 模板接口验证脚本

脚本位置：
- scripts/verify_template_examples.py

这套脚本和现有 scripts/python_scenarios 没有依赖关系。

目标：
- 直接读取 scripts/examples/模板 下的模板文件
- 直接读取 scripts/examples/发送实例 下的发送文件
- 按文件名自动配对
- 对每个场景执行 模板查询/新增/更新 -> 获取最终模板 ID -> 发送

## 命名规则

模板文件示例：
- 场景7：服务器宕机告警 (钉钉机器人).json

发送文件示例：
- 场景7：服务器宕机告警 (钉钉机器人)-发送.json

脚本会自动把它们识别成同一个场景。

## 常用命令

执行全部场景：

```powershell
python scripts/verify_template_examples.py --admin-base-url http://127.0.0.1:8081 --public-base-url http://127.0.0.1:8080
```

只执行一个场景：

```powershell
python scripts/verify_template_examples.py --admin-base-url http://127.0.0.1:8081 --public-base-url http://127.0.0.1:8080 --case 场景7
```

只更新模板，不发消息：

```powershell
python scripts/verify_template_examples.py --admin-base-url http://127.0.0.1:8081 --public-base-url http://127.0.0.1:8080 --skip-send
```

只发消息，不更新模板：

```powershell
python scripts/verify_template_examples.py --admin-base-url http://127.0.0.1:8081 --public-base-url http://127.0.0.1:8080 --skip-upsert --case 场景6
```

遇到第一个失败就停止：

```powershell
python scripts/verify_template_examples.py --admin-base-url http://127.0.0.1:8081 --public-base-url http://127.0.0.1:8080 --fail-fast
```

如果 admin 和 public 恰好共用一个地址，也可以继续使用兼容参数：

```powershell
python scripts/verify_template_examples.py --base-url http://127.0.0.1:8080
```

## 替换模板的方式

如果你要替换某个模板：

1. 直接修改 scripts/examples/模板 对应 JSON 文件
2. 如果发送参数也变了，再同步修改 scripts/examples/发送实例 对应 JSON 文件
3. 重新执行脚本

不需要改 Python 代码。

## 说明

- admin 模板接口和 public 发送接口可以使用不同地址
- 模板文件有 id 时：脚本先 query，再决定 update 或 add
- 模板文件没有 id 时：脚本直接 add，并使用 add 响应返回的模板 id 继续查询和发送
- /send 前会把本次确定下来的 templateId 覆盖到发送请求里，避免发送文件中的 templateId 过期