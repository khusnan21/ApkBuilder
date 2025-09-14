#!/system/bin/sh

APP_DIR="/data/data/com.nkjayanet.app/files"
SOCKET="$APP_DIR/php.socket"

# Kill php-cgi
pkill -f php-cgi
echo "[shutdown.sh] php-cgi stopped"

# Kill lighttpd
pkill -f lighttpd
echo "[shutdown.sh] lighttpd stopped"

# Cleanup socket
[ -e "$SOCKET" ] && rm -f "$SOCKET"
