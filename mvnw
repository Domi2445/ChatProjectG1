#!/bin/sh
# Maven Wrapper script for Linux/macOS
# Downloads Maven automatically if not present

set -e

MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-3.9.9"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"

if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
    echo "Downloading Maven 3.9.9..."
    mkdir -p "${MAVEN_HOME}"
    TEMP_FILE=$(mktemp /tmp/maven-XXXXXX.zip)
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL -o "${TEMP_FILE}" "${MAVEN_URL}"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "${TEMP_FILE}" "${MAVEN_URL}"
    else
        echo "Error: curl or wget required to download Maven" >&2
        exit 1
    fi
    unzip -q -o "${TEMP_FILE}" -d "${MAVEN_HOME}/.."
    rm -f "${TEMP_FILE}"
    echo "Maven 3.9.9 installed."
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
