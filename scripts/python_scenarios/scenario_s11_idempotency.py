#!/usr/bin/env python3
"""场景 S11: 接口幂等验证。"""

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
        "title": "S11 idempotency notification",
        "content": "event={$event}; bizId={$bizId}; timestamp={$ts}",
    }


def main() -> int:
    parser = create_parser("S11 幂等验证脚本")
    add_common_arguments(parser)
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s11")
        resources.template_id = create_template(
            args.base_url,
            "s11",
            resources.account_id,
            build_template_content(args, build_default_template_content()),
            args,
        )

        biz_id = f"IDEMP-{int(time.time() * 1000)}"
        idempotency_key = f"S11-{uuid.uuid4().hex}"

        first = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            biz_id,
            idempotency_key,
            {
                "event": "idempotency-check",
                "bizId": biz_id,
                "ts": str(int(time.time() * 1000)),
            },
            {"businessOwner": "notice-center"},
        )
        second = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            biz_id,
            idempotency_key,
            {
                "event": "idempotency-check",
                "bizId": biz_id,
                "ts": str(int(time.time() * 1000)),
            },
            {"businessOwner": "notice-center"},
        )

        if str(first.get("code")) != "200":
            raise RuntimeError(f"[S11] 第一次请求失败: {first}")

        second_code = str(second.get("code"))
        if second_code not in {"200", "A0007", "A0008"}:
            raise RuntimeError(f"[S11] 第二次请求结果不符合预期: {second}")

        print(f"[S11] 第一次响应: {first}")
        print(f"[S11] 第二次响应: {second}")
        for message_id in extract_message_ids(first):
            trace_result = wait_for_trace(
                args.base_url,
                message_id,
                args.trace_timeout_seconds,
                args.trace_interval_seconds,
            )
            print_trace_result("S11", message_id, trace_result)
    finally:
        cleanup_resources(args.base_url, resources)

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
