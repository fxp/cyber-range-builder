#!/usr/bin/env bash
# Apache Struts 2 — CVE-2017-5638 (S2-045) RCE via Content-Type header
# Ports: 8080 (HTTP)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io curl

systemctl enable --now docker

# piesecurity image bundles the vulnerable Struts 2.3.5 showcase app
docker run -d \
  --name struts2-s2045 \
  --restart unless-stopped \
  -p 8080:8080 \
  piesecurity/apache-struts2-cve-2017-5638:latest

PUBLIC_IP=$(curl -s ifconfig.me)
echo "Struts2 CVE-2017-5638 target ready"
echo "URL:  http://${PUBLIC_IP}:8080/showcase.action"
echo "CVE:  CVE-2017-5638 (S2-045)"
echo "Test: curl -v -H 'Content-Type: %{(#_=@OgnlContext@DEFAULT_MEMBER_ACCESS)...}' http://${PUBLIC_IP}:8080/showcase.action"
