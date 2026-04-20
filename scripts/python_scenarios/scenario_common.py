#!/usr/bin/env python3
"""Python 场景公共工具。

统一封装：
1. 真实 HTTP 调用。
2. 临时账号创建与删除。
3. 临时模板创建、更新与删除。
4. 发送请求与链路日志轮询。
"""

from __future__ import annotations

import argparse
import base64
import json
import random
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from typing import Any, Iterable, Sequence


DEFAULT_CREATOR = "mhd"
DEFAULT_SEND_CHANNEL = 40
DEFAULT_ID_TYPE = 50
DEFAULT_TEMPLATE_TYPE = 20
DEFAULT_MSG_TYPE = 10
DEFAULT_SHIELD_TYPE = 10
DEFAULT_ACCOUNT_CONFIG_JSON = json.dumps(
    {
        "host": "smtp.qq.com",
        "port": 465,
        "from": "replace-me@example.com",
        "user": "replace-me@example.com",
        "pass": "replace-with-auth-code",
        "auth": True,
        "sslEnable": True,
        "starttlsEnable": False,
    },
    ensure_ascii=False,
    separators=(",", ":"),
)
COMMON_HELP_TEXT = """账号配置输入:
    --account-config             支持 JSON 字符串、file:路径、base64:内容
    --account-config-override    局部覆盖，格式 key=value，可重复使用

模板内容输入:
    --template-content           支持 JSON 字符串、file:路径、base64:内容
    --template-content-override  局部覆盖，格式 key=value，可重复使用

跨 Shell 建议:
    1. PowerShell / cmd / Linux 都优先使用 file:路径
    2. 如果必须内联传值，优先使用 base64:内容
    3. 直接传原始 JSON 只在 shell 转义可控时使用
"""


@dataclass
class ScenarioResources:
    """记录当前场景创建的临时资源。"""

    account_id: int | None = None
    template_id: int | None = None


def create_parser(description: str, examples: Sequence[str] | None = None) -> argparse.ArgumentParser:
    """创建统一风格的命令行解析器。"""

    epilog_parts: list[str] = []
    if examples:
        epilog_parts.append("示例:\n  " + "\n  ".join(examples))
    epilog_parts.append(COMMON_HELP_TEXT)
    return argparse.ArgumentParser(
        description=description,
        formatter_class=argparse.RawTextHelpFormatter,
        epilog="\n\n".join(epilog_parts),
    )


def http_get_json(url: str) -> dict:
    """发送 GET 请求并返回 JSON。"""

    request = urllib.request.Request(url=url, method="GET")
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def http_json(url: str, method: str, payload: object | None = None) -> dict:
    """发送 JSON 请求并返回 JSON。"""

    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url=url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def add_common_arguments(parser: argparse.ArgumentParser) -> None:
    """为场景脚本增加统一参数。"""

    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--receiver", required=True)
    parser.add_argument("--creator", default=DEFAULT_CREATOR)
    parser.add_argument("--send-channel", default=DEFAULT_SEND_CHANNEL, type=int)
    parser.add_argument("--id-type", default=DEFAULT_ID_TYPE, type=int)
    parser.add_argument("--template-type", default=DEFAULT_TEMPLATE_TYPE, type=int)
    parser.add_argument("--msg-type", default=DEFAULT_MSG_TYPE, type=int)
    parser.add_argument("--shield-type", default=DEFAULT_SHIELD_TYPE, type=int)
    parser.add_argument("--trace-timeout-seconds", default=15, type=int)
    parser.add_argument("--trace-interval-seconds", default=1, type=int)
    parser.add_argument(
        "--account-config",
        default=DEFAULT_ACCOUNT_CONFIG_JSON,
        help="账号配置来源，支持 JSON 字符串、file:路径、base64:内容；默认给出邮件账号样例",
    )
    parser.add_argument(
        "--account-config-override",
        action="append",
        default=[],
        help="覆盖账号配置中的字段，格式 key=value，可重复使用，支持 a.b.c=value",
    )
    parser.add_argument(
        "--template-content",
        help="模板内容来源，支持 JSON 字符串、file:路径、base64:内容；不传时使用当前场景默认模板内容",
    )
    parser.add_argument(
        "--template-content-override",
        action="append",
        default=[],
        help="覆盖模板内容中的字段，格式 key=value，可重复使用，支持 a.b.c=value",
    )


