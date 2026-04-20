#!/usr/bin/env python3
"""
场景 S1: 有序业务链路验证。

这个脚本模拟真实调用方，针对 order-center 业务方连续发送四条事件：
1. 加购成功
2. 下单成功
3. 发货成功
4. 收货成功

脚本行为：
1. 先检查模板是否存在。
2. 若模板不存在，则自动调用模板接口创建测试模板。
3. 使用真实 /send 接口逐条发送消息。
4. 校验每次响应是否为成功。

注意：
1. 本脚本只验证“接口可正常受理”。
2. 真正的顺序消费结果，需要结合消费者日志或 RocketMQ 侧进一步观察。
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
import uuid


def http_get_json(url: str) -> dict:
    """发送 GET 请求并返回 JSON。"""
    request = urllib.request.Request(url=url, method="GET")
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def http_post_json(url: str, payload: dict) -> dict:
    """发送 POST JSON 请求并返回 JSON。"""
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url=url,
        data=body,
        method="POST",
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def build_template_content() -> str:
    """构造邮箱模板内容，变量名必须与发送时的 variables 对齐。"""
    return json.dumps(
        {
            "title": "S1 ordered notification",
            "content": "event={$event}; bizId={$bizId}; timestamp={$ts}",
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )


def ensure_template_exists(base_url: str, template_id: int, send_account: int) -> None:
    """检查模板是否存在；不存在则自动创建。"""
    query_url = f"{base_url}/messageTemplate/query/{template_id}"
    try:
        result = http_get_json(query_url)
        data = result.get("data")
        if result.get("code") == "200" and data and int(data.get("id", 0)) == template_id:
            print(f"[S1] 模板已存在: {template_id}")
            return
    except Exception:
        pass

    create_payload = {
        "id": template_id,
        "name": f"IT-S1-{template_id}",
        "idType": 50,
        "sendChannel": 40,
        "templateType": 20,
        "msgType": 10,
        "shieldType": 10,
        "sendAccount": send_account,
        "msgContent": build_template_content(),
    }
    result = http_post_json(f"{base_url}/messageTemplate/add", create_payload)
    if result.get("code") != "200":
        raise RuntimeError(f"[S1] 模板创建失败: {result}")
    print(f"[S1] 模板已创建: {template_id}")


def send_message(base_url: str, template_id: int, receiver: str, biz_id: str, event: str) -> dict:
    """调用 /send 接口发送单条消息。"""
    payload = {
        "code": "send",
        "messageTemplateId": template_id,
        "idempotencyKey": f"S1-{uuid.uuid4().hex}",
        "messageParam": {
            "bizId": biz_id,
            "receiver": receiver,
            "variables": {
                "event": event,
                "bizId": biz_id,
                "ts": str(int(time.time() * 1000)),
            },
            "extra": {
                "businessOwner": "order-center",
            },
        },
    }
    return http_post_json(f"{base_url}/send", payload)


def assert_success(response: dict, step_name: str) -> None:
    """断言接口返回成功。"""
    code = str(response.get("code"))
    if code != "200":
        raise RuntimeError(f"[{step_name}] 返回异常: {json.dumps(response, ensure_ascii=False)}")


def main() -> int:
    parser = argparse.ArgumentParser(description="S1 有序业务链路接口测试脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080", help="服务地址")
    parser.add_argument("--template-id", required=True, type=int, help="测试模板ID")
    parser.add_argument("--send-account", required=True, type=int, help="发送账号ID")
    parser.add_argument("--receiver", required=True, help="接收者，建议测试邮箱")
    args = parser.parse_args()

    ensure_template_exists(args.base_url, args.template_id, args.send_account)

    biz_id = f"ORDER-1001-{int(time.time() * 1000)}"
    events = ["cart-success", "order-success", "delivery-success", "receive-success"]

    for event in events:
        response = send_message(args.base_url, args.template_id, args.receiver, biz_id, event)
        assert_success(response, f"S1 {event}")
        print(f"[S1] {event} 发送成功: {json.dumps(response, ensure_ascii=False)}")

    print("[S1] 场景执行完成")
    return 0


if __name__ == "__main__":
    # demo：python scenario_s1_ordered.py --template-id 2 --send-account 2 --receiver "2@qq.com"
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S1] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S1] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
