#!/system/bin/sh

# 1. Base dirs
BASE="$(dirname "$0")/.."
BIN="$BASE/bin"
CONF="$BASE/conf/lighttpd.conf"
WWW="$BASE/www"
TMP="$BASE/tmp"

# 2. Izin dan tmp
chmod +x "$BIN/"* 2>/dev/null
mkdir -p "$TMP"

# 3. PHP FastCGI ← sesuaikan bin-path php-cgi
"$BIN/php-cgi" -b 127.0.0.1:9000 \
    -c "$BASE/conf/php.ini" &

# 4. Lighttpd
"$BIN/lighttpd" -f "$CONF" &

exit 0