def normalize_scalar_strings(value: Any) -> Any:
    """将常见字符串标量值转换为更合适的 JSON 类型。"""

    if isinstance(value, dict):
        return {key: normalize_scalar_strings(item) for key, item in value.items()}
    if isinstance(value, list):
        return [normalize_scalar_strings(item) for item in value]
    if isinstance(value, str):
        lowered = value.strip().lower()
        if lowered == "true":
            return True
        if lowered == "false":
            return False
        if lowered in {"null", "none"}:
            return None
    return value




def load_structured_text(raw_value: str, source_name: str) -> str:
    """读取结构化文本输入。"""

    value = raw_value.strip()
    if value.startswith("file:"):
        file_path = value[5:]
        with open(file_path, "r", encoding="utf-8") as file:
            return file.read()
    if value.startswith("base64:"):
        try:
            return base64.b64decode(value[7:]).decode("utf-8")
        except Exception as exc:
            raise RuntimeError(f"{source_name} base64 解码失败: {exc}") from exc
    return value


def parse_json_object(raw_json: str, source_name: str) -> dict[str, Any]:
    """解析 JSON 对象。"""

    try:
        data = json.loads(load_structured_text(raw_json, source_name))
    except json.JSONDecodeError as exc:
        raise RuntimeError(
            f"{source_name} 不是合法 JSON: {exc}。建议统一使用 file:路径 或 base64:内容，避免不同 shell 的转义差异"
        ) from exc
    if not isinstance(data, dict):
        raise RuntimeError(f"{source_name} 必须是 JSON 对象")
    return normalize_scalar_strings(data)


def parse_override_value(raw_value: str) -> Any:
    """将覆盖值尽量解析为 JSON 标量。"""

    try:
        return json.loads(raw_value)
    except json.JSONDecodeError:
        return raw_value


def apply_override(config: dict[str, Any], override_expression: str) -> None:
    """将 key=value 覆盖应用到账号配置对象。"""

    if "=" not in override_expression:
        raise RuntimeError(f"账号配置覆盖格式错误: {override_expression}，应为 key=value")

    key_path, raw_value = override_expression.split("=", 1)
    key_parts = [part.strip() for part in key_path.split(".") if part.strip()]
    if not key_parts:
        raise RuntimeError(f"账号配置覆盖键不能为空: {override_expression}")

    current = config
    for key in key_parts[:-1]:
        next_value = current.get(key)
        if next_value is None:
            current[key] = {}
            next_value = current[key]
        if not isinstance(next_value, dict):
            raise RuntimeError(f"账号配置覆盖路径冲突: {override_expression}")
        current = next_value
    current[key_parts[-1]] = parse_override_value(raw_value)


def build_account_config(args: argparse.Namespace) -> str:
    """构造最终账号配置 JSON。"""

    config = parse_json_object(args.account_config, "account-config")

    for override_expression in args.account_config_override:
        apply_override(config, override_expression)

    return json.dumps(config, ensure_ascii=False, separators=(",", ":"))


def build_template_content(args: argparse.Namespace, default_content: dict[str, Any]) -> str:
    """构造最终模板内容 JSON。"""

    if args.template_content:
        content = parse_json_object(args.template_content, "template-content")
    else:
        content = normalize_scalar_strings(default_content)

    for override_expression in args.template_content_override:
        apply_override(content, override_expression)

    return json.dumps(content, ensure_ascii=False, separators=(",", ":"))


def create_account(base_url: str, args: argparse.Namespace, name_prefix: str) -> int:
    """创建一个临时账号，并返回账号 ID。"""

    account_name = f"{name_prefix}-account-{uuid.uuid4().hex[:8]}"
    payload = {
        "name": account_name,
        "sendChannel": args.send_channel,
        "accountConfig": build_account_config(args),
    }
    result = http_json(f"{base_url}/account/save", "POST", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"账号创建失败: {json.dumps(result, ensure_ascii=False)}")

    account_list = http_get_json(
        f"{base_url}/account/queryByChannelType?channelType={args.send_channel}&creator={args.creator}"
    )
    if str(account_list.get("code")) != "200":
        raise RuntimeError(f"账号查询失败: {json.dumps(account_list, ensure_ascii=False)}")

    for account in account_list.get("data") or []:
        if account.get("name") == account_name:
            return int(account["id"])
    raise RuntimeError(f"未找到刚创建的账号: {account_name}")


