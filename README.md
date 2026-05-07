# Cyber Range Builder

AI-powered provisioner for vulnerable cloud targets used in security research and training.
Describe what you want in plain language — the agent writes Terraform, provisions the infrastructure, and returns connection info and security notes.

> **For authorized security research only.** The generated infrastructure is intentionally vulnerable.
> Always deploy in isolated, private cloud environments and destroy when done.

## How it works

```
You:   "DVWA on AWS t3.small"
Agent: generates provider.tf, network.tf, compute.tf, scripts/install.sh
       → terraform init → validate → apply
       → outputs: public_ip, ssh_command, app_url, default_creds
       → security notes: SQLi entry points, CVEs, suggested tools
```

The agent is a Claude-powered loop using tool use to write files and run Terraform commands.
No rigid templates — the agent adapts to any requirement you describe.

## Prerequisites

| Tool | Version |
|------|---------|
| Python | ≥ 3.11 |
| Terraform | ≥ 1.5 |
| Docker | any (on target VM, not host) |
| Cloud CLI | aws / gcloud / az (credentials configured) |

## Quick start

```bash
# 1. Clone and install
git clone https://github.com/YOUR_ORG/cyber-range-builder.git
cd cyber-range-builder
pip install -e .            # or: uv pip install -e .

# 2. Configure
cp .env.example .env
# Edit .env — set ANTHROPIC_API_KEY and cloud credentials

# 3. Build a target
python main.py build "DVWA on AWS t3.small in us-east-1"

# Dry-run (no apply)
python main.py build "Apache Struts2 CVE-2017-5638 on GCP" --dry-run

# 4. List targets
python main.py list

# 5. Show info / security notes
python main.py info <workspace_id>

# 6. Destroy when done
python main.py destroy <workspace_id>
```

## Example requests

```
"DVWA on AWS t3.small, expose ports 80 and 22"
"OWASP Juice Shop on GCP e2-medium in europe-west1"
"WordPress 4.9 + MySQL 5.7 on AWS, I want to practice SQLi and auth bypass"
"Apache Struts 2 with CVE-2017-5638 on AWS t3.micro — S2-045 RCE lab"
"WebGoat + WebWolf on Azure Standard_B2s"
"Log4Shell CVE-2021-44228 demo environment on AWS"
"Mutillidae II on GCP, open ports 80 22 443"
```

## Project structure

```
cyber-range-builder/
├── main.py                  # CLI: build / list / destroy / info
├── agent/
│   ├── builder.py           # Claude agent loop (tool-use driven)
│   ├── tools.py             # Tool implementations
│   └── models.py            # Pydantic data models
├── tf_templates/            # Reference Terraform snippets (Jinja2)
│   ├── providers/           # aws / gcp / azure provider configs
│   ├── network/             # VPC, subnets, security groups
│   └── compute/             # VM instances
├── app_scripts/             # Docker-based install scripts per target app
│   ├── dvwa.sh
│   ├── juice_shop.sh
│   ├── struts2_cve_2017_5638.sh
│   ├── wordpress_old.sh
│   ├── webgoat.sh
│   ├── mutillidae.sh
│   └── log4shell_cve_2021_44228.sh
└── workspaces/              # Runtime-generated (gitignored)
    └── <workspace_id>/
        ├── provider.tf
        ├── variables.tf
        ├── network.tf
        ├── compute.tf
        ├── outputs.tf
        ├── scripts/install.sh
        └── .crb_state.json
```

## Extending with new targets

1. Add an install script to `app_scripts/your_app.sh`
2. Describe the app in your build request — the agent will reference the script and generate appropriate Terraform
3. If you need a new cloud provider, add a template to `tf_templates/providers/`

## Architecture

The agent is built on the Anthropic Claude API with tool use:

```
User request
    │
    ▼
RangeBuilder.build()
    │
    ▼  (loop)
Claude (claude-sonnet-4-6)
    │  decides which tools to call
    ├─ create_workspace      → creates workspaces/<id>/
    ├─ write_file            → writes .tf files and scripts
    ├─ run_terraform init    → initializes providers
    ├─ run_terraform validate→ syntax check
    ├─ run_terraform apply   → provisions cloud resources
    ├─ get_terraform_outputs → retrieves IPs, URLs, creds
    └─ add_security_note     → records CVEs, attack vectors
    │
    ▼
Final report: connection info + security notes
```

## Security & responsible use

- Generated targets are **intentionally vulnerable** — deploy in isolated environments only
- Always use a dedicated cloud account/project separate from production
- Tag resources as `Project=cyber-range` and set budget alerts
- Run `python main.py destroy <id>` when done to avoid ongoing charges
- Never expose these targets to the public internet without proper network-level isolation

## License

MIT
