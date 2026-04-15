#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:?usage: prepare-android-curl.sh <releaseExecutableDir>}"
BASE_URL="https://packages.termux.dev/apt/termux-main"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

download_and_extract() {
  local relative_url="$1"
  local name="$2"
  local deb_path="$WORK_DIR/${name}.deb"
  local extract_dir="$WORK_DIR/$name"
  curl -L "${BASE_URL}/${relative_url}" -o "$deb_path"
  mkdir -p "$extract_dir"
  dpkg-deb -x "$deb_path" "$extract_dir"
}

copy_file() {
  local source_path="$1"
  local target_path="$2"
  mkdir -p "$(dirname "$target_path")"
  install -m 755 "$source_path" "$target_path"
}

copy_text_file() {
  local source_path="$1"
  local target_path="$2"
  mkdir -p "$(dirname "$target_path")"
  install -m 644 "$source_path" "$target_path"
}

download_and_extract "pool/main/c/curl/curl_8.19.0_aarch64.deb" "curl"
download_and_extract "pool/main/libc/libcurl/libcurl_8.19.0_aarch64.deb" "libcurl"
download_and_extract "pool/main/o/openssl/openssl_1:3.6.2_aarch64.deb" "openssl"
download_and_extract "pool/main/c/ca-certificates/ca-certificates_1:2026.03.19_all.deb" "ca-certificates"
download_and_extract "pool/main/libn/libnghttp2/libnghttp2_1.68.1_aarch64.deb" "libnghttp2"
download_and_extract "pool/main/libn/libnghttp3/libnghttp3_1.15.0_aarch64.deb" "libnghttp3"
download_and_extract "pool/main/libn/libngtcp2/libngtcp2_1.22.0_aarch64.deb" "libngtcp2"
download_and_extract "pool/main/libs/libssh2/libssh2_1.11.1-1_aarch64.deb" "libssh2"
download_and_extract "pool/main/z/zlib/zlib_1.3.2_aarch64.deb" "zlib"

TERMUX_ROOT="$WORK_DIR/curl/data/data/com.termux/files/usr"

copy_file "$WORK_DIR/curl/data/data/com.termux/files/usr/bin/curl" "$OUT_DIR/curl.bin"

copy_file "$WORK_DIR/libcurl/data/data/com.termux/files/usr/lib/libcurl.so" "$OUT_DIR/lib/libcurl.so"
copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/libssl.so.3" "$OUT_DIR/lib/libssl.so.3"
copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/libssl.so.3" "$OUT_DIR/lib/libssl.so"
copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/libcrypto.so.3" "$OUT_DIR/lib/libcrypto.so.3"
copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/libcrypto.so.3" "$OUT_DIR/lib/libcrypto.so"
copy_file "$WORK_DIR/libnghttp2/data/data/com.termux/files/usr/lib/libnghttp2.so" "$OUT_DIR/lib/libnghttp2.so"
copy_file "$WORK_DIR/libnghttp3/data/data/com.termux/files/usr/lib/libnghttp3.so" "$OUT_DIR/lib/libnghttp3.so"
copy_file "$WORK_DIR/libngtcp2/data/data/com.termux/files/usr/lib/libngtcp2.so" "$OUT_DIR/lib/libngtcp2.so"
copy_file "$WORK_DIR/libngtcp2/data/data/com.termux/files/usr/lib/libngtcp2_crypto_ossl.so" "$OUT_DIR/lib/libngtcp2_crypto_ossl.so"
copy_file "$WORK_DIR/libssh2/data/data/com.termux/files/usr/lib/libssh2.so" "$OUT_DIR/lib/libssh2.so"
copy_file "$WORK_DIR/zlib/data/data/com.termux/files/usr/lib/libz.so.1.3.2" "$OUT_DIR/lib/libz.so.1.3.2"
copy_file "$WORK_DIR/zlib/data/data/com.termux/files/usr/lib/libz.so.1.3.2" "$OUT_DIR/lib/libz.so.1"
copy_file "$WORK_DIR/zlib/data/data/com.termux/files/usr/lib/libz.so.1.3.2" "$OUT_DIR/lib/libz.so"

copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/engines-3/capi.so" "$OUT_DIR/lib/engines-3/capi.so"
copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/engines-3/loader_attic.so" "$OUT_DIR/lib/engines-3/loader_attic.so"
copy_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/lib/ossl-modules/legacy.so" "$OUT_DIR/lib/ossl-modules/legacy.so"
copy_text_file "$WORK_DIR/openssl/data/data/com.termux/files/usr/etc/tls/openssl.cnf" "$OUT_DIR/etc/tls/openssl.cnf"
copy_text_file "$WORK_DIR/ca-certificates/data/data/com.termux/files/usr/etc/tls/cert.pem" "$OUT_DIR/etc/tls/cert.pem"

cat > "$OUT_DIR/curl" <<'EOF'
#!/system/bin/sh
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
export LD_LIBRARY_PATH="$DIR/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export SSL_CERT_FILE="$DIR/etc/tls/cert.pem"
export CURL_CA_BUNDLE="$DIR/etc/tls/cert.pem"
export OPENSSL_CONF="$DIR/etc/tls/openssl.cnf"
export OPENSSL_MODULES="$DIR/lib/ossl-modules"
exec "$DIR/curl.bin" "$@"
EOF

chmod 755 "$OUT_DIR/curl"

echo "Bundled Android curl into $OUT_DIR"
