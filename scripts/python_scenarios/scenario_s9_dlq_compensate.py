#!/usr/bin/env python3
"""
场景 S9: DLQ 人工补偿验证。

这个脚本不负责制造失败消息，而是在已经存在 DLQ 记录时执行人工补偿流程：
1. 查询 DLQ 列表。
2. 获取最新一条记录的明细。
3. 调用人工补偿接口重新投递。

说明：
1. 如果当前没有 DLQ 记录，脚本会直接提示并退出。
2. 这个脚本验证的是“人工处理入口”是否正常。
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request

from scenario_common import create_parser


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
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    parser = create_parser("S9 DLQ 人工补偿脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    args = parser.parse_args()

    list_result = http_get_json(f"{args.base_url}/mq/dlq/list?pageNo=1&pageSize=5")
    if str(list_result.get("code")) != "200":
        raise RuntimeError(f"[S9] 查询 DLQ 列表失败: {json.dumps(list_result, ensure_ascii=False)}")

    records = list_result.get("data") or []
    if not records:
        print("[S9] 当前没有 DLQ 记录，请先制造一个消费失败场景")
        return 0

    message_id = str(records[0].get("messageId") or "")
    if not message_id:
        raise RuntimeError("[S9] DLQ 记录缺少 messageId")

    detail_result = http_get_json(f"{args.base_url}/mq/dlq/{message_id}")
    if str(detail_result.get("code")) != "200":
        raise RuntimeError(f"[S9] 查询 DLQ 明细失败: {json.dumps(detail_result, ensure_ascii=False)}")
    print(f"[S9] DLQ 明细: {json.dumps(detail_result, ensure_ascii=False)}")

    compensate_result = http_post_json(f"{args.base_url}/mq/dlq/compensate/{message_id}", {})
    if str(compensate_result.get("code")) != "200":
        raise RuntimeError(f"[S9] 人工补偿失败: {json.dumps(compensate_result, ensure_ascii=False)}")
    print(f"[S9] 人工补偿成功: {json.dumps(compensate_result, ensure_ascii=False)}")

    print("[S9] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S9] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S9] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
