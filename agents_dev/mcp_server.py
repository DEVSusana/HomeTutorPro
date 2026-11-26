#!/usr/bin/env python3
"""
Minimal, zero-dependency MCP (Model Context Protocol) stdio server for HomeTutorPro.
Exposes local project tools (Hilt binding check, Kotlin metrics audit, spec listing)
over standard JSON-RPC 2.0 so any MCP client (Antigravity, Claude Code, Cursor, etc.)
can discover and invoke them natively.
"""

import contextlib
import io
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

AGENTS_DEV_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = AGENTS_DEV_DIR.parent
if str(AGENTS_DEV_DIR) not in sys.path:
    sys.path.insert(0, str(AGENTS_DEV_DIR))

from skills.check_code_metrics import audit_file_sizes  # noqa: E402
from skills.hilt_checker import check_hilt_bindings  # noqa: E402

PROTOCOL_VERSION = "2024-11-05"

TOOLS: List[Dict[str, Any]] = [
    {
        "name": "check_hilt_bindings",
        "description": "Verify whether a UseCase or Repository Kotlin file has its Hilt binding registered in di/.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file_path": {
                    "type": "string",
                    "description": "Relative or absolute path to the UseCase or Repository .kt file.",
                }
            },
            "required": ["file_path"],
        },
    },
    {
        "name": "check_code_metrics",
        "description": "Audit Kotlin production files against the 300-line AGENTS.md limit.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "target_dir": {
                    "type": "string",
                    "description": "Directory relative to project root (defaults to app/src/main/java).",
                }
            },
        },
    },
    {
        "name": "list_specs",
        "description": "List available feature specifications in specs/ with their current status.",
        "inputSchema": {
            "type": "object",
            "properties": {},
        },
    },
]


def _tool_list_specs() -> str:
    specs_dir = PROJECT_ROOT / "specs"
    if not specs_dir.exists():
        return "No specs/ directory found."

    entries = []
    for path in sorted(specs_dir.glob("*.md")):
        if path.name == "_TEMPLATE.md":
            continue
        first_lines = path.read_text(encoding="utf-8", errors="ignore").splitlines()[:8]
        title = next((l.lstrip("# ").strip() for l in first_lines if l.startswith("#")), path.name)
        status = next((l.strip() for l in first_lines if "Estado:" in l), "Estado: unknown")
        entries.append(f"- {path.relative_to(PROJECT_ROOT)} | {title} | {status}")

    return "\n".join(entries) if entries else "No specs found in specs/."


def _tool_check_code_metrics(target_dir: str = "app/src/main/java") -> str:
    full_dir = PROJECT_ROOT / target_dir
    if not full_dir.exists():
        return f"ERROR: Directory '{target_dir}' does not exist."
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        audit_file_sizes(str(full_dir))
    return buf.getvalue().strip()


def call_tool(name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
    """Dispatches an MCP tools/call request and formats the MCP content response."""
    try:
        if name == "check_hilt_bindings":
            file_path = arguments.get("file_path", "")
            text = check_hilt_bindings(file_path)
        elif name == "check_code_metrics":
            target_dir = arguments.get("target_dir") or "app/src/main/java"
            text = _tool_check_code_metrics(target_dir)
        elif name == "list_specs":
            text = _tool_list_specs()
        else:
            return {
                "content": [{"type": "text", "text": f"Unknown tool: {name}"}],
                "isError": True,
            }
        return {"content": [{"type": "text", "text": text}], "isError": False}
    except Exception as exc:
        return {"content": [{"type": "text", "text": f"Tool error: {exc}"}], "isError": True}


def handle_request(request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Handles a single JSON-RPC 2.0 MCP request; returns None for notifications."""
    req_id = request.get("id")
    method = request.get("method", "")
    params = request.get("params") or {}

    # Notifications have no id and expect no response
    if req_id is None:
        return None

    if method == "initialize":
        result = {
            "protocolVersion": PROTOCOL_VERSION,
            "capabilities": {"tools": {}},
            "serverInfo": {"name": "hometutorpro-agents", "version": "0.1.0"},
        }
    elif method == "ping":
        result = {}
    elif method == "tools/list":
        result = {"tools": TOOLS}
    elif method == "tools/call":
        result = call_tool(params.get("name", ""), params.get("arguments") or {})
    else:
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "error": {"code": -32601, "message": f"Method not found: {method}"},
        }

    return {"jsonrpc": "2.0", "id": req_id, "result": result}


def main() -> None:
    """Runs the newline-delimited JSON-RPC 2.0 stdio loop."""
    for raw_line in sys.stdin:
        line = raw_line.strip()
        if not line:
            continue
        try:
            request = json.loads(line)
        except json.JSONDecodeError as exc:
            err = {
                "jsonrpc": "2.0",
                "id": None,
                "error": {"code": -32700, "message": f"Parse error: {exc}"},
            }
            sys.stdout.write(json.dumps(err) + "\n")
            sys.stdout.flush()
            continue

        response = handle_request(request)
        if response is not None:
            sys.stdout.write(json.dumps(response) + "\n")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
