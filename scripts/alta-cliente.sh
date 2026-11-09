#!/usr/bin/env bash
# Da de alta un cliente nuevo de EpCore con su propia base de datos (versión Linux).
#   1. Crea la base epcore_<id> y un usuario de MySQL con permisos SOLO sobre ella
#      (pide la contraseña de root de MySQL; el script no la guarda).
#   2. Genera claves aleatorias para ese usuario y para el primer administrador.
#   3. Agrega el cliente a config/clientes.properties (archivo local, fuera de Git).
# Al reiniciar EpCore, las migraciones crean las tablas, roles, catálogos y el administrador.
#
# Uso: scripts/alta-cliente.sh <id> "<Nombre>" <correo-admin> [host-mysql] [puerto] [host-usuario]
# Ej.: scripts/alta-cliente.sh bancocafetero "Banco Cafetero" accesos@bancocafetero.co
set -euo pipefail

ID="${1:?Falta el identificador (subdominio)}"
NOMBRE="${2:?Falta el nombre del cliente}"
ADMIN="${3:?Falta el correo del administrador}"
MYSQL_HOST="${4:-localhost}"
MYSQL_PUERTO="${5:-3306}"
HOST_USUARIO="${6:-localhost}"   # '%' si EpCore corre en otro contenedor o servidor
ARCHIVO="${EPCORE_CLIENTES:-$(cd "$(dirname "$0")/.." && pwd)/config/clientes.properties}"

[[ "$ID" =~ ^[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$ && "$ID" != "principal" ]] \
  || { echo "Identificador no válido: minúsculas, números y guion." >&2; exit 1; }
[[ "$ADMIN" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]] || { echo "Correo no válido." >&2; exit 1; }
if [[ -f "$ARCHIVO" ]] && grep -q "^epcore\.multicliente\.clientes\.$ID\." "$ARCHIVO"; then
  echo "El cliente '$ID' ya está en $ARCHIVO." >&2; exit 1
fi

clave() { LC_ALL=C tr -dc 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789' < /dev/urandom | head -c "$1"; }
BASE="epcore_${ID//-/_}"
USUARIO="epc_${ID//-/_}"; USUARIO="${USUARIO:0:32}"
CLAVE_BD="$(clave 24)"
CLAVE_ADMIN="$(clave 16)"

echo "Se creará la base '$BASE' y el usuario '$USUARIO'@'$HOST_USUARIO' en $MYSQL_HOST:$MYSQL_PUERTO."
if [[ -z "${MYSQL_PWD:-}" ]]; then read -rsp "Contraseña de root de MySQL (no se muestra ni se guarda): " MYSQL_PWD; echo; fi
MYSQL_PWD="$MYSQL_PWD" mysql -h "$MYSQL_HOST" -P "$MYSQL_PUERTO" -u root <<SQL || { echo "MySQL no pudo crear la base. No se cambió la configuración." >&2; exit 1; }
CREATE DATABASE $BASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER '$USUARIO'@'$HOST_USUARIO' IDENTIFIED BY '$CLAVE_BD';
GRANT ALL PRIVILEGES ON $BASE.* TO '$USUARIO'@'$HOST_USUARIO';
SQL

mkdir -p "$(dirname "$ARCHIVO")"
umask 077
if [[ ! -f "$ARCHIVO" ]] || ! grep -q "^epcore\.multicliente\.habilitado=" "$ARCHIVO"; then
  printf '# Clientes de EpCore. ARCHIVO LOCAL CON CLAVES: no se sube a Git.\nepcore.multicliente.habilitado=true\n' >> "$ARCHIVO"
fi
# Nombre con tildes: Java .properties lo lee bien escapado como \uXXXX
NOMBRE_ESC="$(printf '%s' "$NOMBRE" | python3 -c 'import sys; print("".join(c if ord(c) < 127 else chr(92) + "u%04x" % ord(c) for c in sys.stdin.buffer.read().decode("utf-8")))')"
P="epcore.multicliente.clientes.$ID"
cat >> "$ARCHIVO" <<EOF

# $ID - alta $(date +%F)
$P.nombre=$NOMBRE_ESC
$P.url=jdbc:mysql://$MYSQL_HOST:$MYSQL_PUERTO/$BASE?sslMode=PREFERRED&serverTimezone=America/Bogota
$P.usuario=$USUARIO
$P.clave=$CLAVE_BD
$P.admin-correo=$ADMIN
$P.admin-clave=$CLAVE_ADMIN
EOF
chmod 600 "$ARCHIVO"

echo
echo "Cliente '$ID' dado de alta."
echo "  Configuración: $ARCHIVO"
echo "  Primer administrador: $ADMIN"
echo "  Clave inicial (entréguela por un canal seguro y pida cambiarla): $CLAVE_ADMIN"
echo
echo "Siguiente paso: reinicie EpCore. Las tablas, roles, catálogos y el administrador se crean solos."
echo "El cliente entra por https://$ID.<dominio-base> (configure ese subdominio en el DNS)."
