#!/usr/bin/env python3
"""
场景 S6: MQ 发送失败注入。

这个脚本通过“停掉 RocketMQ -> 调用 /send -> 再启动 RocketMQ -> 再次调用 /send”
来模拟真实生产中的 MQ 不可用故障。

脚本行为：
1. 检查模板，不存在则自动创建。
2. 调用 stop-rocketmq.bat 停止 MQ。
3. 调用 /send，预期返回发送失败。
4. 调用 start-rocketmq.bat 启动 MQ。
5. 再次调用 /send，预期恢复成功。

注意：
1. 这个脚本会真实影响你当前本机 RocketMQ 进程。
2. 只建议在测试环境或你明确知道影响范围时执行。
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path


def http_get_json(url: str) -> dict:
    with urllib.request.urlopen(urllib.request.Request(url=url, method="GET"), timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def http_post_json(url: str, payload: dict) -> dict:
    request = urllib.request.Request(
        url=url,
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def ensure_template_exists(base_url: str, template_id: int, send_account: int) -> None:
    try:
        result = http_get_json(f"{base_url}/messageTemplate/query/{template_id}")
        if result.get("code") == "200" and result.get("data"):
            print(f"[S6] 模板已存在: {template_id}")
            return
    except Exception:
        pass

    payload = {
        "id": template_id,
        "name": f"IT-S6-{template_id}",
        "idType": 50,
        "sendChannel": 40,
        "templateType": 20,
        "msgType": 10,
        "shieldType": 10,
        "sendAccount": send_account,
        "msgContent": json.dumps(
            {
                "title": "S6 mq failure notification",
                "content": "event={$event}; bizId={$bizId}; timestamp={$ts}",
            },
            ensure_ascii=False,
            separators=(",", ":"),
        ),
    }
    result = http_post_json(f"{base_url}/messageTemplate/add", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"[S6] 模板创建失败: {result}")
    print(f"[S6] 模板已创建: {template_id}")


def send_message(base_url: str, template_id: int, receiver: str, biz_id: str, label: str) -> dict:
    payload = {
        "code": "send",
        "messageTemplateId": template_id,
        "idempotencyKey": f"S6-{label}-{uuid.uuid4().hex}",
        "messageParam": {
            "bizId": biz_id,
            "receiver": receiver,
            "variables": {
                "event": label,
                "bizId": biz_id,
                "ts": str(int(time.time() * 1000)),
            },
            "extra": {
                "businessOwner": "order-center",
            },
        },
    }
    return http_post_json(f"{base_url}/send", payload)


def run_batch_script(script_path: Path) -> None:
    if not script_path.exists():
        raise FileNotFoundError(f"脚本不存在: {script_path}")
    subprocess.run([str(script_path)], check=True, shell=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="S6 MQ 失败注入脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--template-id", required=True, type=int)
    parser.add_argument("--send-account", required=True, type=int)
    parser.add_argument("--receiver", required=True)
    parser.add_argument("--rocketmq-script-dir", default="./doc/rocketmq")
    args = parser.parse_args()

    ensure_template_exists(args.base_url, args.template_id, args.send_account)

    script_dir = Path(args.rocketmq_script_dir)
    stop_script = script_dir / "stop-rocketmq.bat"
    start_script = script_dir / "start-rocketmq.bat"

    biz_id = f"MQFAIL-{int(time.time() * 1000)}"

    print("[S6] 停止 RocketMQ，开始故障注入")
    run_batch_script(stop_script)
    time.sleep(4)

    fail_result = send_message(args.base_url, args.template_id, args.receiver, biz_id, "mq-down")
    fail_code = str(fail_result.get("code"))
    if fail_code not in {"B0001", "-1", "500"}:
        raise RuntimeError(f"[S6] MQ关闭时返回不符合预期: {json.dumps(fail_result, ensure_ascii=False)}")
    print(f"[S6] MQ关闭时响应: {json.dumps(fail_result, ensure_ascii=False)}")

    print("[S6] 启动 RocketMQ，等待恢复")
    run_batch_script(start_script)
    time.sleep(8)

    recover_result = send_message(args.base_url, args.template_id, args.receiver, biz_id + "-recover", "mq-up")
    if str(recover_result.get("code")) != "200":
        raise RuntimeError(f"[S6] MQ恢复后发送失败: {json.dumps(recover_result, ensure_ascii=False)}")
    print(f"[S6] MQ恢复后响应: {json.dumps(recover_result, ensure_ascii=False)}")

    print("[S6] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S6] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S6] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
