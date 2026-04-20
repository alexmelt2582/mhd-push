#!/usr/bin/env python3
"""
场景 S3/S4: 无序业务方验证。

这个脚本用于验证以下两类业务方走普通发送链路：
1. notice-center
2. growth-center

脚本行为：
1. 检查模板是否存在，不存在则自动创建。
2. 使用真实 /send 接口分别发送两类业务方消息。
3. 校验接口返回成功。

说明：
这个场景主要验证“系统可正常受理非白名单业务方请求”，
并确保测试输入符合当前接口字段结构。
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


def build_template_content() -> str:
    return json.dumps(
        {
            "title": "S3 unordered notification",
            "content": "owner={$owner}; event={$event}; bizId={$bizId}; timestamp={$ts}",
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )


def ensure_template_exists(base_url: str, template_id: int, send_account: int) -> None:
    try:
        result = http_get_json(f"{base_url}/messageTemplate/query/{template_id}")
        if result.get("code") == "200" and result.get("data"):
            print(f"[S3/S4] 模板已存在: {template_id}")
            return
    except Exception:
        pass

    payload = {
        "id": template_id,
        "name": f"IT-S3S4-{template_id}",
        "idType": 50,
        "sendChannel": 40,
        "templateType": 20,
        "msgType": 10,
        "shieldType": 10,
        "sendAccount": send_account,
        "msgContent": build_template_content(),
    }
    result = http_post_json(f"{base_url}/messageTemplate/add", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"[S3/S4] 模板创建失败: {result}")
    print(f"[S3/S4] 模板已创建: {template_id}")


def send_message(base_url: str, template_id: int, receiver: str, owner: str) -> dict:
    biz_id = f"{owner}-{int(time.time() * 1000)}"
    payload = {
        "code": "send",
        "messageTemplateId": template_id,
        "idempotencyKey": f"S3S4-{uuid.uuid4().hex}",
        "messageParam": {
            "bizId": biz_id,
            "receiver": receiver,
            "variables": {
                "owner": owner,
                "event": "unordered",
                "bizId": biz_id,
                "ts": str(int(time.time() * 1000)),
            },
            "extra": {
                "businessOwner": owner,
            },
        },
    }
    return http_post_json(f"{base_url}/send", payload)


def main() -> int:
    parser = argparse.ArgumentParser(description="S3/S4 无序业务方接口测试脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--template-id", required=True, type=int)
    parser.add_argument("--send-account", required=True, type=int)
    parser.add_argument("--receiver", required=True)
    args = parser.parse_args()

    ensure_template_exists(args.base_url, args.template_id, args.send_account)

    for owner in ("notice-center", "growth-center"):
        response = send_message(args.base_url, args.template_id, args.receiver, owner)
        if str(response.get("code")) != "200":
            raise RuntimeError(f"[S3/S4] {owner} 发送失败: {json.dumps(response, ensure_ascii=False)}")
        print(f"[S3/S4] {owner} 发送成功: {json.dumps(response, ensure_ascii=False)}")

    print("[S3/S4] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S3/S4] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S3/S4] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
