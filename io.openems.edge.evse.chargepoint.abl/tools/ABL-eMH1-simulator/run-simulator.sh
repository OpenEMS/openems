#!/bin/bash
###############################################################################
# ABL Modbus Simulator Launcher
#
# Quick launcher script for the ABL EVCC2/3 Modbus simulator.
# Supports TCP and Serial (ASCII/RTU) modes.
#
# Usage:
#   ./run-simulator.sh tcp [ip] [port] [deviceId]
#   ./run-simulator.sh serial [port] [baudrate] [deviceId]
#   ./run-simulator.sh serial-rtu [port] [baudrate] [deviceId]
###############################################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_DIR="$PROJECT_ROOT/test"

echo -e "${GREEN}ABL Modbus Simulator Launcher${NC}"
echo "========================================"

# Find j2mod library
echo "Looking for j2mod library..."
J2MOD_JAR=""

# Check common locations
SEARCH_PATHS=(
    "$HOME/.m2/repository/com/ghgande/j2mod"
    "$HOME/.gradle/caches/modules-2/files-2.1/com.ghgande/j2mod"
    "/usr/share/java"
)

for path in "${SEARCH_PATHS[@]}"; do
    if [ -d "$path" ]; then
        J2MOD_JAR=$(find "$path" -name "j2mod*.jar" 2>/dev/null | head -1)
        if [ -n "$J2MOD_JAR" ]; then
            echo -e "${GREEN}Found j2mod: $J2MOD_JAR${NC}"
            break
        fi
    fi
done

if [ -z "$J2MOD_JAR" ]; then
    echo -e "${RED}ERROR: j2mod library not found!${NC}"
    echo "Please install it via Maven or Gradle:"
    echo "  mvn dependency:get -Dartifact=com.ghgande:j2mod:3.2.1"
    echo "Or download from: https://repo1.maven.org/maven2/com/ghgande/j2mod/"
    exit 1
fi

# Build classpath
CLASSPATH="$TEST_DIR:$J2MOD_JAR"

# Parse command line - if no args or first arg looks like a port, use ASCII mode as default
FIRST_ARG="${1:-}"

# Default to ASCII mode with ABL spec settings (38400 8E1) if no mode specified
if [ -z "$FIRST_ARG" ] || [[ "$FIRST_ARG" =~ ^/dev/ ]] || [[ "$FIRST_ARG" =~ ^COM ]]; then
    # Default ASCII mode
    MODE="ascii"
    if [ "$(uname)" == "Linux" ]; then
        ADDRESS="${1:-/dev/ttyUSB0}"
    else
        ADDRESS="${1:-COM3}"
    fi
    PORT="0"
    BAUDRATE="${2:-38400}"  # ABL spec default
    DEVICE_ID="${3:-1}"
    SIMULATOR_MODE="SERIAL_ASCII"

    # Check serial port permissions on Linux
    if [ "$(uname)" == "Linux" ] && [ -e "$ADDRESS" ]; then
        if [ ! -r "$ADDRESS" ] || [ ! -w "$ADDRESS" ]; then
            echo -e "${YELLOW}WARNING: No permission for $ADDRESS${NC}"
            echo "Try: sudo chmod 666 $ADDRESS"
            echo "Or: sudo usermod -a -G dialout \$USER (then log out/in)"
            echo ""
        fi
    fi