def delete_account(base_url: str, account_id: int | None) -> None:
    """删除临时账号。"""

    if account_id is None:
        return
    http_json(f"{base_url}/account/delete", "DELETE", [account_id])


def build_template_payload(
    template_id: int,
    name: str,
    send_account_id: int,
    msg_content: str,
    args: argparse.Namespace,
) -> dict:
    """构造模板保存请求。"""

    return {
        "id": template_id,
        "name": name,
        "idType": args.id_type,
        "sendChannel": args.send_channel,
        "templateType": args.template_type,
        "msgType": args.msg_type,
        "shieldType": args.shield_type,
        "sendAccount": send_account_id,
        "msgContent": msg_content,
    }


def create_template(base_url: str, name_prefix: str, send_account_id: int, msg_content: str, args: argparse.Namespace) -> int:
    """创建一个临时模板，并返回模板 ID。"""

    template_id = int(time.time()) + random.randint(1000, 99999)
    payload = build_template_payload(
        template_id,
        f"{name_prefix}-template-{template_id}",
        send_account_id,
        msg_content,
        args,
    )
    result = http_json(f"{base_url}/messageTemplate/add", "POST", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"模板创建失败: {json.dumps(result, ensure_ascii=False)}")
    return template_id


def update_template(
    base_url: str,
    template_id: int,
    name_prefix: str,
    send_account_id: int,
    msg_content: str,
    args: argparse.Namespace,
) -> None:
    """更新现有模板。"""

    payload = build_template_payload(
        template_id,
        f"{name_prefix}-template-{template_id}-{int(time.time())}",
        send_account_id,
        msg_content,
        args,
    )
    result = http_json(f"{base_url}/messageTemplate/update", "POST", payload)
    if str(result.get("code")) != "200":
        raise RuntimeError(f"模板更新失败: {json.dumps(result, ensure_ascii=False)}")


def delete_template(base_url: str, template_id: int | None) -> None:
    """删除临时模板。"""

    if template_id is None:
        return
    http_json(f"{base_url}/messageTemplate/delete/{template_id}", "DELETE")


def send_message(
    base_url: str,
    template_id: int,
    receiver: str,
    biz_id: str,
    idempotency_key: str,
    variables: dict,
    extra: dict,
) -> dict:
    """调用真实 /send 接口。"""

    payload = {
        "code": "send",
        "messageTemplateId": template_id,
        "idempotencyKey": idempotency_key,
        "messageParam": {
            "bizId": biz_id,
            "receiver": receiver,
            "variables": variables,
            "extra": extra,
        },
    }
    return http_json(f"{base_url}/send", "POST", payload)


def extract_message_ids(send_response: dict) -> list[str]:
    """从发送响应中提取 messageId 列表。"""

    return [
        str(item.get("messageId"))
        for item in (send_response.get("data") or [])
        if item.get("messageId")
    ]


def query_trace_once(base_url: str, message_id: str) -> dict:
    """查询单条消息链路。"""

    return http_json(f"{base_url}/trace/message", "POST", {"messageId": message_id})


def wait_for_trace(base_url: str, message_id: str, timeout_seconds: int, interval_seconds: int) -> dict:
    """轮询链路日志，直到有结果或超时。"""

    deadline = time.time() + timeout_seconds
    latest = {"items": []}
    while time.time() < deadline:
        latest = query_trace_once(base_url, message_id)
        if latest.get("items"):
            return latest
        time.sleep(interval_seconds)
    return latest


def print_trace_result(label: str, message_id: str, trace_result: dict) -> None:
    """打印链路追踪结果。"""

    item_count = len(trace_result.get("items") or [])
    print(f"[{label}] messageId={message_id} trace_items={item_count}")
    print(f"[{label}] trace={json.dumps(trace_result, ensure_ascii=False)}")


def cleanup_resources(base_url: str, resources: ScenarioResources) -> None:
    """统一清理模板和账号。"""

#     delete_template(base_url, resources.template_id)
#     delete_account(base_url, resources.account_id)