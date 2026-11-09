#!/usr/bin/env bash
# Respaldo de EpCore: base principal, base de cada cliente y documentos cargados.
# Pensado para correr cada noche con cron en el servidor, por ejemplo:
#   30 2 * * * cd /opt/epcore && scripts/respaldo.sh >> /var/log/epcore-respaldo.log 2>&1
#
# Lee las credenciales de donde ya las usa EpCore (nunca de la línea de comandos):
#   - base principal: variables DB_URL, DB_USERNAME, DB_PASSWORD (o el archivo .env)
#   - clientes: config/clientes.properties (creado por scripts/alta-cliente.sh)
# Variables opcionales: RESPALDO_DIR (/var/backups/epcore), RETENCION_DIAS (14),
#   EPCORE_ARCHIVOS (uploads), EPCORE_CLIENTES (config/clientes.properties)
set -euo pipefail
umask 077

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
[[ -f "$RAIZ/.env" && -z "${DB_URL:-}" ]] && { set -a; . "$RAIZ/.env"; set +a; }
DESTINO_BASE="${RESPALDO_DIR:-/var/backups/epcore}"
RETENCION="${RETENCION_DIAS:-14}"
ARCHIVOS="${EPCORE_ARCHIVOS:-$RAIZ/uploads}"
CLIENTES="${EPCORE_CLIENTES:-$RAIZ/config/clientes.properties}"
DESTINO="$DESTINO_BASE/$(date +%Y%m%d-%H%M)"
mkdir -p "$DESTINO"

# jdbc:mysql://host:puerto/base?... -> "host puerto base"
partes_url() { sed -E 's#^jdbc:mysql://([^:/?]+)(:([0-9]+))?/([^?]+).*#\1 \3 \4#' <<<"$1"; }

respaldar_base() { # nombre url usuario clave
  read -r host puerto base <<<"$(partes_url "$2")"
  MYSQL_PWD="$4" mysqldump -h "$host" -P "${puerto:-3306}" -u "$3" --single-transaction --quick \
    --routines --triggers --no-tablespaces --set-gtid-purged=OFF "$base" | gzip -9 > "$DESTINO/$1.sql.gz"
  echo "  base $1 ($base): $(du -h "$DESTINO/$1.sql.gz" | cut -f1)"
}

echo "Respaldo EpCore $(date '+%F %T') -> $DESTINO"
respaldar_base principal "${DB_URL:?Falta DB_URL}" "${DB_USERNAME:?Falta DB_USERNAME}" "${DB_PASSWORD:?Falta DB_PASSWORD}"

if [[ -f "$CLIENTES" ]]; then
  prop() { grep -E "^epcore\.multicliente\.clientes\.$1\.$2=" "$CLIENTES" | head -1 | cut -d= -f2-; }
  for id in $(grep -oE '^epcore\.multicliente\.clientes\.[a-z0-9-]+\.url=' "$CLIENTES" | cut -d. -f4 | sort -u); do
    respaldar_base "cliente-$id" "$(prop "$id" url)" "$(prop "$id" usuario)" "$(prop "$id" clave)"
  done
fi

if [[ -d "$ARCHIVOS" ]]; then
  tar -czf - -C "$(dirname "$ARCHIVOS")" "$(basename "$ARCHIVOS")" > "$DESTINO/documentos.tar.gz"
  echo "  documentos: $(du -h "$DESTINO/documentos.tar.gz" | cut -f1)"
fi

( cd "$DESTINO" && sha256sum -- * > SHA256SUMS )
# Prueba rápida de que cada archivo comprimido se puede leer
for f in "$DESTINO"/*.gz; do gzip -t "$f"; done

find "$DESTINO_BASE" -mindepth 1 -maxdepth 1 -type d -mtime +"$RETENCION" -exec rm -rf {} +
echo "Listo. Se conservan los respaldos de los últimos $RETENCION días."
