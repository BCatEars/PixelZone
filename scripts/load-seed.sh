#!/usr/bin/env bash
#
# Carga el esquema y los datos de PixelZone forzando utf8mb4 en el cliente.
#
# Por que: el cliente `mysql` cae en latin1_swedish_ci cuando LANG/LC_ALL estan
# vacios, y entonces los acentos del seed se guardan doble-codificados
# (Público -> PÃºblico). Con --default-character-set=utf8mb4 no ocurre.
#
# Uso:
#   ./scripts/load-seed.sh                      # root@localhost; pide contrasena
#   PZ_PASSWORD=secreto ./scripts/load-seed.sh  # sin prompt
#   ./scripts/load-seed.sh root 127.0.0.1       # usuario y host
#
# Nota: para backups usa tambien `mysqldump --default-character-set=utf8mb4`.
#
set -euo pipefail

export LANG="${LANG:-C.UTF-8}"
export LC_ALL="${LC_ALL:-C.UTF-8}"

USUARIO="${1:-root}"
HOST="${2:-localhost}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_DIR="$RAIZ/src/main/resources"

OPCIONES=(--default-character-set=utf8mb4 -u "$USUARIO" -h "$HOST")
if [[ -n "${PZ_PASSWORD:-}" ]]; then
    OPCIONES+=("-p${PZ_PASSWORD}")
else
    OPCIONES+=("-p")
fi

for script in PZ_DDL.sql PZ_DML.sql PZ_PL.sql; do
    echo ">> Cargando $script"
    mysql "${OPCIONES[@]}" < "$SQL_DIR/$script"
done

echo ">> Listo: base 'pixel_zone' cargada en utf8mb4."
