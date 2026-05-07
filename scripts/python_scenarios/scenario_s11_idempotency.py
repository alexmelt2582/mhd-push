#!/usr/bin/env python3
"""场景 S11: 接口幂等验证。"""

from __future__ import annotations

import argparse
import json
import sys
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
        "title": "接口幂等验证：{$eventName}",
        "content": "幂等检查消息，业务键 {$bizId}，事件 {$eventName}，固定时间 {$ts}",
    },
    ensure_ascii=False,
    separators=(",", ":"),
)


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
            build_template_content(args, DEFAULT_TEMPLATE_CONTENT_JSON),
            args,
        )

        run_id = uuid.uuid4().hex[:8]
        biz_id = f"IDEMP-{run_id}"
        idempotency_key = f"S11-{uuid.uuid4().hex}"
        fixed_variables = {
            "eventName": "重复提交同一条付款成功通知",
            "bizId": biz_id,
            "ts": "2026-04-21T10:00:00Z",
        }
        fixed_extra = {"businessOwner": "notice-center", "orderKey": f"notice-center:{biz_id}"}

        first = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            biz_id,
            idempotency_key,
            fixed_variables,
            fixed_extra,
        )
        second = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            biz_id,
            idempotency_key,
            fixed_variables,
            fixed_extra,
        )

        if str(first.get("code")) != "200":
            raise RuntimeError(f"[S11] 第一次请求失败: {first}")

        second_code = str(second.get("code"))
        if second_code not in {"200", "A0007", "A0008"}:
            raise RuntimeError(f"[S11] 第二次请求结果不符合预期: {second}")

        print(f"[S11] 第一次响应: {first}")
        print(f"[S11] 第二次响应: {second}")
        print(f"[S11] 两次请求使用完全相同的 payload 与 idempotencyKey，仅允许命中一次真实受理")
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
