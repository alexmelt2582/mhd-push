#!/usr/bin/env python3
"""场景 S15: 多层次 QPS / TPS / 顺序链路压测脚本。"""

from __future__ import annotations

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


def percentile(values: list[float], ratio: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int(len(ordered) * ratio) - 1))
    return ordered[index]


def build_default_template_content() -> dict:
    return json.dumps(
        {
            "title": "性能压测：{$mode}",
            "content": "业务键 {$bizId}，追踪 {$traceId}，业务域 {$owner}，步骤 {$step}",
            "url": "https://notify.example.com/perf/{$bizId}",
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )


def send_once(base_url: str, template_id: int, receiver: str, owner: str, mode: str, run_id: str, index: int) -> tuple[float, dict]:
    start = time.perf_counter()
    biz_id = f"S15-{mode}-{run_id}-{index:04d}"
    order_key = f"{owner}:{biz_id}" if mode == "ordered" else None
    response = send_message(
        base_url,
        template_id,
        receiver,
        biz_id,
        f"S15-{uuid.uuid4().hex}",
        {
            "mode": mode,
            "bizId": biz_id,
            "traceId": f"TRACE-{mode}-{run_id}-{index:04d}",
            "owner": owner,
            "step": str(index),
        },
        {
            "businessOwner": owner,
            "orderKey": order_key,
        },
    )
    return (time.perf_counter() - start) * 1000, response


def run_benchmark(base_url: str, template_id: int, receiver: str, owner: str, mode: str, run_id: str, requests: int, concurrency: int) -> tuple[list[float], list[dict], float]:
    latencies: list[float] = []
    responses: list[dict] = []
    started_at = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [
            executor.submit(send_once, base_url, template_id, receiver, owner, mode, run_id, index)
            for index in range(requests)
        ]
        for future in concurrent.futures.as_completed(futures):
            latency_ms, response = future.result()
            latencies.append(latency_ms)
            responses.append(response)
    return latencies, responses, time.perf_counter() - started_at


def main() -> int:
    parser = create_parser("S15 多层次压测：入口受理、无序吞吐、有序链路")
    add_common_arguments(parser)
    parser.add_argument("--requests", default=200, type=int)
    parser.add_argument("--concurrency", default=40, type=int)
    parser.add_argument("--mode", choices=["ingress", "unordered", "ordered"], default="ingress")
    parser.add_argument("--owner", default="perf-center")
    parser.add_argument("--trace-samples", default=3, type=int)
    args = parser.parse_args()

    resources = ScenarioResources()
    try:
#         resources.account_id = create_account(args.base_url, args, "s15")
        resources.account_id = create_account(args.base_url, args, "s15")
        resources.template_id = create_template(
            args.base_url,
            "s15",
            resources.account_id,
            build_template_content(args, build_default_template_content()),
            args,
        )

        owner = args.owner if args.mode != "ordered" else "order-center"
        run_id = uuid.uuid4().hex[:8]
        latencies, responses, total_seconds = run_benchmark(
            args.base_url,
            resources.template_id,
            args.receiver,
            owner,
            args.mode,
            run_id,
            args.requests,
            args.concurrency,
        )

        success_count = sum(1 for response in responses if str(response.get("code")) == "200")
        qps = 0.0 if total_seconds <= 0 else args.requests / total_seconds
        print(
            f"[S15] mode={args.mode} requests={args.requests} concurrency={args.concurrency} success={success_count}/{args.requests} qps={qps:.2f}"
        )
        print(
            f"[S15] avg_ms={statistics.mean(latencies):.2f} p95_ms={percentile(latencies, 0.95):.2f} p99_ms={percentile(latencies, 0.99):.2f} max_ms={max(latencies):.2f} total_seconds={total_seconds:.2f}"
        )

        sample_message_ids: list[str] = []
        for response in responses:
            sample_message_ids.extend(extract_message_ids(response))
            if len(sample_message_ids) >= args.trace_samples:
                sample_message_ids = sample_message_ids[: args.trace_samples]
                break

        for message_id in sample_message_ids:
            trace_result = wait_for_trace(args.base_url, message_id, args.trace_timeout_seconds, args.trace_interval_seconds)
            print_trace_result("S15", message_id, trace_result)
    finally:
        cleanup_resources(args.base_url, resources)

    print("[S15] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        # python scenario_s15_qps_benchmark.py --receiver 2@qq.com --account-config file:./account-email.json --requests 1 --concurrency 1
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S15] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S15] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)