#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
umask 077
mkdir -p .runtime

if [ -e .runtime/dev.env ]; then
    printf '%s\n' 'Using existing .runtime/dev.env'
    exit 0
fi

db_password=$(openssl rand -hex 24)
root_password=$(openssl rand -hex 24)
jwt_secret=$(openssl rand -hex 32)
seed_password=$(openssl rand -hex 20)

cat > .runtime/dev.env <<ENV
MYSQL_ROOT_PASSWORD=$root_password
MYSQL_DATABASE=cafe_management_dev
MYSQL_USER=cafepulse_dev
MYSQL_PASSWORD=$db_password
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3307/cafe_management_dev
SPRING_DATASOURCE_USERNAME=cafepulse_dev
SPRING_DATASOURCE_PASSWORD=$db_password
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8081
CAFE_CORS_ORIGIN=http://localhost:5174
VITE_API_BASE_URL=http://localhost:8081
CAFE_JWT_SECRET=$jwt_secret
CAFE_DEV_SEED_PASSWORD=$seed_password
ENV
printf '%s\n' 'Created .runtime/dev.env'
