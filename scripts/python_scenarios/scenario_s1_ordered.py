#!/usr/bin/env python3
"""场景 S1: 有序业务链路验证。"""

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
        "title": "订单进度通知：{$eventName}",
        "content": "您好，订单 {$bizId} 当前节点：{$eventName}，处理时间 {$ts}，详情请查看 {$detailUrl}",
    },
    ensure_ascii=False,
    separators=(",", ":"),
)


def main() -> int:
    parser = create_parser(
        "S1 有序业务链路接口测试脚本",
        examples=[
            "python scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json",
            "python scenario_s1_ordered.py --receiver 2@qq.com --account-config file:./account-email.json --template-content file:./template-s1.json",
        ],
    )
    add_common_arguments(parser)
    parser.add_argument("--biz-id", help="同一条有序业务链路的业务键，不传则自动生成")
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s1")
        resources.template_id = create_template(
            args.base_url,
            "s1",
            resources.account_id,
            build_template_content(args, DEFAULT_TEMPLATE_CONTENT_JSON),
            args,
        )

        run_id = uuid.uuid4().hex[:8]
        biz_id = args.biz_id or f"ORDER-{run_id}"
        order_key = f"order-center:{biz_id}"
        events = [
            ("cart-locked", "购物车锁单"),
            ("payment-success", "支付成功"),
            ("warehouse-shipped", "仓库发货"),
            ("delivery-signed", "用户签收"),
        ]
        base_ts = int(time.time() * 1000)

        for index, (event_code, event_name) in enumerate(events):
            response = send_message(
                args.base_url,
                resources.template_id,
                args.receiver,
                biz_id,
                f"S1-{run_id}-{event_code}",
                {
                    "eventName": event_name,
                    "bizId": biz_id,
                    "ts": str(base_ts + index),
                    "detailUrl": f"https://order.example.com/orders/{biz_id}?step={event_code}",
                },
                {
                    "businessOwner": "order-center",
                    "orderKey": order_key,
                },
            )
            if str(response.get("code")) != "200":
                raise RuntimeError(f"[S1] {event_code} 发送失败: {response}")
            message_ids = extract_message_ids(response)
            print(f"[S1] {event_code} 发送成功: {response}")
            for message_id in message_ids:
                trace_result = wait_for_trace(
                    args.base_url,
                    message_id,
                    args.trace_timeout_seconds,
                    args.trace_interval_seconds,
                )
                print_trace_result(f"S1 {event_code}", message_id, trace_result)
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
