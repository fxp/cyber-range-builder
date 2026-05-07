#!/usr/bin/env bash
# OWASP Juice Shop — Docker installation
# Ports: 3000 (HTTP)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io curl

systemctl enable --now docker

docker run -d \
  --name juice-shop \
  --restart unless-stopped \
  -p 3000:3000 \
  bkimminich/juice-shop:latest

echo "Juice Shop ready at http://$(curl -s ifconfig.me):3000"
echo "No default creds — register a new account or use: admin@juice-sh.op / admin123"
