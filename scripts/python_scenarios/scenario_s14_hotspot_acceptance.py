#!/usr/bin/env python3
"""场景 S14: 热点渠道压测下的接口受理验证。"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
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
    wait_for_trace,
)


DEFAULT_TEMPLATE_CONTENT_JSON = json.dumps(
    {
        "title": "热点受理压测：{$eventName}",
        "content": "业务键 {$bizId}，追踪 {$traceId}，业务域 {$owner}",
        "url": "https://notify.example.com/hotspot/{$bizId}",
    },
    ensure_ascii=False,
    separators=(",", ":"),
)


def send_once(base_url: str, template_id: int, receiver: str, owner: str, run_id: str, index: int) -> tuple[float, dict]:
    """执行一次真实 /send 调用。"""

    start = time.perf_counter()
    biz_id = f"S14-{run_id}-{index:04d}"
    response = send_message(
        base_url,
        template_id,
        receiver,
        biz_id,
        f"S14-{uuid.uuid4().hex}",
        {
            "eventName": f"热点批次-{index}",
            "bizId": biz_id,
            "traceId": f"TRACE-{run_id}-{index:04d}",
            "owner": owner,
        },
        {"businessOwner": owner},
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


def main() -> int:
    """执行热点 burst 验证。"""

    parser = create_parser("S14 热点渠道压测下的接口受理验证")
    add_common_arguments(parser)
    parser.add_argument("--requests", default=100, type=int)
    parser.add_argument("--concurrency", default=20, type=int)
    parser.add_argument("--owner", default="hotspot-center")
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s14")
        resources.template_id = create_template(
            args.base_url,
            "s14",
            resources.account_id,
            build_template_content(args, DEFAULT_TEMPLATE_CONTENT_JSON),
            args,
        )

        run_id = uuid.uuid4().hex[:8]
        latencies: list[float] = []
        responses: list[dict] = []
        started_at = time.perf_counter()
        with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
            futures = [
                executor.submit(send_once, args.base_url, resources.template_id, args.receiver, args.owner, run_id, index)
                for index in range(args.requests)
            ]
            for future in concurrent.futures.as_completed(futures):
                latency_ms, response = future.result()
                latencies.append(latency_ms)
                responses.append(response)
        total_seconds = time.perf_counter() - started_at

        success_count = sum(1 for response in responses if str(response.get("code")) == "200")
        sample_message_ids: list[str] = []
        for response in responses:
            sample_message_ids.extend(extract_message_ids(response))
            if len(sample_message_ids) >= 5:
                sample_message_ids = sample_message_ids[:5]
                break

        print(f"[S14] requests={args.requests} concurrency={args.concurrency} success={success_count}/{args.requests}")
        print(f"[S14] total_seconds={total_seconds:.2f} avg_ms={statistics.mean(latencies):.2f} p95_ms={percentile(latencies, 0.95):.2f} max_ms={max(latencies):.2f}")
        print(f"[S14] sample_message_ids={sample_message_ids}")
        for message_id in sample_message_ids[:3]:
            trace_result = wait_for_trace(args.base_url, message_id, args.trace_timeout_seconds, args.trace_interval_seconds)
            print_trace_result("S14", message_id, trace_result)
    finally:
        cleanup_resources(args.base_url, resources)

    print("[S14] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S14] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S14] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)