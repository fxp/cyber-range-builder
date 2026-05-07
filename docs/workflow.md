# Cyber Range Builder — 完整构建流程

本文档记录从用户输入一条自然语言请求，到云端靶场目标完全就绪的每一个步骤。

---

## 总览

```
用户输入
  "DVWA on AWS t3.small in us-east-1"
        │
        ▼
  ┌─────────────┐
  │  CLI (main) │  解析参数、读取 .env、初始化 RangeBuilder
  └──────┬──────┘
         │
         ▼
  ┌──────────────────────────────────────┐
  │  Agent 循环 (builder.py)             │
  │                                      │
  │  Claude ──工具调用──► 工具实现        │
  │    ▲                   │             │
  │    └────── 工具结果 ───┘             │
  └──────────────────────────────────────┘
         │
         ▼
  工作区目录 workspaces/<id>/
    provider.tf / network.tf / compute.tf
    variables.tf / outputs.tf
    scripts/install.sh
         │
         ▼
  terraform init → validate → apply
         │
         ▼
  输出报告
    public_ip / ssh_command
    app_urls / default_creds
    安全分析注记 (CVEs、攻击向量、推荐工具)
```

---

## 第一阶段：CLI 入口

**文件：** `main.py` → `cli()` → `build()`

1. 用户执行：
   ```bash
   python main.py build "DVWA on AWS t3.small in us-east-1"
   ```

2. Click 解析参数：
   - `request` — 自然语言描述
   - `--dry-run` — 若设置，跳过 `terraform apply`
   - `--model` — 默认 `claude-sonnet-4-6`

3. 从 `.env` 读取 `ANTHROPIC_API_KEY` 及云凭据（`AWS_*` / `GOOGLE_*` / `ARM_*`）。

4. 实例化 `RangeBuilder`，注入 `on_event` 回调用于向终端输出进度。

5. 调用 `builder.build(request, dry_run=dry_run)`。

---

## 第二阶段：Agent 主循环

**文件：** `agent/builder.py` → `RangeBuilder._run_loop()`

Agent 循环遵循标准的 Claude tool-use 多轮对话模式：

```
while True:
    response = claude.messages.create(
        system=SYSTEM_PROMPT,
        tools=TOOL_DEFINITIONS,
        messages=conversation
    )

    if stop_reason == "end_turn":
        break                          # 任务完成

    if stop_reason == "tool_use":
        for tool_call in response:
            result = dispatch_tool(tool_call.name, tool_call.inputs)
        conversation.append(tool_results)
        continue                       # 将工具结果送回 Claude，继续下一轮
```

**System Prompt** 告知 Claude：
- 它是 Cyber Range Builder，专注于安全研究靶场
- 必须按顺序执行：解析需求 → 创建工作区 → 生成 Terraform 文件 → 生成安装脚本 → 运行 Terraform → 收集输出 → 记录安全注记
- Terraform 文件的风格规范（provider 版本锁定、安全组全开、user_data 引用方式）
- 安装脚本规范（Ubuntu 22.04 + Docker、写日志到 `/var/log/crb_install.log`）
- 验证失败时必须自行修复，不得放弃

---

## 第三阶段：工具调用序列

Claude 在一次典型构建中会依次调用以下工具（`agent/tools.py`）：

### 3.1 `create_workspace`

```python
create_workspace(name="dvwa-aws")
# → 返回: {"workspace_id": "dvwa-aws-a3f8c2", "path": "workspaces/dvwa-aws-a3f8c2"}
```

- 在 `workspaces/` 下创建以随机后缀区分的目录
- 写入 `.crb_state.json` 记录初始状态

### 3.2 `write_file` × 5（Terraform 文件）

Claude 逐一生成并写入以下文件：

| 文件 | 内容 |
|------|------|
| `provider.tf` | Terraform 版本约束 + AWS/GCP/Azure provider，版本锁定 |
| `variables.tf` | `region`、`instance_type`、`target_name` 等变量及默认值 |
| `network.tf` | VPC、子网、Internet Gateway、路由表、安全组（按需开放端口） |
| `compute.tf` | TLS 密钥对 + EC2/VM 实例，`user_data` 指向安装脚本 |
| `outputs.tf` | `public_ip`、`ssh_command`、`app_urls`、`default_credentials` |

**安全组生成示例（AWS）：**

```hcl
# 靶场环境：对 0.0.0.0/0 开放请求的端口（有意为之）
ingress {
  from_port   = 80
  to_port     = 80
  protocol    = "tcp"
  cidr_blocks = ["0.0.0.0/0"]
}
```

### 3.3 `write_file`（安装脚本）

写入 `scripts/install.sh`，内容来自 `app_scripts/<app>.sh` 模板，或由 Claude 根据需求即时生成。脚本在 VM 首次启动时通过 `user_data`（AWS）/ `metadata_startup_script`（GCP）执行：

```bash
#!/usr/bin/env bash
exec > /var/log/crb_install.log 2>&1
apt-get update -y
apt-get install -y docker.io docker-compose-plugin
systemctl enable --now docker
docker run -d --name dvwa -p 80:80 vulnerables/web-dvwa
```

### 3.4 `run_terraform init`

```
terraform init
```

- 下载 provider 插件（`hashicorp/aws`、`hashicorp/tls` 等）
- 初始化本地 backend

### 3.5 `run_terraform validate`

```
terraform validate
```

- 语法检查和引用验证
- **若失败**：Claude 读取错误信息，调用 `write_file` 修复对应 `.tf` 文件，再次 validate，最多重试若干轮

