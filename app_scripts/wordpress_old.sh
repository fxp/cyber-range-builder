#!/usr/bin/env bash
# WordPress 4.9 + MySQL 5.7 — wide attack surface, multiple known vulns
# Ports: 80 (HTTP)
set -euo pipefail
exec > /var/log/crb_install.log 2>&1

apt-get update -y
apt-get install -y docker.io docker-compose-plugin curl

systemctl enable --now docker

mkdir -p /opt/wordpress
cat > /opt/wordpress/docker-compose.yml <<'EOF'
version: "3"
services:
  db:
    image: mysql:5.7
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: wordpress
      MYSQL_USER: wp
      MYSQL_PASSWORD: wppass
    volumes:
      - db_data:/var/lib/mysql

  wordpress:
    image: wordpress:4.9
    restart: unless-stopped
    depends_on: [db]
    ports:
      - "80:80"
    environment:
      WORDPRESS_DB_HOST: db:3306
      WORDPRESS_DB_USER: wp
      WORDPRESS_DB_PASSWORD: wppass
      WORDPRESS_DB_NAME: wordpress

volumes:
  db_data:
EOF

docker compose -f /opt/wordpress/docker-compose.yml up -d

PUBLIC_IP=$(curl -s ifconfig.me)
echo "WordPress 4.9 ready at http://${PUBLIC_IP}"
echo "Admin setup: http://${PUBLIC_IP}/wp-admin/install.php"
echo "DB creds: wp / wppass"
