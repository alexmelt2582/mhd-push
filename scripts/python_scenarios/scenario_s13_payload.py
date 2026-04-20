#!/usr/bin/env python3
"""
场景 S13/S14: 大消息边界验证。

这个脚本用于验证：
1. 接近阈值的消息可以正常发送。
2. 超过阈值的消息会被接口拒绝。

脚本行为：
1. 检查模板，不存在则创建。
2. 构造两个请求：一个 near-limit，一个 over-limit。
3. 校验 near-limit 返回 200，over-limit 返回 A0006。
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
            print(f"[S13/S14] 模板已存在: {template_id}")
            return
    except Exception:
        pass

    payload = {
        "id": template_id,
        "name": f"IT-S13S14-{template_id}",
        "idType": 50,
        "sendChannel": 40,
        "templateType": 20,
        "msgType": 10,
        "shieldType": 10,
        "sendAccount": send_account,
        "msgContent": json.dumps(
            {
                "title": "S13S14 payload notification",
                "content": "event={$event}; bizId={$bizId}; blob={$blob}; timestamp={$ts}",
            },
            ensure_ascii=False,
            separators=(",", ":"),
        ),
    }
    result = http_post_json(f"{base_url}/messageTemplate/add", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"[S13/S14] 模板创建失败: {result}")
    print(f"[S13/S14] 模板已创建: {template_id}")


def send_payload(base_url: str, template_id: int, receiver: str, biz_id: str, blob: str, label: str) -> dict:
    payload = {
        "code": "send",
        "messageTemplateId": template_id,
        "idempotencyKey": f"{label}-{uuid.uuid4().hex}",
        "messageParam": {
            "bizId": biz_id,
            "receiver": receiver,
            "variables": {
                "event": label,
                "bizId": biz_id,
                "blob": blob,
                "ts": str(int(time.time() * 1000)),
            },
            "extra": {
                "businessOwner": "order-center",
            },
        },
    }
    return http_post_json(f"{base_url}/send", payload)


def main() -> int:
    parser = argparse.ArgumentParser(description="S13/S14 大消息边界测试脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--template-id", required=True, type=int)
    parser.add_argument("--send-account", required=True, type=int)
    parser.add_argument("--receiver", required=True)
    parser.add_argument("--large-payload-bytes", default=3300000, type=int)
    args = parser.parse_args()

    ensure_template_exists(args.base_url, args.template_id, args.send_account)

    near_blob = "x" * max(1024, args.large_payload_bytes - 2048)
    over_blob = "x" * args.large_payload_bytes
    biz_id = f"PAYLOAD-{int(time.time() * 1000)}"

    near_result = send_payload(args.base_url, args.template_id, args.receiver, f"{biz_id}-near", near_blob, "near")
    over_result = send_payload(args.base_url, args.template_id, args.receiver, f"{biz_id}-over", over_blob, "over")

    if str(near_result.get("code")) != "200":
        raise RuntimeError(f"[S14] near-limit 结果异常: {json.dumps(near_result, ensure_ascii=False)}")
    if str(over_result.get("code")) != "A0006":
        raise RuntimeError(f"[S13] over-limit 结果异常: {json.dumps(over_result, ensure_ascii=False)}")

    print(f"[S14] near-limit 响应: {json.dumps(near_result, ensure_ascii=False)}")
    print(f"[S13] over-limit 响应: {json.dumps(over_result, ensure_ascii=False)}")
    print("[S13/S14] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S13/S14] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S13/S14] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
