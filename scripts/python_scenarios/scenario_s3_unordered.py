#!/usr/bin/env python3
"""场景 S3/S4: 无序业务方验证。"""

from __future__ import annotations

import argparse
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


def build_default_template_content() -> dict:
    """构造场景默认模板内容。"""

    return {
        "title": "S3 unordered notification",
        "content": "owner={$owner}; event={$event}; bizId={$bizId}; timestamp={$ts}",
    }


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
            build_template_content(args, build_default_template_content()),
            args,
        )

        for owner in ("notice-center", "growth-center"):
            biz_id = f"{owner}-{int(time.time() * 1000)}"
            response = send_message(
                args.base_url,
                resources.template_id,
                args.receiver,
                biz_id,
                f"S3S4-{uuid.uuid4().hex}",
                {
                    "owner": owner,
                    "event": "unordered",
                    "bizId": biz_id,
                    "ts": str(int(time.time() * 1000)),
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
