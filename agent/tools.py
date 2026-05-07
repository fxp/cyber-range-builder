"""
Tool implementations for the Cyber Range Builder agent.

Each function here corresponds to a tool Claude can invoke.
Tools write Terraform files, run Terraform commands, and manage workspaces.
"""
from __future__ import annotations

import json
import os
import subprocess
import uuid
from pathlib import Path

from .models import ToolResult

WORKSPACES_ROOT = Path(__file__).parent.parent / "workspaces"
WORKSPACES_ROOT.mkdir(exist_ok=True)

# ── Tool schemas (passed to Claude) ────────────────────────────────────────

TOOL_DEFINITIONS = [
    {
        "name": "create_workspace",
        "description": (
            "Create a new Terraform workspace directory for a build. "
            "Returns the workspace_id and absolute path."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "name": {
                    "type": "string",
                    "description": "Short slug for the workspace, e.g. 'dvwa-aws'",
                }
            },
            "required": ["name"],
        },
    },
    {
        "name": "write_file",
        "description": (
            "Write a file inside a workspace directory. "
            "Use this to create .tf files, shell scripts, or any config files."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "workspace_id": {"type": "string"},
                "filename": {
                    "type": "string",
                    "description": "Relative path inside the workspace, e.g. 'main.tf' or 'scripts/install.sh'",
                },
                "content": {"type": "string", "description": "Full file content"},
            },
            "required": ["workspace_id", "filename", "content"],
        },
    },
    {
        "name": "read_file",
        "description": "Read the content of a file inside a workspace.",
        "input_schema": {
            "type": "object",
            "properties": {
                "workspace_id": {"type": "string"},
                "filename": {"type": "string"},
            },
            "required": ["workspace_id", "filename"],
        },
    },
    {
        "name": "list_workspace_files",
        "description": "List all files currently in a workspace directory.",
        "input_schema": {
            "type": "object",
            "properties": {"workspace_id": {"type": "string"}},
            "required": ["workspace_id"],
        },
    },
    {
        "name": "run_terraform",
        "description": (
            "Execute a Terraform command inside a workspace. "
            "Supported commands: 'init', 'validate', 'plan', 'apply', 'output', 'destroy'. "
            "'apply' and 'destroy' are run with -auto-approve."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "workspace_id": {"type": "string"},
                "command": {
                    "type": "string",
                    "enum": ["init", "validate", "plan", "apply", "output", "destroy"],
                },
                "extra_args": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Extra flags, e.g. ['-var', 'region=us-east-1']",
                },
            },
            "required": ["workspace_id", "command"],
        },
    },
    {
        "name": "get_terraform_outputs",
        "description": "Return all Terraform outputs as a JSON object after a successful apply.",
        "input_schema": {
            "type": "object",
            "properties": {"workspace_id": {"type": "string"}},
            "required": ["workspace_id"],
        },
    },
    {
        "name": "list_workspaces",
        "description": "List all existing workspaces with their status.",
        "input_schema": {"type": "object", "properties": {}},
    },
    {
        "name": "add_security_note",
        "description": (
            "Record a security analysis note for this target, e.g. known CVEs, "
            "suggested attack vectors, relevant tools. These are included in the final report."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "workspace_id": {"type": "string"},
                "note": {"type": "string"},
            },
            "required": ["workspace_id", "note"],
        },
    },
]


# ── Tool implementations ────────────────────────────────────────────────────

def _workspace_path(workspace_id: str) -> Path:
    return WORKSPACES_ROOT / workspace_id


def create_workspace(name: str) -> ToolResult:
    workspace_id = f"{name}-{uuid.uuid4().hex[:6]}"
    path = _workspace_path(workspace_id)
    path.mkdir(parents=True, exist_ok=True)
    # Write state file
    state = {"workspace_id": workspace_id, "status": "pending", "notes": []}
    (path / ".crb_state.json").write_text(json.dumps(state, indent=2))
    return ToolResult(
        success=True,
        output=json.dumps({"workspace_id": workspace_id, "path": str(path)}),
    )


def write_file(workspace_id: str, filename: str, content: str) -> ToolResult:
    path = _workspace_path(workspace_id) / filename
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    # Make shell scripts executable
    if filename.endswith(".sh"):
        path.chmod(0o755)
    return ToolResult(success=True, output=f"Written {len(content)} bytes to {filename}")


