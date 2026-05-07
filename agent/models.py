from __future__ import annotations
from enum import Enum
from pathlib import Path
from typing import Any
from pydantic import BaseModel, Field


class CloudProvider(str, Enum):
    AWS = "aws"
    GCP = "gcp"
    AZURE = "azure"


class WorkspaceStatus(str, Enum):
    PENDING = "pending"
    PLANNING = "planning"
    PROVISIONING = "provisioning"
    RUNNING = "running"
    FAILED = "failed"
    DESTROYED = "destroyed"


class RangeTarget(BaseModel):
    """Parsed description of the cyber range target to build."""
    name: str = Field(description="Short identifier for this target, e.g. 'dvwa-aws-01'")
    description: str = Field(description="Human-readable description of what will be built")
    provider: CloudProvider = Field(description="Cloud provider to use")
    region: str = Field(description="Cloud region, e.g. 'us-east-1'")
    instance_type: str = Field(description="VM size, e.g. 't3.small' for AWS")
    applications: list[str] = Field(default_factory=list, description="Applications to install")
    open_ports: list[int] = Field(default_factory=list, description="Ports to expose")
    extra_requirements: list[str] = Field(default_factory=list, description="Additional constraints from user")
    os_image: str = Field(default="ubuntu-22.04", description="Base OS image identifier")
    tags: dict[str, str] = Field(default_factory=dict)


class WorkspaceState(BaseModel):
    """Tracks the state of a single build workspace."""
    workspace_id: str
    target: RangeTarget
    status: WorkspaceStatus = WorkspaceStatus.PENDING
    workspace_dir: str = ""
    terraform_outputs: dict[str, Any] = Field(default_factory=dict)
    error: str | None = None
    notes: list[str] = Field(default_factory=list, description="Security analysis notes")


class ToolResult(BaseModel):
    success: bool
    output: str
    error: str | None = None