### 3.6 `run_terraform plan`（dry-run 模式下终止于此）

```
terraform plan
```

- 显示将要创建的资源列表
- dry-run 模式下跳过后续 apply

### 3.7 `run_terraform apply`（正式构建）

```
terraform apply -auto-approve
```

- 依序创建：密钥对 → VPC → 子网 → 安全组 → EC2 实例
- EC2 实例启动后 `user_data` 在后台异步执行安装脚本（约 2–5 分钟）
- Terraform 完成后即返回，应用可能仍在初始化中

### 3.8 `get_terraform_outputs`

```
terraform output -json
```

解析并展平输出：

```json
{
  "public_ip": "54.123.45.67",
  "ssh_command": "ssh -i dvwa-aws.pem ubuntu@54.123.45.67",
  "app_urls": ["http://54.123.45.67/"],
  "default_credentials": "admin / password"
}
```

### 3.9 `add_security_note` × N

Claude 根据部署的应用自动添加安全分析注记，例如：

```
CVE: DVWA 包含 SQLi（Low/Medium/High 难度级别），入口：/vulnerabilities/sqli/
工具: sqlmap -u "http://IP/vulnerabilities/sqli/?id=1&Submit=Submit" --cookie="..."
默认凭据: admin / password（首次访问需点击 /setup.php 初始化数据库）
攻击向量: 文件上传 → webshell（/vulnerabilities/upload/）
```

---

## 第四阶段：工作区状态

每个工作区目录结构：

```
workspaces/dvwa-aws-a3f8c2/
├── provider.tf
├── variables.tf
├── network.tf
├── compute.tf
├── outputs.tf
├── scripts/
│   └── install.sh
├── dvwa-aws-a3f8c2.pem       # 自动生成的 SSH 私钥（chmod 600）
├── .terraform/                # provider 插件缓存（gitignore）
├── terraform.tfstate          # Terraform 状态（gitignore）
└── .crb_state.json            # CRB 自用状态（status + notes）
```

`.crb_state.json` 示例：

```json
{
  "workspace_id": "dvwa-aws-a3f8c2",
  "status": "running",
  "notes": [
    "CVE: SQLi at /vulnerabilities/sqli/ — use sqlmap to exploit",
    "Default creds: admin / password",
    "Setup required: visit /setup.php on first access"
  ]
}
```

---

## 第五阶段：最终报告

Agent 循环以 `end_turn` 结束后，CLI 渲染最终报告：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━ Done ━━━━━━━━━━━━━━━━━━━━━━━━━━

Workspace: dvwa-aws-a3f8c2

┌─ Terraform Outputs ───────────────────────────────────┐
│ public_ip           54.123.45.67                      │
│ ssh_command         ssh -i dvwa-aws.pem ubuntu@...    │
│ app_urls            http://54.123.45.67/              │
│ default_credentials admin / password                  │
└───────────────────────────────────────────────────────┘

┌─ Security Notes ──────────────────────────────────────┐
│ • SQLi at /vulnerabilities/sqli/ — use sqlmap         │
│ • Default creds: admin / password                     │
│ • XSS at /vulnerabilities/xss_r/                      │
│ • File upload → webshell: /vulnerabilities/upload/    │
└───────────────────────────────────────────────────────┘
```

---

## 错误处理与重试

| 场景 | 处理方式 |
|------|---------|
| `terraform validate` 失败 | Claude 读取错误，修复 `.tf` 文件，重新 validate |
| `terraform apply` 失败 | Claude 分析错误（权限/配额/资源冲突），提示用户检查云凭据或修改参数 |
| terraform 未安装 | 工具返回明确错误：`terraform binary not found`，指引安装 |
| 命令超时（>10分钟） | 工具返回超时错误，Claude 报告并建议重试 |

---

## 销毁流程

```bash
python main.py destroy dvwa-aws-a3f8c2
```

1. 二次确认提示（`--yes` 跳过）
2. Agent 调用 `run_terraform destroy`（`-auto-approve`）
3. 按依赖反序删除：EC2 → 安全组 → 子网 → VPC → 密钥对
4. 更新 `.crb_state.json` 状态为 `destroyed`

---

## 数据流图（完整）

```
用户
 │  python main.py build "DVWA on AWS t3.small"
 ▼
main.py
 │  RangeBuilder(api_key, model, on_event)
 │  builder.build(request)
 ▼
Anthropic API  ◄────────────────────────────────────┐
 │  [system: SYSTEM_PROMPT]                          │
 │  [tools: TOOL_DEFINITIONS]                        │
 │  [user: request]                                  │
 │                                                   │
 │  stop_reason="tool_use"                           │
 ▼                                                   │
agent/tools.py                                       │
 ├─ create_workspace()  → workspaces/id/             │
 ├─ write_file()        → provider.tf                │
 ├─ write_file()        → variables.tf               │
 ├─ write_file()        → network.tf                 │
 ├─ write_file()        → compute.tf                 │
 ├─ write_file()        → outputs.tf                 │
 ├─ write_file()        → scripts/install.sh         │
 ├─ run_terraform(init)    → subprocess(terraform)   │
 ├─ run_terraform(validate)→ subprocess(terraform)   │
 ├─ run_terraform(apply)   → subprocess(terraform)──►│ 云 API
 ├─ get_terraform_outputs()→ subprocess(terraform)   │  (AWS/GCP/Azure)
 └─ add_security_note() × N → .crb_state.json        │
                                                      │
 tool_results ──────────────────────────────────────►┘
                                                      │
                                          stop_reason="end_turn"
                                                      │
                                                      ▼
                                              final report
```
