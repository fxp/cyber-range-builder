# Example Build Requests

## Web Application Targets

```bash
# DVWA — classic SQLi, XSS, CSRF, file upload
python main.py build "DVWA on AWS t3.small in us-east-1"

# OWASP Juice Shop — modern JS/Angular vulnerabilities
python main.py build "OWASP Juice Shop on GCP e2-medium in us-central1"

# WebGoat — Java-based, OWASP Top 10 lessons
python main.py build "WebGoat and WebWolf on AWS t3.small"

# Mutillidae II — 100+ vulnerabilities
python main.py build "Mutillidae II on Azure Standard_B2s in eastus"

# WordPress attack surface
python main.py build "WordPress 4.9 with MySQL 5.7 on AWS t3.small, expose ports 80 22"
```

## CVE-Specific Labs

```bash
# S2-045 — Apache Struts 2 RCE via Content-Type (EternalBlue-class web RCE)
python main.py build "Apache Struts2 CVE-2017-5638 lab on AWS t3.micro"

# Log4Shell — JNDI injection via log messages
python main.py build "Log4Shell CVE-2021-44228 demo on AWS t3.micro"

# Spring4Shell
python main.py build "Spring4Shell CVE-2022-22965 lab on GCP e2-micro"
```

## Custom Configurations

```bash
# Multi-port, specific region
python main.py build "DVWA on AWS t3.medium in ap-southeast-1, expose ports 22 80 443 3306"

# Dry run to review Terraform before applying
python main.py build "Juice Shop on GCP" --dry-run

# Multiple apps on one host
python main.py build "DVWA and Juice Shop both on AWS t3.medium, use Ubuntu 22.04"
```
