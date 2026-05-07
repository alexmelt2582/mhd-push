#!/usr/bin/env python3
"""场景 S3/S4: 无序业务方验证。"""

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
        "title": "业务通知：{$eventName}",
        "content": "业务域 {$owner} 发起 {$eventName}，业务键 {$bizId}，生成时间 {$ts}",
    },
    ensure_ascii=False,
    separators=(",", ":"),
)


def main() -> int:
    parser = create_parser("S3/S4 无序业务方接口测试脚本")
    add_common_arguments(parser)
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s3s4")
        resources.template_id = create_template(
            args.base_url,
            "s3s4",
            resources.account_id,
            build_template_content(args, DEFAULT_TEMPLATE_CONTENT_JSON),
            args,
        )

        run_id = uuid.uuid4().hex[:8]
        owners = (
            ("notice-center", "账单生成通知"),
            ("growth-center", "会员营销触达"),
        )
        base_ts = int(time.time() * 1000)
        for index, (owner, event_name) in enumerate(owners):
            biz_id = f"UNORDERED-{run_id}-{index + 1}"
            response = send_message(
                args.base_url,
                resources.template_id,
                args.receiver,
                biz_id,
                f"S3S4-{run_id}-{owner}",
                {
                    "owner": owner,
                    "eventName": event_name,
                    "bizId": biz_id,
                    "ts": str(base_ts + index),
                },
                {
                    "businessOwner": owner,
                },
            )
            if str(response.get("code")) != "200":
                raise RuntimeError(f"[S3/S4] {owner} 发送失败: {response}")
            print(f"[S3/S4] {owner} 发送成功: {response}")
            for message_id in extract_message_ids(response):
                trace_result = wait_for_trace(
                    args.base_url,
                    message_id,
                    args.trace_timeout_seconds,
                    args.trace_interval_seconds,
                )
                print_trace_result(f"S3/S4 {owner}", message_id, trace_result)
    finally:
        cleanup_resources(args.base_url, resources)

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
