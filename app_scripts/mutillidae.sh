#!/usr/bin/env bash
# Mutillidae II — OWASP Top 10 training (100+ vulns)
# Ports: 80 (HTTP)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io curl

systemctl enable --now docker

docker run -d \
  --name mutillidae \
  --restart unless-stopped \
  -p 80:80 \
  webpwnized/mutillidae:latest

PUBLIC_IP=$(curl -s ifconfig.me)
echo "Mutillidae II ready at http://${PUBLIC_IP}"
echo "Default creds: admin / adminpass  (or user: john, pass: monkey)"
