#!/usr/bin/env python3
"""模板样例接口验证脚本。

设计目标：
1. 不依赖现有 python_scenarios 目录中的公共函数。
2. 直接读取模板目录和发送实例目录，按文件名自动配对。
3. 支持单场景重跑，替换任意模板文件后无需改脚本。

默认流程：
1. 读取模板 JSON。
2. 调用管理端模板接口，已有 ID 时优先 update；没有 ID 时直接 add。
3. 从 add 响应或 query 结果中拿到最终模板 ID。
4. 再次查询模板详情，确认已落库。
5. 读取发送实例并调用 public-api 的 /send。
5. 打印每个场景的清晰结果摘要。
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SUCCESS_CODE = "200"


@dataclass
class CaseFiles:
    """单个场景需要的模板文件和发送文件。"""

    case_key: str
    template_file: Path
    send_file: Path


def parse_args() -> argparse.Namespace:
    """解析命令行参数。"""

    parser = argparse.ArgumentParser(
        description="读取模板样例和发送样例，执行模板 upsert 与发送验证。",
        formatter_class=argparse.RawTextHelpFormatter,
        epilog=(
            "示例:\n"
            "  python verify_template_examples.py --admin-base-url http://127.0.0.1:8081 --public-base-url http://127.0.0.1:8080\n"
            "  python verify_template_examples.py --case 场景7\n"
            "  python verify_template_examples.py --case \"场景6：每周运营数据周报 (Email)\"\n"
        ),
    )
    parser.add_argument("--admin-base-url", default="http://127.0.0.1:8081", help="管理端 API 地址")
    parser.add_argument("--public-base-url", default="http://127.0.0.1:8080", help="用户发送 API 地址")
    parser.add_argument(
        "--templates-dir",
        default="scripts/examples/模板",
        help="模板样例目录",
    )
    parser.add_argument(
        "--send-dir",
        default="scripts/examples/发送实例",
        help="发送样例目录",
    )
    parser.add_argument(
        "--case",
        action="append",
        default=[],
        help="只执行指定场景，可传多次；支持场景前缀或完整名称",
    )
    parser.add_argument(
        "--skip-upsert",
        action="store_true",
        help="跳过模板新增/更新，只执行发送请求",
    )
    parser.add_argument(
        "--skip-send",
        action="store_true",
        help="只验证模板新增/更新，不发送",
    )
    parser.add_argument(
        "--fail-fast",
        action="store_true",
        help="遇到第一个失败场景时立即退出",
    )
    return parser.parse_args()


def http_json(url: str, method: str, payload: object | None = None) -> dict[str, Any]:
    """发送 JSON 请求并返回解析结果。"""

    data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url=url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def http_get_json(url: str) -> dict[str, Any]:
    """发送 GET 请求并返回 JSON。"""

    request = urllib.request.Request(url=url, method="GET")
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def read_json_file(file_path: Path) -> dict[str, Any]:
    """读取单个 JSON 文件。"""

    with file_path.open("r", encoding="utf-8") as file:
        return json.load(file)


def resolve_base_urls(args: argparse.Namespace) -> tuple[str, str]:
    """解析管理端和用户端 API 地址。"""
    admin_base_url = args.admin_base_url.rstrip("/")
    public_base_url = args.public_base_url.rstrip("/")
    return admin_base_url, public_base_url


def normalize_template_case_key(file_name: str) -> str:
    """从模板文件名提取场景键。"""

    return Path(file_name).stem


def normalize_send_case_key(file_name: str) -> str:
    """从发送文件名提取场景键。"""

    stem = Path(file_name).stem
    if stem.endswith("-发送"):
        return stem[:-3]
    return stem


def collect_case_files(templates_dir: Path, send_dir: Path, selected_cases: list[str]) -> list[CaseFiles]:
    """收集模板文件和发送文件的配对关系。"""

    template_files = {
        normalize_template_case_key(path.name): path
        for path in sorted(templates_dir.glob("*.json"))
    }
    send_files = {
        normalize_send_case_key(path.name): path
        for path in sorted(send_dir.glob("*.json"))
    }

    missing_send_cases = sorted(set(template_files) - set(send_files))
    if missing_send_cases:
        raise RuntimeError(f"以下模板缺少对应发送样例: {missing_send_cases}")

    case_keys = sorted(template_files.keys())
    if selected_cases:
        filtered_keys: list[str] = []
        for case_key in case_keys:
            if any(case_key == selected or case_key.startswith(selected) for selected in selected_cases):
                filtered_keys.append(case_key)
        if not filtered_keys:
            raise RuntimeError(f"未匹配到任何场景: {selected_cases}")
        case_keys = filtered_keys

    return [
        CaseFiles(case_key=case_key, template_file=template_files[case_key], send_file=send_files[case_key])
        for case_key in case_keys
    ]


def build_template_payload(template_data: dict[str, Any]) -> dict[str, Any]:
    """筛选出模板接口需要的字段。"""

    allowed_keys = {
        "id",
        "name",
        "idType",
        "sendChannel",
        "templateType",
        "msgType",
        "shieldType",
        "msgContent",
        "sendAccount",
    }
    return {key: value for key, value in template_data.items() if key in allowed_keys}


def query_template(admin_base_url: str, template_id: int) -> dict[str, Any]:
    """查询模板详情。"""

    return http_get_json(f"{admin_base_url}/messageTemplate/query/{template_id}")


def upsert_template(admin_base_url: str, template_data: dict[str, Any]) -> tuple[str, int, dict[str, Any], dict[str, Any]]:
    """模板存在则更新，不存在则新增，并返回最终模板 ID。"""

    template_id = template_data.get("id")
    payload = build_template_payload(template_data)

    if template_id is None:
        result = http_json(f"{admin_base_url}/messageTemplate/add", "POST", payload)
        generated_template_id = result.get("data")
        if str(result.get("code")) != SUCCESS_CODE or generated_template_id is None:
            raise RuntimeError(f"模板 add 未返回可用的模板ID: {json.dumps(result, ensure_ascii=False)}")
        return "add", int(generated_template_id), result, query_template(admin_base_url, int(generated_template_id))

    query_result = query_template(admin_base_url, int(template_id))
    exists = str(query_result.get("code")) == SUCCESS_CODE and query_result.get("data") is not None
    if exists:
        action = "update"
        result = http_json(f"{admin_base_url}/messageTemplate/update", "POST", payload)
    else:
        action = "add"
        result = http_json(f"{admin_base_url}/messageTemplate/add", "POST", payload)

    resolved_template_id = int(template_id)
    if action == "add" and result.get("data") is not None:
        resolved_template_id = int(result.get("data"))
    return action, resolved_template_id, result, query_template(admin_base_url, resolved_template_id)


def send_case(public_base_url: str, send_data: dict[str, Any]) -> dict[str, Any]:
    """执行发送请求。"""

    return http_json(f"{public_base_url}/send", "POST", send_data)


def resolve_send_template_id(template_data: dict[str, Any], send_data: dict[str, Any], template_id: int | None) -> int | None:
    """决定发送请求最终使用的模板 ID。"""

    if template_id is not None:
        return template_id
    if template_data.get("id") is not None:
        return int(template_data.get("id"))
    if send_data.get("templateId") is not None:
        return int(send_data.get("templateId"))
    return None


def print_case_header(case_key: str) -> None:
    """打印场景头部。"""

    print("=" * 80)
    print(case_key)
    print("=" * 80)


def print_step_result(step: str, result: dict[str, Any]) -> None:
    """打印单步结果摘要。"""

    print(f"[{step}] code={result.get('code')} msg={result.get('msg')}")
    data = result.get("data")
    if data is not None:
        print(f"[{step}] data={json.dumps(data, ensure_ascii=False)}")


def ensure_success(result: dict[str, Any], step_name: str) -> None:
    """断言接口响应成功。"""

    if str(result.get("code")) != SUCCESS_CODE:
        raise RuntimeError(f"{step_name} 失败: {json.dumps(result, ensure_ascii=False)}")


def run_case(admin_base_url: str, public_base_url: str, case_files: CaseFiles, skip_upsert: bool, skip_send: bool) -> None:
    """执行单个场景。"""

    template_data = read_json_file(case_files.template_file)
    send_data = read_json_file(case_files.send_file)
    resolved_template_id: int | None = None

    print_case_header(case_files.case_key)
    print(f"模板文件: {case_files.template_file}")
    print(f"发送文件: {case_files.send_file}")

    if not skip_upsert:
        action, resolved_template_id, upsert_result, query_result = upsert_template(admin_base_url, template_data)
        print(f"[template] action={action}")
        print(f"[template] id={resolved_template_id}")
        print_step_result("template-upsert", upsert_result)
        ensure_success(upsert_result, "模板 upsert")
        print_step_result("template-query", query_result)
        ensure_success(query_result, "模板查询")

    if not skip_send:
        resolved_template_id = resolve_send_template_id(template_data, send_data, resolved_template_id)
        if resolved_template_id is None:
            raise RuntimeError("发送前无法确定 templateId，请先执行模板新增/更新，或在模板/发送文件中提供 templateId")

        # 始终以本次 upsert 或模板文件解析出的 ID 覆盖发送参数，避免 admin/public 数据不一致。
        send_data["templateId"] = resolved_template_id
        send_result = send_case(public_base_url, send_data)
        print_step_result("send", send_result)
        ensure_success(send_result, "发送接口")


def main() -> int:
    """脚本入口。"""

    args = parse_args()
    admin_base_url, public_base_url = resolve_base_urls(args)
    root = Path(__file__).resolve().parent.parent
    templates_dir = (root / args.templates_dir).resolve()
    send_dir = (root / args.send_dir).resolve()

    if not templates_dir.exists():
        raise RuntimeError(f"模板目录不存在: {templates_dir}")
    if not send_dir.exists():
        raise RuntimeError(f"发送目录不存在: {send_dir}")

    case_files_list = collect_case_files(templates_dir, send_dir, args.case)
    print(f"[INFO] admin_base_url={admin_base_url}")
    print(f"[INFO] public_base_url={public_base_url}")
    print(f"[INFO] templates_dir={templates_dir}")
    print(f"[INFO] send_dir={send_dir}")
    print(f"[INFO] cases={len(case_files_list)}")

    failed_cases: list[tuple[str, str]] = []
    for case_files in case_files_list:
        try:
            run_case(admin_base_url, public_base_url, case_files, args.skip_upsert, args.skip_send)
        except Exception as exc:
            failed_cases.append((case_files.case_key, str(exc)))
            print(f"[FAIL] {case_files.case_key}: {exc}")
            if args.fail_fast:
                break

    print("-" * 80)
    if failed_cases:
        print(f"[SUMMARY] success={len(case_files_list) - len(failed_cases)} fail={len(failed_cases)}")
        for case_key, reason in failed_cases:
            print(f"[SUMMARY] {case_key} -> {reason}")
        return 1

    print(f"[SUMMARY] success={len(case_files_list)} fail=0")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        print(f"[HTTP ERROR] code={exc.code} reason={exc.reason} body={body}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"[ERROR] {exc}", file=sys.stderr)
        sys.exit(1)