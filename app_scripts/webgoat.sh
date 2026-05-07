#!/usr/bin/env bash
# WebGoat + WebWolf — OWASP Java-based security training
# Ports: 8080 (WebGoat), 9090 (WebWolf)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io docker-compose-plugin curl

systemctl enable --now docker

mkdir -p /opt/webgoat
cat > /opt/webgoat/docker-compose.yml <<'EOF'
version: "3"
services:
  webgoat:
    image: webgoat/goat-and-wolf:latest
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "9090:9090"
    environment:
      WEBGOAT_PORT: 8080
      WEBWOLF_PORT: 9090
EOF

docker compose -f /opt/webgoat/docker-compose.yml up -d

PUBLIC_IP=$(curl -s ifconfig.me)
echo "WebGoat ready at http://${PUBLIC_IP}:8080/WebGoat"
echo "WebWolf ready at http://${PUBLIC_IP}:9090/WebWolf"
echo "Register a new account at first login"
