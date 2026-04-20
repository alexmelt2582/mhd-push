#!/usr/bin/env python3
"""场景 S1: 有序业务链路验证。"""

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
    """构造邮箱模板内容，变量名必须与发送时的 variables 对齐。"""

    return {
        "title": "S1 ordered notification",
        "content": "event={$event}; bizId={$bizId}; timestamp={$ts}",
    }


def main() -> int:
    parser = create_parser(
        "S1 有序业务链路接口测试脚本",
        examples=[
            "python scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json",
            "python scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json --template-content file:./template-s1.json",
        ],
    )
    add_common_arguments(parser)
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s1")
        resources.template_id = create_template(
            args.base_url,
            "s1",
            resources.account_id,
            build_template_content(args, build_default_template_content()),
            args,
        )

        biz_id = f"ORDER-1001-{int(time.time() * 1000)}"
        events = ["cart-success", "order-success", "delivery-success", "receive-success"]

        for event in events:
            response = send_message(
                args.base_url,
                resources.template_id,
                args.receiver,
                biz_id,
                f"S1-{uuid.uuid4().hex}",
                {
                    "event": event,
                    "bizId": biz_id,
                    "ts": str(int(time.time() * 1000)),
                },
                {
                    "businessOwner": "order-center",
                },
            )
            if str(response.get("code")) != "200":
                raise RuntimeError(f"[S1] {event} 发送失败: {response}")
            message_ids = extract_message_ids(response)
            print(f"[S1] {event} 发送成功: {response}")
            for message_id in message_ids:
                trace_result = wait_for_trace(
                    args.base_url,
                    message_id,
                    args.trace_timeout_seconds,
                    args.trace_interval_seconds,
                )
                print_trace_result(f"S1 {event}", message_id, trace_result)
    finally:
        cleanup_resources(args.base_url, resources)

    print("[S1] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S1] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S1] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
