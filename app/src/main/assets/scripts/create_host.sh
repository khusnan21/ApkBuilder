#!/system/bin/sh

# 1. Base directories (relatif ke posisi scripts)
BASE="$(dirname "$0")/.."
BIN="$BASE/bin"
SCRIPTS="$BASE/scripts"
CONF_DIR="$BASE/conf"
WWW_DIR="$BASE/www"

# 2. Argumen: nama host, port, lokasi (relatif ke WWW_DIR atau absolut)
NAME="$1"
PORT="${2:-8080}"
LOCATION_ARG="$3"
if [ -z "$LOCATION_ARG" ]; then
  LOCATION="$WWW_DIR"
elif [ "${LOCATION_ARG#/}" = "$LOCATION_ARG" ]; then
  LOCATION="$WWW_DIR/$LOCATION_ARG"
else
  LOCATION="$LOCATION_ARG"
fi

# 3. Paths template & output
TEMPLATE="$SCRIPTS/lighttpd_vhost.template"
OUTPUT="$CONF_DIR/lighttpd_vhost_${NAME}.conf"

# 4. Copy template → output
cp "$TEMPLATE" "$OUTPUT"
chmod 644 "$OUTPUT"

# 5. Ganti placeholder %PORT% & %LOCATION%
"$BIN/busybox" sed -i "s|%PORT%|$PORT|g"      "$OUTPUT"
"$BIN/busybox" sed -i "s|%LOCATION%|$LOCATION|g" "$OUTPUT"

echo "1"
