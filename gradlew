#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"

GRADLE_VERSION="8.13"
GRADLE_HOME="${HOME}/.gradle/ohecompat/gradle-${GRADLE_VERSION}"
GRADLE_ZIP="${HOME}/.gradle/ohecompat/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -x "${GRADLE_HOME}/bin/gradle" ]; then
  mkdir -p "${HOME}/.gradle/ohecompat"
  echo "[OHE] Downloading official Gradle ${GRADLE_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "${GRADLE_URL}" -o "${GRADLE_ZIP}"
  else
    wget -O "${GRADLE_ZIP}" "${GRADLE_URL}"
  fi
  unzip -q -o "${GRADLE_ZIP}" -d "${HOME}/.gradle/ohecompat"
fi

exec "${GRADLE_HOME}/bin/gradle" "$@"
