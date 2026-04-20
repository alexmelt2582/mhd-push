#!/usr/bin/env python3
"""
总控脚本: 顺序执行所有 Python 场景脚本。

这个脚本的目的不是复用公共逻辑，而是作为统一入口，按顺序调度每个单独场景：
1. S1 有序业务链路
2. S3/S4 无序业务方
3. S11 幂等
4. S13/S14 大消息边界
5. S9 DLQ 人工补偿
6. S6 MQ 失败注入（可选）

说明：
1. 每个场景脚本都是完全自包含的。
2. 总控脚本只是负责串行执行，方便你一次跑完所有场景。
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


def run_script(script_path: Path, args: list[str]) -> None:
    """执行单个 Python 场景脚本。"""
    command = [sys.executable, str(script_path), *args]
    print(f"[RUN] {' '.join(command)}")
    subprocess.run(command, check=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="所有 Python API 场景总控脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--template-id", required=True, type=int)
    parser.add_argument("--send-account", required=True, type=int)
    parser.add_argument("--receiver", required=True)
    parser.add_argument("--include-mq-failure", action="store_true")
    parser.add_argument("--rocketmq-script-dir", default="./doc/rocketmq")
    parser.add_argument("--large-payload-bytes", default=3300000, type=int)
    args = parser.parse_args()

    root = Path(__file__).resolve().parent
    common_args = [
        "--base-url", args.base_url,
        "--template-id", str(args.template_id),
        "--send-account", str(args.send_account),
        "--receiver", args.receiver,
    ]

    run_script(root / "scenario_s1_ordered.py", common_args)
    run_script(root / "scenario_s3_unordered.py", common_args)
    run_script(root / "scenario_s11_idempotency.py", common_args)
    run_script(root / "scenario_s13_payload.py", common_args + ["--large-payload-bytes", str(args.large_payload_bytes)])
    run_script(root / "scenario_s9_dlq_compensate.py", ["--base-url", args.base_url])

    if args.include_mq_failure:
        run_script(
            root / "scenario_s6_mq_failure.py",
            common_args + ["--rocketmq-script-dir", args.rocketmq_script_dir],
        )
    else:
        print("[SKIP] S6 MQ 故障注入未启用，如需执行请加 --include-mq-failure")

    print("[DONE] 所有已选场景执行完成")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except subprocess.CalledProcessError as exc:
        print(f"[RUN] 子场景执行失败，退出码: {exc.returncode}", file=sys.stderr)
        sys.exit(exc.returncode)
    except Exception as exc:
        print(f"[RUN] 执行失败: {exc}", file=sys.stderr)
        sys.exit(1)
