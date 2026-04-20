#!/usr/bin/env python3
"""
场景 S11: 接口幂等验证。

这个脚本会使用同一个 idempotencyKey 连续调用两次 /send：
1. 第一次请求应正常成功。
2. 第二次请求应命中缓存、处理中状态或重复请求状态。

脚本行为：
1. 检查模板是否存在，不存在则自动创建。
2. 构造相同幂等键的两次请求。
3. 输出两次调用结果，便于人工判断幂等是否生效。
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
    request = urllib.request.Request(url=url, method="GET")
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def http_post_json(url: str, payload: dict) -> dict:
    request = urllib.request.Request(
        url=url,
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def ensure_template_exists(base_url: str, template_id: int, send_account: int) -> None:
    try:
        result = http_get_json(f"{base_url}/messageTemplate/query/{template_id}")
        if result.get("code") == "200" and result.get("data"):
            print(f"[S11] 模板已存在: {template_id}")
            return
    except Exception:
        pass

    payload = {
        "id": template_id,
        "name": f"IT-S11-{template_id}",
        "idType": 50,
        "sendChannel": 40,
        "templateType": 20,
        "msgType": 10,
        "shieldType": 10,
        "sendAccount": send_account,
        "msgContent": json.dumps(
            {
                "title": "S11 idempotency notification",
                "content": "event={$event}; bizId={$bizId}; timestamp={$ts}",
            },
            ensure_ascii=False,
            separators=(",", ":"),
        ),
    }
    result = http_post_json(f"{base_url}/messageTemplate/add", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"[S11] 模板创建失败: {result}")
    print(f"[S11] 模板已创建: {template_id}")


def send_with_same_key(base_url: str, template_id: int, receiver: str, idempotency_key: str, biz_id: str) -> dict:
    payload = {
        "code": "send",
        "messageTemplateId": template_id,
        "idempotencyKey": idempotency_key,
        "messageParam": {
            "bizId": biz_id,
            "receiver": receiver,
            "variables": {
                "event": "idempotency-check",
                "bizId": biz_id,
                "ts": str(int(time.time() * 1000)),
            },
            "extra": {
                "businessOwner": "notice-center",
            },
        },
    }
    return http_post_json(f"{base_url}/send", payload)


def main() -> int:
    parser = argparse.ArgumentParser(description="S11 幂等验证脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--template-id", required=True, type=int)
    parser.add_argument("--send-account", required=True, type=int)
    parser.add_argument("--receiver", required=True)
    args = parser.parse_args()

    ensure_template_exists(args.base_url, args.template_id, args.send_account)

    biz_id = f"IDEMP-{int(time.time() * 1000)}"
    idempotency_key = f"S11-{uuid.uuid4().hex}"

    first = send_with_same_key(args.base_url, args.template_id, args.receiver, idempotency_key, biz_id)
    second = send_with_same_key(args.base_url, args.template_id, args.receiver, idempotency_key, biz_id)

    if str(first.get("code")) != "200":
        raise RuntimeError(f"[S11] 第一次请求失败: {json.dumps(first, ensure_ascii=False)}")

    second_code = str(second.get("code"))
    if second_code not in {"200", "A0007", "A0008"}:
        raise RuntimeError(f"[S11] 第二次请求结果不符合预期: {json.dumps(second, ensure_ascii=False)}")

    print(f"[S11] 第一次响应: {json.dumps(first, ensure_ascii=False)}")
    print(f"[S11] 第二次响应: {json.dumps(second, ensure_ascii=False)}")
    print("[S11] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S11] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S11] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