def read_file(workspace_id: str, filename: str) -> ToolResult:
    path = _workspace_path(workspace_id) / filename
    if not path.exists():
        return ToolResult(success=False, output="", error=f"{filename} not found")
    return ToolResult(success=True, output=path.read_text())


def list_workspace_files(workspace_id: str) -> ToolResult:
    path = _workspace_path(workspace_id)
    if not path.exists():
        return ToolResult(success=False, output="", error="Workspace not found")
    files = [
        str(p.relative_to(path))
        for p in sorted(path.rglob("*"))
        if p.is_file() and not p.name.startswith(".")
    ]
    return ToolResult(success=True, output=json.dumps(files))


def run_terraform(workspace_id: str, command: str, extra_args: list[str] | None = None) -> ToolResult:
    path = _workspace_path(workspace_id)
    if not path.exists():
        return ToolResult(success=False, output="", error="Workspace not found")

    cmd = ["terraform", command]
    if command in ("apply", "destroy"):
        cmd.append("-auto-approve")
    if command == "output":
        cmd.extend(["-json"])
    if extra_args:
        cmd.extend(extra_args)

    env = {**os.environ, "TF_IN_AUTOMATION": "true"}
    try:
        result = subprocess.run(
            cmd,
            cwd=path,
            capture_output=True,
            text=True,
            timeout=600,
            env=env,
        )
        combined = result.stdout + ("\n" + result.stderr if result.stderr else "")
        if result.returncode != 0:
            return ToolResult(success=False, output=combined, error=f"Exit code {result.returncode}")
        return ToolResult(success=True, output=combined)
    except FileNotFoundError:
        return ToolResult(
            success=False,
            output="",
            error="terraform binary not found — install Terraform and ensure it is on PATH",
        )
    except subprocess.TimeoutExpired:
        return ToolResult(success=False, output="", error="Terraform command timed out (>10m)")


def get_terraform_outputs(workspace_id: str) -> ToolResult:
    result = run_terraform(workspace_id, "output")
    if not result.success:
        return result
    try:
        outputs = json.loads(result.output)
        # Flatten {key: {value: ..., type: ...}} -> {key: value}
        flat = {k: v.get("value", v) for k, v in outputs.items()}
        return ToolResult(success=True, output=json.dumps(flat, indent=2))
    except json.JSONDecodeError:
        return ToolResult(success=True, output=result.output)


def list_workspaces() -> ToolResult:
    if not WORKSPACES_ROOT.exists():
        return ToolResult(success=True, output=json.dumps([]))
    items = []
    for d in sorted(WORKSPACES_ROOT.iterdir()):
        if not d.is_dir():
            continue
        state_file = d / ".crb_state.json"
        if state_file.exists():
            state = json.loads(state_file.read_text())
        else:
            state = {"workspace_id": d.name, "status": "unknown"}
        items.append(state)
    return ToolResult(success=True, output=json.dumps(items, indent=2))


def add_security_note(workspace_id: str, note: str) -> ToolResult:
    path = _workspace_path(workspace_id) / ".crb_state.json"
    if not path.exists():
        return ToolResult(success=False, output="", error="Workspace not found")
    state = json.loads(path.read_text())
    state.setdefault("notes", []).append(note)
    path.write_text(json.dumps(state, indent=2))
    return ToolResult(success=True, output=f"Note added: {note[:80]}")


# ── Dispatch ────────────────────────────────────────────────────────────────

TOOL_HANDLERS: dict[str, callable] = {
    "create_workspace": lambda i: create_workspace(**i),
    "write_file": lambda i: write_file(**i),
    "read_file": lambda i: read_file(**i),
    "list_workspace_files": lambda i: list_workspace_files(**i),
    "run_terraform": lambda i: run_terraform(**i),
    "get_terraform_outputs": lambda i: get_terraform_outputs(**i),
    "list_workspaces": lambda i: list_workspaces(),
    "add_security_note": lambda i: add_security_note(**i),
}


def dispatch_tool(name: str, inputs: dict) -> ToolResult:
    handler = TOOL_HANDLERS.get(name)
    if handler is None:
        return ToolResult(success=False, output="", error=f"Unknown tool: {name}")
    return handler(inputs)
