#!/usr/bin/env python3
"""场景 S13: 大消息边界验证。"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import uuid

from scenario_common import (
    ScenarioResources,
    add_common_arguments,
    build_template_content,
    cleanup_resources,
    create_account,
    create_parser,
    create_template,
    extract_message_ids,
    print_trace_result,
    send_message,
    wait_for_trace,
)


DEFAULT_TEMPLATE_CONTENT_JSON = json.dumps(
    {
        "title": "大消息边界验证：{$eventName}",
        "content": "业务键 {$bizId} 的正文片段 {$blob}，生成时间 {$ts}",
    },
    ensure_ascii=False,
    separators=(",", ":"),
)


def main() -> int:
    parser = create_parser("S13 大消息边界测试脚本")
    add_common_arguments(parser)
    parser.add_argument("--large-payload-bytes", default=3300000, type=int)
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s13")
        resources.template_id = create_template(
            args.base_url,
            "s13",
            resources.account_id,
            build_template_content(args, DEFAULT_TEMPLATE_CONTENT_JSON),
            args,
        )

        near_blob = ("<p>订单摘要</p>" * max(128, args.large_payload_bytes // 12))[: max(1024, args.large_payload_bytes - 2048)]
        over_blob = ("<div>营销活动正文</div>" * max(128, args.large_payload_bytes // 18))[: args.large_payload_bytes]
        biz_id = f"PAYLOAD-{int(time.time() * 1000)}"

        near_result = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            f"{biz_id}-near",
            f"near-{uuid.uuid4().hex}",
            {
                "eventName": "接近阈值正文",
                "bizId": f"{biz_id}-near",
                "blob": near_blob,
                "ts": str(int(time.time() * 1000)),
            },
            {"businessOwner": "order-center"},
        )
        over_result = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            f"{biz_id}-over",
            f"over-{uuid.uuid4().hex}",
            {
                "eventName": "超出阈值正文",
                "bizId": f"{biz_id}-over",
                "blob": over_blob,
                "ts": str(int(time.time() * 1000)),
            },
            {"businessOwner": "order-center"},
        )

        if str(near_result.get("code")) != "200":
            raise RuntimeError(f"[S13] near-limit 结果异常: {near_result}")
        if str(over_result.get("code")) != "A0006":
            raise RuntimeError(f"[S13] over-limit 结果异常: {over_result}")

        print(f"[S13] near-limit 响应: {near_result}")
        print(f"[S13] over-limit 响应: {over_result}")
        for message_id in extract_message_ids(near_result):
            trace_result = wait_for_trace(args.base_url, message_id, args.trace_timeout_seconds, args.trace_interval_seconds)
            print_trace_result("S13", message_id, trace_result)
    finally:
        cleanup_resources(args.base_url, resources)

    print("[S13] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S13] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S13] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
