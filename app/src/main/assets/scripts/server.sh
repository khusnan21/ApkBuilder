#!/system/bin/sh

APP_DIR="/data/data/com.nkjayanet.app/files"
BIN_DIR="$APP_DIR/bin"
CONF_DIR="$APP_DIR/conf"
WWW_DIR="$APP_DIR/www"
SOCKET="$APP_DIR/php.socket"

# Kill existing socket if any
[ -e "$SOCKET" ] && rm -f "$SOCKET"

# Start PHP-CGI as FastCGI backend
"$BIN_DIR/php-cgi" -b "$SOCKET" &
echo "[server.sh] php-cgi started on socket $SOCKET"

# Start Lighttpd with config
"$BIN_DIR/lighttpd" -f "$CONF_DIR/lighttpd.conf"
echo "[server.sh] lighttpd started with config $CONF_DIR/lighttpd.conf"