else
    # Explicit mode specified
    MODE="$1"

    case "$MODE" in
        tcp)
            SIMULATOR_MODE="TCP"
            ADDRESS="${2:-127.0.0.1}"
            PORT="${3:-502}"
            BAUDRATE="0"
            DEVICE_ID="${4:-1}"
            ;;
        ascii)
            SIMULATOR_MODE="SERIAL_ASCII"
            if [ "$(uname)" == "Linux" ]; then
                ADDRESS="${2:-/dev/ttyUSB0}"
            else
                ADDRESS="${2:-COM3}"
            fi
            PORT="0"
            BAUDRATE="${3:-38400}"  # ABL spec default
            DEVICE_ID="${4:-1}"

            # Check serial port permissions on Linux
            if [ "$(uname)" == "Linux" ] && [ -e "$ADDRESS" ]; then
                if [ ! -r "$ADDRESS" ] || [ ! -w "$ADDRESS" ]; then
                    echo -e "${YELLOW}WARNING: No permission for $ADDRESS${NC}"
                    echo "Try: sudo chmod 666 $ADDRESS"
                    echo "Or: sudo usermod -a -G dialout \$USER (then log out/in)"
                    echo ""
                fi
            fi
            ;;
        rtu)
            SIMULATOR_MODE="SERIAL_RTU"
            if [ "$(uname)" == "Linux" ]; then
                ADDRESS="${2:-/dev/ttyUSB0}"
            else
                ADDRESS="${2:-COM3}"
            fi
            PORT="0"
            BAUDRATE="${3:-9600}"  # Common RTU default
            DEVICE_ID="${4:-1}"
            ;;
        *)
            echo -e "${RED}ERROR: Unknown mode '$MODE'${NC}"
            echo ""
            echo "Usage:"
            echo "  $0                              # ASCII mode with defaults (38400 8E1)"
            echo "  $0 [port]                       # ASCII mode with custom port"
            echo "  $0 [port] [baudrate] [deviceId] # ASCII mode with custom settings"
            echo ""
            echo "  $0 tcp [ip] [port] [deviceId]"
            echo "  $0 ascii [port] [baudrate] [deviceId]"
            echo "  $0 rtu [port] [baudrate] [deviceId]"
            echo ""
            echo "Examples:"
            echo "  $0                              # ASCII on /dev/ttyUSB0 @ 38400"
            echo "  $0 /dev/ttyUSB1                 # ASCII on /dev/ttyUSB1 @ 38400"
            echo "  $0 COM3                         # ASCII on COM3 @ 38400 (Windows)"
            echo "  $0 tcp                          # TCP on localhost:502"
            echo "  $0 tcp 0.0.0.0 502 1           # TCP on all interfaces"
            echo "  $0 ascii /dev/ttyUSB0 38400 1  # Explicit ASCII"
            echo "  $0 rtu /dev/ttyUSB0 9600 1     # RTU mode"
            exit 1
            ;;
    esac
fi

# Display configuration
echo ""
echo "Configuration:"
echo "  Mode:      $SIMULATOR_MODE"
if [ "$SIMULATOR_MODE" == "TCP" ]; then
    echo "  Address:   $ADDRESS:$PORT"
else
    echo "  Port:      $ADDRESS"
    echo "  Baudrate:  $BAUDRATE"
fi
echo "  Device ID: $DEVICE_ID"
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found!${NC}"
    echo "Please install Java 21 or later"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${YELLOW}WARNING: Java version $JAVA_VERSION detected. Java 17+ recommended.${NC}"
fi

# Check if class files exist, if not try to compile
MAIN_CLASS="io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator"
CLASS_FILE="$TEST_DIR/io/openems/edge/evse/chargepoint/abl/simulator/AblModbusSimulator.class"

if [ ! -f "$CLASS_FILE" ]; then
    echo -e "${YELLOW}Class files not found. Attempting to compile...${NC}"

    # Find all Java source files
    cd "$TEST_DIR" || exit 1
    JAVA_FILES=$(find io/openems/edge/evse/chargepoint/abl/simulator -name "*.java" 2>/dev/null)

    if [ -z "$JAVA_FILES" ]; then
        echo -e "${RED}ERROR: Source files not found in $TEST_DIR${NC}"
        exit 1
    fi

    # Compile
    echo "Compiling simulator classes..."
    javac -cp "$CLASSPATH" $JAVA_FILES

    if [ $? -ne 0 ]; then
        echo -e "${RED}ERROR: Compilation failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}Compilation successful${NC}"
fi

# Launch simulator
echo -e "${GREEN}Starting simulator...${NC}"
echo "Press Ctrl+C to stop"
echo ""

cd "$TEST_DIR" || exit 1
java -cp "$CLASSPATH" "$MAIN_CLASS" "$SIMULATOR_MODE" "$ADDRESS" "$PORT" "$BAUDRATE" "$DEVICE_ID"

# Exit code
EXIT_CODE=$?
echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}Simulator stopped normally${NC}"
else
    echo -e "${RED}Simulator exited with error code $EXIT_CODE${NC}"
fi

exit $EXIT_CODE
