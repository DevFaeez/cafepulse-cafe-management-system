#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ $# -ne 2 ]]; then
    printf 'Usage: bash dev/backup-db.sh MYSQL_CONTAINER OUTPUT.sql.gz\n' >&2
    exit 2
fi

container=$1
output=$2
dock=${DOCKER_CMD:-docker}
if [[ $output != *.sql.gz || -e $output || -L $output ]]; then
    printf 'Choose a new .sql.gz path for the backup.\n' >&2
    exit 2
fi

mkdir -p "$(dirname "$output")"
temporary=$(mktemp "$(dirname "$output")/.cafepulse-backup.XXXXXX")
trap 'rm -f "$temporary"' EXIT

"$dock" exec "$container" sh -eu -c '
    export MYSQL_PWD="$MYSQL_PASSWORD"
    exec mysqldump --single-transaction --no-tablespaces --set-gtid-purged=OFF --no-create-db --skip-add-drop-table \
      --ignore-table="$MYSQL_DATABASE.login_session" -u "$MYSQL_USER" "$MYSQL_DATABASE"
' | gzip -c > "$temporary"
gzip -t "$temporary"
mv -n "$temporary" "$output"
if [[ -e $temporary ]]; then
    printf 'Backup path was created by another process. Choose a new file.\n' >&2
    exit 1
fi
printf '%s\n' "$output"
