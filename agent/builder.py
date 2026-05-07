"""
Cyber Range Builder — main agent loop.

Drives Claude in a tool-use loop to:
  1. Parse user requirements
  2. Generate Terraform configuration files
  3. Provision infrastructure (terraform init → plan → apply)
  4. Return target connection info + security notes
"""
from __future__ import annotations

import json
import os
from collections.abc import Callable
from typing import Any

import anthropic

from .tools import TOOL_DEFINITIONS, dispatch_tool

# Model to use — Sonnet 4.6 balances speed and capability
DEFAULT_MODEL = "claude-sonnet-4-6"

SYSTEM_PROMPT = """\
You are the Cyber Range Builder agent — an expert in infrastructure-as-code and cybersecurity.
Your job is to provision intentionally-vulnerable cloud targets for security research and training.

## Workflow

1. **Parse** the user's request into a clear target specification.
2. **Create a workspace** for this build.
3. **Generate Terraform files** in that workspace:
   - `provider.tf` — cloud provider + backend
   - `variables.tf` — input variables with sensible defaults
   - `network.tf` — VPC/subnet/security groups (open ports as requested)
   - `compute.tf` — VM instance with `user_data` install script
   - `outputs.tf` — public IP, SSH key path, application URLs, default creds
4. **Generate install scripts** referenced by `user_data`.
5. Run `terraform init`, then `terraform validate`, then `terraform apply`.
6. Retrieve outputs and add security notes about the deployed target.

## Terraform style rules

- Use Terraform ≥ 1.5. Always pin provider versions.
- AWS: use `aws` provider, default region variable, key pair resource.
- GCP: use `google` provider, project variable, service account.
- Azure: use `azurerm` provider, resource group.
- Security groups for cyber range targets should allow the requested ports from `0.0.0.0/0`
  (intentionally open — this is a controlled training environment).
- Use `user_data` (AWS) or `metadata_startup_script` (GCP) to run the install script on boot.
- Always output: `public_ip`, `ssh_command`, `app_urls` (list), `default_credentials`.

## Install script rules

- Target Ubuntu 22.04 LTS.
- Use Docker + docker-compose where possible for application isolation.
- Pin image versions to specific CVE-relevant tags when the user requests old/vulnerable versions.
- Write install output to `/var/log/crb_install.log`.

## Security notes

After a successful apply, call `add_security_note` for each relevant item:
- Known CVEs in the deployed software
- Suggested attack vectors / entry points
- Useful tools for testing (e.g., sqlmap, metasploit modules, burp suite)
- Default credentials

## Supported applications (examples)

| App | Docker image | Notes |
|-----|-------------|-------|
| DVWA | `vulnerables/web-dvwa` | PHP/MySQL SQLi, XSS, CSRF, file upload |
| OWASP Juice Shop | `bkimminich/juice-shop` | Modern JS vulns |
| WebGoat | `webgoat/goat-and-wolf` | Java-based |
| Apache Struts 2 (CVE-2017-5638) | `piesecurity/apache-struts2-cve-2017-5638` | RCE via Content-Type |
| WordPress 4.x | `wordpress:4.9` + `mysql:5.7` | Wide attack surface |
| Metasploitable 2 network | custom | Multi-service |
| bWAPP | `raesene/bwapp` | 100+ vulnerabilities |
| Mutillidae II | `webpwnized/mutillidae` | OWASP Top 10 |

Always confirm what you are about to build before calling `run_terraform apply`.
If validation fails, fix the Terraform files and retry — do not give up.
"""


class RangeBuilder:
    def __init__(
        self,
        api_key: str | None = None,
        model: str = DEFAULT_MODEL,
        on_event: Callable[[str, Any], None] | None = None,
    ):
        self.client = anthropic.Anthropic(api_key=api_key or os.environ["ANTHROPIC_API_KEY"])
        self.model = model
        self.on_event = on_event or (lambda event, data: None)
        self._messages: list[dict] = []

    # ── Public API ──────────────────────────────────────────────────────────

    def build(self, user_request: str, dry_run: bool = False) -> dict:
        """
        Run the full build for the given user request.
        Returns the final report dict with outputs and security notes.
        """
        self._messages = []
        request_text = user_request
        if dry_run:
            request_text += "\n\n[DRY RUN: generate all files and run terraform init + validate + plan, but do NOT run terraform apply]"

        self._add_user(request_text)
        return self._run_loop()

    def destroy(self, workspace_id: str) -> dict:
        """Destroy a previously built workspace."""
        self._messages = []
        self._add_user(
            f"Destroy the workspace '{workspace_id}'. "
            "Run `terraform destroy` in that workspace and report the result."
        )
        return self._run_loop()

    # ── Internal loop ───────────────────────────────────────────────────────

    def _run_loop(self) -> dict:
        final_text = ""
        while True:
            self.on_event("llm_call", {"messages": len(self._messages)})
            response = self.client.messages.create(
                model=self.model,
                max_tokens=8192,
                system=SYSTEM_PROMPT,
                tools=TOOL_DEFINITIONS,
                messages=self._messages,
            )
            self._messages.append({"role": "assistant", "content": response.content})

            # Collect text from this turn
            for block in response.content:
                if hasattr(block, "text"):
                    final_text = block.text
                    self.on_event("text", block.text)

            if response.stop_reason == "end_turn":
                break

            if response.stop_reason == "tool_use":
                tool_results = []
                for block in response.content:
                    if block.type != "tool_use":
                        continue
                    self.on_event("tool_call", {"name": block.name, "inputs": block.input})
                    result = dispatch_tool(block.name, block.input)
                    self.on_event("tool_result", {"name": block.name, "result": result})
                    tool_results.append({
                        "type": "tool_result",
                        "tool_use_id": block.id,
                        "content": result.output if result.success else f"ERROR: {result.error}",
                    })
                self._messages.append({"role": "user", "content": tool_results})
                continue

            # Unexpected stop reason
            break

        return {"summary": final_text}
