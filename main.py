#!/usr/bin/env python3
"""
Cyber Range Builder — CLI entry point

Usage:
  python main.py build "DVWA on AWS t3.small in us-east-1"
  python main.py build "WordPress 4.9 + MySQL 5.7 on GCP, expose port 80" --dry-run
  python main.py list
  python main.py destroy dvwa-aws-abc123
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

import click
from dotenv import load_dotenv
from rich.console import Console
from rich.markdown import Markdown
from rich.panel import Panel
from rich.rule import Rule
from rich.syntax import Syntax
from rich.table import Table

load_dotenv()

console = Console()


def make_event_handler(verbose: bool) -> callable:
    """Returns an event handler that renders progress to the console."""
    def handle(event: str, data: object) -> None:
        if event == "llm_call":
            console.print(f"[dim]→ Thinking... (context length: {data['messages']} messages)[/dim]")

        elif event == "tool_call":
            name = data["name"]
            inputs = data["inputs"]
            # Summarize inputs for display
            summary = _summarize_inputs(name, inputs)
            console.print(f"  [bold cyan]⚙ {name}[/bold cyan]  [dim]{summary}[/dim]")

        elif event == "tool_result":
            result = data["result"]
            if not result.success:
                console.print(f"    [bold red]✗ Error:[/bold red] {result.error}")
            elif verbose:
                preview = result.output[:200].replace("\n", " ")
                console.print(f"    [green]✓[/green] [dim]{preview}[/dim]")
            else:
                console.print(f"    [green]✓[/green]")

        elif event == "text":
            # Stream assistant text — only show if non-empty
            text: str = data
            if text.strip():
                console.print(Markdown(text))

    return handle


def _summarize_inputs(tool_name: str, inputs: dict) -> str:
    if tool_name == "write_file":
        lines = inputs.get("content", "").count("\n") + 1
        return f"{inputs.get('filename')}  ({lines} lines)"
    if tool_name == "run_terraform":
        return f"terraform {inputs.get('command')}"
    if tool_name == "create_workspace":
        return inputs.get("name", "")
    if tool_name == "add_security_note":
        return inputs.get("note", "")[:80]
    return str(inputs)[:120]


# ── CLI ──────────────────────────────────────────────────────────────────────

@click.group()
def cli():
    """Cyber Range Builder — AI-powered vulnerable target provisioner."""
    pass


@cli.command()
@click.argument("request")
@click.option("--dry-run", is_flag=True, help="Plan only — do not run terraform apply")
@click.option("--model", default="claude-sonnet-4-6", show_default=True)
@click.option("--verbose", "-v", is_flag=True)
def build(request: str, dry_run: bool, model: str, verbose: bool):
    """Build a cyber range target from a natural-language description.

    \b
    Examples:
      crb build "DVWA on AWS t3.small"
      crb build "Apache Struts2 CVE-2017-5638 on GCP e2-medium" --dry-run
      crb build "WordPress 4.9 + MySQL 5.7 on AWS, expose ports 80 22"
    """
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        console.print("[bold red]Error:[/bold red] ANTHROPIC_API_KEY not set. Copy .env.example to .env and fill it in.")
        sys.exit(1)

    from agent.builder import RangeBuilder  # local import to keep startup fast

    console.print(Rule("[bold green]Cyber Range Builder[/bold green]"))
    console.print(Panel(f"[bold]{request}[/bold]", title="Request", border_style="green"))
    if dry_run:
        console.print("[yellow]DRY RUN mode — terraform apply will not be executed[/yellow]")

    builder = RangeBuilder(
        api_key=api_key,
        model=model,
        on_event=make_event_handler(verbose),
    )

    try:
        result = builder.build(request, dry_run=dry_run)
    except KeyboardInterrupt:
        console.print("\n[yellow]Interrupted.[/yellow]")
        sys.exit(1)
    except Exception as exc:
        console.print(f"[bold red]Build failed:[/bold red] {exc}")
        if verbose:
            console.print_exception()
        sys.exit(1)

    console.print(Rule("[bold green]Done[/bold green]"))


@cli.command("list")
def list_targets():
    """List all existing workspaces."""
    from agent.tools import list_workspaces

    result = list_workspaces()
    workspaces = json.loads(result.output)

    if not workspaces:
        console.print("[dim]No workspaces found.[/dim]")
        return

    table = Table(title="Cyber Range Workspaces", show_lines=True)
    table.add_column("ID", style="cyan")
    table.add_column("Status", style="bold")
    table.add_column("Notes")

    for ws in workspaces:
        ws_id = ws.get("workspace_id", "?")
        status = ws.get("status", "unknown")
        notes = ws.get("notes", [])
        status_color = {
            "running": "green",
            "failed": "red",
            "destroyed": "dim",
            "pending": "yellow",
        }.get(status, "white")
        table.add_row(
            ws_id,
            f"[{status_color}]{status}[/{status_color}]",
            f"{len(notes)} note(s)",
        )

    console.print(table)


@cli.command()
@click.argument("workspace_id")
@click.option("--yes", "-y", is_flag=True, help="Skip confirmation prompt")
@click.option("--model", default="claude-sonnet-4-6", show_default=True)
@click.option("--verbose", "-v", is_flag=True)
def destroy(workspace_id: str, yes: bool, model: str, verbose: bool):
    """Destroy a provisioned workspace and all its cloud resources."""
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        console.print("[bold red]Error:[/bold red] ANTHROPIC_API_KEY not set.")
        sys.exit(1)

    if not yes:
        confirmed = click.confirm(
            f"This will run terraform destroy on '{workspace_id}'. Continue?",
            default=False,
        )
        if not confirmed:
            console.print("Aborted.")
            return

    from agent.builder import RangeBuilder

    console.print(Rule(f"[bold red]Destroying {workspace_id}[/bold red]"))
    builder = RangeBuilder(
        api_key=api_key,
        model=model,
        on_event=make_event_handler(verbose),
    )

    try:
        builder.destroy(workspace_id)
    except Exception as exc:
        console.print(f"[bold red]Destroy failed:[/bold red] {exc}")
        sys.exit(1)

    console.print(Rule("[bold]Done[/bold]"))


@cli.command()
@click.argument("workspace_id")
def info(workspace_id: str):
    """Show info and security notes for a workspace."""
    import json
    from agent.tools import WORKSPACES_ROOT, get_terraform_outputs

    ws_path = WORKSPACES_ROOT / workspace_id
    state_file = ws_path / ".crb_state.json"

    if not state_file.exists():
        console.print(f"[red]Workspace '{workspace_id}' not found.[/red]")
        sys.exit(1)

    state = json.loads(state_file.read_text())

    console.print(Panel(f"[bold]{workspace_id}[/bold]", title="Workspace", border_style="cyan"))

    outputs_result = get_terraform_outputs(workspace_id)
    if outputs_result.success and outputs_result.output.strip():
        try:
            outputs = json.loads(outputs_result.output)
            table = Table(title="Terraform Outputs", show_lines=True)
            table.add_column("Key", style="cyan")
            table.add_column("Value")
            for k, v in outputs.items():
                table.add_row(k, str(v))
            console.print(table)
        except json.JSONDecodeError:
            console.print(outputs_result.output)

    notes = state.get("notes", [])
    if notes:
        console.print(Panel("\n".join(f"• {n}" for n in notes), title="Security Notes", border_style="yellow"))


if __name__ == "__main__":
    cli()
