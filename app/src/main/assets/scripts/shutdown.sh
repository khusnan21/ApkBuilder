#!/system/bin/sh

# 1. Base directory = parent folder dari scripts
BASE="$(dirname "$0")/.."
BIN="$BASE/bin"
BUSYBOX="$BIN/busybox"

# 2. Hentikan PHP FastCGI & Lighttpd
#   - SIGTERM mengizinkan proses cleanup sebelum mati
"$BUSYBOX" killall -SIGTERM php-cgi   2>/dev/null
"$BUSYBOX" killall -SIGTERM lighttpd 2>/dev/null

# 3. (Opsional) Bersihkan tmp socket/logs
rm -rf "$BASE/tmp"/* 2>/dev/null

exit 0
