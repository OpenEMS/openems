#!/usr/bin/env bash
# modpoll_j2mod.sh — Run j2mod's ModPoll using the jar built by ./gradlew buildEdge
#
# This script uses the build environment of OpenEMS to compile and run j2mod's ModPoll utility.
# It supports:
# -m ascii      Modbus ASCII protocol (with messages starting with `:`)
# -m rtu        Modbus RTU protocol (default if SERIALPORT contains /, \ or COM)
# -m tcp        MODBUS/TCP protocol (default otherwise)
# -m udp        MODBUS UDP
# -m enc        Encapsulated Modbus RTU over TCP
#
# All Modbus/serial parameters are passed through directly to ModPoll.
# Java classpath boilerplate is handled internally.
#
# Expected behavior (no problem, as no logging needed for cli use):
# SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
# SLF4J: Defaulting to no-operation (NOP) logger implementation
# SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
#
# Usage:
#   modpoll_j2mod.sh [ModPoll options] <serial-port>
#   
#   for help just: 
#   modpoll_j2mod.sh 
#
# Examples: 
#   (Alfen, Modbus TCP):
#   ./modpoll_j2mod.sh -m tcp -a 200 -r 100 -c 79 -t 4 10.62.100.1
#
#   (Fronius, Modbus TCP):
#   ./modpoll_j2mod.sh -m tcp -v -1 -a 1 -r 40071 -c 60 -t 4 10.62.195.5
#
#   (ABL eMH1, Modbus ASCII -> cannot work, as ABL answers with `>` instead of the expected standard `:`):
#   ./modpoll_j2mod.sh -m ascii -b 38400 -d 8 -s 1 -p even -a 1 -r 1 -c 2 -t 4:hex -o 2.0 -1 /dev/ttyUSB0
#
# Common ModPoll options:
#   -m <ascii|rtu|tcp|udp|enc>   Modbus mode
#   -b <baud>            Baud rate
#   -d <bits>            Data bits (5..8)
#   -s <bits>            Stop bits (1|2)
#   -p <parity>          Parity (even|odd|none)
#   -a <addr>            Slave device address (1..247)
#   -r <reg>             First register to poll (1-based)
#   -c <count>           Number of registers to poll
#   -t <type>            Register type (1=coil, 2=discrete, 3=input, 4=holding; append :hex for hex output)
#   -o <secs>            Response timeout in seconds (float)
#   -1                   Poll once and exit (default: continuous)
#
# Required parameters (no auto-detect), e.g.
#   -t 4
#
# Prerequisites:
#   Run './gradlew buildEdge' from the repository root at least once.
#
# Caching:
#   The j2mod jar is extracted from EdgeApp.jar into .modpoll_cache/ next to this
#   script and reused on subsequent calls. It is re-extracted automatically whenever
#   EdgeApp.jar is newer than the cached copy.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Locate repository root via git
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"

# Find EdgeApp.jar produced by ./gradlew buildEdge (search generically under repo root)
EDGE_APP_JAR="$(find "$REPO_ROOT" \
    -name "EdgeApp.jar" \
    -path "*/distributions/executable/EdgeApp.jar" \
    2>/dev/null | head -1)"

if [[ -z "$EDGE_APP_JAR" ]]; then
    echo "ERROR: EdgeApp.jar not found under $REPO_ROOT" >&2
    echo "       Run './gradlew buildEdge' from the repository root first." >&2
    exit 1
fi

# Cache directory for extracted jars
CACHE_DIR="$REPO_ROOT/tools/build/modpoll_cache"
mkdir -p "$CACHE_DIR"

# j2mod and its runtime dependencies are all bundled in EdgeApp.jar under jar/.
# Extract each pattern, cache by filename, re-extract when EdgeApp.jar is newer.
EDGE_APP_CONTENTS="$(jar tf "$EDGE_APP_JAR")"

extract_jar() {
    local pattern="$1"
    local entry
    entry="$(echo "$EDGE_APP_CONTENTS" | grep -E "$pattern" | head -1)"
    if [[ -z "$entry" ]]; then
        echo "ERROR: No jar matching '$pattern' found inside $EDGE_APP_JAR" >&2
        exit 1
    fi
    local filename cached
    filename="$(basename "$entry")"
    cached="$CACHE_DIR/$filename"
    if [[ ! -f "$cached" || "$EDGE_APP_JAR" -nt "$cached" ]]; then
        echo "[modpoll_j2mod] Extracting $filename from EdgeApp.jar ..." >&2
        local tmp
        tmp="$(mktemp -d)"
        trap 'rm -rf "$tmp"' RETURN
        (cd "$tmp" && jar xf "$EDGE_APP_JAR" "$entry")
        cp "$tmp/$entry" "$cached"
    fi
    echo "$cached"
}

J2MOD_JAR="$(extract_jar '^jar/j2mod.*\.jar$')"
JSERIALCOMM_JAR="$(extract_jar '^jar/jSerialComm.*\.jar$')"

# slf4j-api is required for serial mode (SerialConnection.<clinit> calls LoggerFactory directly).
# Search: cache dir first, then gradle, then maven local.
SLF4J_API_JAR="$(find "$CACHE_DIR" -name "slf4j-api-*.jar" ! -name "*sources*" 2>/dev/null | sort -V | tail -1)"
if [[ -z "$SLF4J_API_JAR" ]]; then
    SLF4J_API_JAR="$(find "$HOME/.gradle" \
        -path "*/org.slf4j/slf4j-api/*/slf4j-api-*.jar" ! -name "*sources*" \
        2>/dev/null | sort -V | tail -1)"
fi
if [[ -z "$SLF4J_API_JAR" ]]; then
    SLF4J_API_JAR="$(find "$HOME/.m2/repository/org/slf4j/slf4j-api" \
        -name "slf4j-api-*.jar" ! -name "*sources*" 2>/dev/null | sort -V | tail -1)"
fi
if [[ -z "$SLF4J_API_JAR" ]]; then
    echo "ERROR: slf4j-api jar not found — serial mode will not work." >&2
    echo "       Place a slf4j-api-*.jar in $CACHE_DIR or run './gradlew buildEdge' to populate the gradle cache." >&2
    exit 1
fi

# Symlink slf4j-api into the cache so it's visible alongside j2mod/jSerialComm
# and it's clear where it came from (the symlink target shows the origin).
SLF4J_LINK="$CACHE_DIR/$(basename "$SLF4J_API_JAR")"
if [[ ! -L "$SLF4J_LINK" ]]; then
    ln -sf "$SLF4J_API_JAR" "$SLF4J_LINK"
fi
SLF4J_API_JAR="$SLF4J_LINK"

CP="$J2MOD_JAR:$JSERIALCOMM_JAR"
[[ -n "$SLF4J_API_JAR" ]] && CP="$CP:$SLF4J_API_JAR"

exec java -cp "$CP" com.ghgande.j2mod.modbus.util.ModPoll "$@"
