#!/bin/bash
# Instala el Android SDK mínimo para compilar Cita Huellas.
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$SDK/cmdline-tools"
  tmp=$(mktemp -d)
  curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o "$tmp/ct.zip"
  unzip -q "$tmp/ct.zip" -d "$tmp"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platforms;android-35" "build-tools;35.0.0" "platform-tools" >/dev/null 2>&1
echo "SDK listo en $SDK"
