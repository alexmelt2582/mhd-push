#!/usr/bin/env python3
"""场景 S12: 模板缓存与真实发送链路验证。"""

from __future__ import annotations

import argparse
import statistics
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
    update_template,
    wait_for_trace,
)


def build_default_template_content(body_size: int) -> dict:
    """构造指定正文规模的邮件模板。"""

    repeated_text = "x" * max(64, body_size)
    return {
        "title": "cache-check {$event}",
        "content": f"biz={{$bizId}}; trace={{$traceId}}; payload={repeated_text}",
        "url": "https://notify.example.com/{$bizId}/{$traceId}",
    }


def send_once(base_url: str, template_id: int, receiver: str, marker: str) -> tuple[float, dict]:
    """发送单次消息并返回耗时。"""

    start = time.perf_counter()
    response = send_message(
        base_url,
        template_id,
        receiver,
        f"S12-{marker}-{int(time.time() * 1000)}",
        f"S12-{uuid.uuid4().hex}",
        {
            "event": marker,
            "bizId": f"BIZ-{marker}",
            "traceId": uuid.uuid4().hex,
        },
        {"businessOwner": "cache-observer"},
    )
    cost_ms = (time.perf_counter() - start) * 1000
    return cost_ms, response


def percentile(values: list[float], ratio: float) -> float:
    """计算简单分位值。"""

    if not values:
        return 0.0
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int(len(ordered) * ratio) - 1))
    return ordered[index]


def run_case(base_url: str, template_id: int, send_account_id: int, receiver: str, body_size: int, hot_runs: int, args: argparse.Namespace) -> None:
    """运行一个模板规模场景。"""

    update_template(
        base_url,
        template_id,
        "s12",
        send_account_id,
        build_template_content(args, build_default_template_content(body_size)),
        args,
    )
    cold_cost_ms, cold_response = send_once(base_url, template_id, receiver, f"cold-{body_size}")

    hot_costs: list[float] = []
    last_response = cold_response
    for index in range(hot_runs):
        hot_cost_ms, last_response = send_once(base_url, template_id, receiver, f"hot-{body_size}-{index}")
        hot_costs.append(hot_cost_ms)

    print(f"[S12] body_size={body_size} cold_ms={cold_cost_ms:.2f} hot_avg_ms={statistics.mean(hot_costs):.2f} hot_p95_ms={percentile(hot_costs, 0.95):.2f}")
    for message_id in extract_message_ids(last_response):
        trace_result = wait_for_trace(base_url, message_id, args.trace_timeout_seconds, args.trace_interval_seconds)
        print_trace_result("S12", message_id, trace_result)


def parse_args() -> argparse.Namespace:
    """解析命令行参数。"""

    parser = create_parser("S12 模板缓存与真实发送链路验证脚本")
    add_common_arguments(parser)
    parser.add_argument("--hot-runs", default=20, type=int)
    parser.add_argument("--body-sizes", default="256,4096", help="逗号分隔的正文大小列表")
    return parser.parse_args()


def main() -> int:
    """执行脚本主流程。"""

    args = parse_args()
    body_sizes = [int(item.strip()) for item in args.body_sizes.split(",") if item.strip()]
    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s12")
        resources.template_id = create_template(
            args.base_url,
            "s12",
            resources.account_id,
            build_template_content(args, build_default_template_content(body_sizes[0])),
            args,
        )
        for body_size in body_sizes:
            run_case(args.base_url, resources.template_id, resources.account_id, args.receiver, body_size, args.hot_runs, args)
    finally:
        cleanup_resources(args.base_url, resources)
    print("[S12] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S12] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S12] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)