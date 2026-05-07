#!/usr/bin/env bash
# DVWA (Damn Vulnerable Web Application) — Docker installation
# Ports: 80 (HTTP)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io docker-compose-plugin curl

systemctl enable --now docker

mkdir -p /opt/dvwa
cat > /opt/dvwa/docker-compose.yml <<'EOF'
version: "3"
services:
  dvwa:
    image: vulnerables/web-dvwa:latest
    restart: unless-stopped
    ports:
      - "80:80"
    environment:
      - MYSQL_PASS=p@ssw0rd
EOF

docker compose -f /opt/dvwa/docker-compose.yml up -d

echo "DVWA ready at http://$(curl -s ifconfig.me)"
echo "Default creds: admin / password"
echo "Setup URL:     http://$(curl -s ifconfig.me)/setup.php"
