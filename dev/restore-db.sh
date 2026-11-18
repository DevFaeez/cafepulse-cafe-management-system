#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
    printf 'Usage: bash dev/restore-db.sh EMPTY_MYSQL_CONTAINER BACKUP.sql.gz\n' >&2
    exit 2
fi

container=$1
input=$2
dock=${DOCKER_CMD:-docker}
gzip -t "$input"

tables=$("$dock" exec "$container" sh -eu -c '
    export MYSQL_PWD="$MYSQL_PASSWORD"
    exec mysql -u "$MYSQL_USER" -D "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"
')
if [[ $tables != 0 ]]; then
    printf 'Restore requires an empty target schema. Found %s tables.\n' "$tables" >&2
    exit 1
fi

gzip -dc "$input" | "$dock" exec -i "$container" sh -eu -c '
    export MYSQL_PWD="$MYSQL_PASSWORD"
    exec mysql -u "$MYSQL_USER" "$MYSQL_DATABASE"
'
printf 'Restore finished in %s. Start CafePulse against this database to verify data.\n' "$container"
