#!/usr/bin/env python3
"""场景 S6: MQ 发送失败注入。"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
import urllib.error
import uuid
from pathlib import Path

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
        "title": "MQ 可用性验证：{$eventName}",
        "content": "业务键 {$bizId} 在阶段 {$eventName} 进行发送验证，时间 {$ts}",
    },
    ensure_ascii=False,
    separators=(",", ":"),
)


def run_batch_script(script_path: Path) -> None:
    if not script_path.exists():
        raise FileNotFoundError(f"脚本不存在: {script_path}")
    subprocess.run([str(script_path)], check=True, shell=True)


def main() -> int:
    parser = create_parser("S6 MQ 失败注入脚本")
    add_common_arguments(parser)
    parser.add_argument("--rocketmq-script-dir", default="./doc/rocketmq")
    args = parser.parse_args()

    script_dir = Path(args.rocketmq_script_dir)
    stop_script = script_dir / "stop-rocketmq.bat"
    start_script = script_dir / "start-rocketmq.bat"

    resources = ScenarioResources()
    try:
        resources.account_id = create_account(args.base_url, args, "s6")
        resources.template_id = create_template(
            args.base_url,
            "s6",
            resources.account_id,
            build_template_content(args, DEFAULT_TEMPLATE_CONTENT_JSON),
            args,
        )

        run_id = uuid.uuid4().hex[:8]
        biz_id = f"MQFAIL-{run_id}"
        base_ts = int(time.time() * 1000)

        print("[S6] 停止 RocketMQ，开始故障注入")
        run_batch_script(stop_script)
        time.sleep(4)

        fail_result = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            biz_id,
            f"S6-mq-down-{run_id}",
            {
                "eventName": "MQ停机",
                "bizId": biz_id,
                "ts": str(base_ts),
            },
            {"businessOwner": "order-center", "orderKey": f"order-center:{biz_id}"},
        )
        fail_code = str(fail_result.get("code"))
        if fail_code not in {"B0001", "-1", "500"}:
            raise RuntimeError(f"[S6] MQ关闭时返回不符合预期: {fail_result}")
        print(f"[S6] MQ关闭时响应: {fail_result}")

        print("[S6] 启动 RocketMQ，等待恢复")
        run_batch_script(start_script)
        time.sleep(8)

        recover_result = send_message(
            args.base_url,
            resources.template_id,
            args.receiver,
            biz_id,
            f"S6-mq-up-{run_id}",
            {
                "eventName": "MQ恢复",
                "bizId": biz_id,
                "ts": str(base_ts + 1),
            },
            {"businessOwner": "order-center", "orderKey": f"order-center:{biz_id}"},
        )
        if str(recover_result.get("code")) != "200":
            raise RuntimeError(f"[S6] MQ恢复后发送失败: {recover_result}")
        print(f"[S6] MQ恢复后响应: {recover_result}")
        for message_id in extract_message_ids(recover_result):
            trace_result = wait_for_trace(args.base_url, message_id, args.trace_timeout_seconds, args.trace_interval_seconds)
            print_trace_result("S6", message_id, trace_result)
    finally:
        try:
            run_batch_script(start_script)
        except Exception:
            pass
        cleanup_resources(args.base_url, resources)

    print("[S6] 场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        print(f"[S6] HTTP 错误: {exc.code} {exc.reason}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[S6] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
