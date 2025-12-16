# ABL Modbus Simulator - Serial Gateway Setup Guide

This guide explains how to set up the ABL Modbus simulator with a hardware Modbus gateway for realistic testing.

## Overview

```
┌──────────────┐  USB-Serial  ┌─────────────┐  Ethernet  ┌──────────────┐
│   Simulator  │──────────────│   Modbus    │────────────│  OpenEMS     │
│   (ASCII)    │   RS-485     │   Gateway   │   TCP      │   Edge       │
└──────────────┘              └─────────────┘            └──────────────┘
 /dev/ttyUSB0                  192.168.1.100             192.168.1.50
 38400 baud 8E1                Port 502                  Bridge.Modbus.Tcp
```

## Hardware Requirements

### 1. USB-to-RS485 Adapter

Recommended models:
- **FTDI-based adapters** (FT232RL chip)
  - Most reliable, best Linux support
  - Example: "FTDI USB to RS485 Converter"

- **CH340-based adapters**
  - Low cost, good compatibility
  - May require driver installation

- **CP2102-based adapters**
  - Good performance, Linux-friendly

**Features needed:**
- ✅ 2-wire or 4-wire RS-485
- ✅ Automatic flow control (DE/RE)
- ✅ 120Ω termination resistor (optional but recommended)

### 2. Modbus Gateway (RS-485 to Ethernet)

Recommended models:
- **Moxa NPort 5110** - Industrial grade, reliable
- **USR-TCP232-410S** - Budget-friendly, good performance
- **Waveshare RS485 to Ethernet** - Easy to configure
- **Phoenix Contact PSI-MODEM** - Industrial applications

**Required features:**
- ✅ RS-485 serial interface
- ✅ Ethernet TCP/IP interface
- ✅ Modbus TCP support
- ✅ Configurable via web interface

### 3. Cabling

**RS-485 Wiring:**
```
USB-RS485 Adapter          Modbus Gateway
┌────────────────┐        ┌────────────────┐
│                │        │                │
│   A (D+)  ●────┼────────┼───●  A (D+)   │
│   B (D-)  ●────┼────────┼───●  B (D-)   │
│   GND     ●────┼────────┼───●  GND      │
│                │        │                │
└────────────────┘        └────────────────┘
```

**Important:**
- Connect A to A, B to B (or D+ to D+, D- to D-)
- Use **twisted pair cable** for RS-485 (Cat5/Cat6 works)
- Maximum cable length: 1200 meters (4000 feet)
- For testing: 1-2 meters is fine
- Add 120Ω termination at both ends for long cables

**Typical wire colors (if using Cat5 cable):**
- A/D+ → Orange or Blue
- B/D- → Orange-White or Blue-White
- GND → Brown or Green

## Software Setup

### Step 1: Install Dependencies

**On Linux:**
```bash
# Check if USB-serial driver is loaded
lsmod | grep usbserial

# If not loaded, load it
sudo modprobe usbserial

# For CH340 chips specifically
sudo modprobe ch341

# Install serial port tools (optional, for debugging)
sudo apt-get install setserial minicom
```

**On Windows:**
- Install USB-serial driver from manufacturer
- Check Device Manager for COM port number

### Step 2: Find Serial Port

**Linux:**
```bash
# List all serial ports
ls -l /dev/ttyUSB* /dev/ttyACM*

# Expected output:
# /dev/ttyUSB0 -> USB-serial adapter

# Check port details
dmesg | grep ttyUSB

# Give your user permission
sudo usermod -a -G dialout $USER
# Then log out and back in

# Or temporarily:
sudo chmod 666 /dev/ttyUSB0
```

**Windows:**
```cmd
REM Open Device Manager
devmgmt.msc

REM Look under "Ports (COM & LPT)"
REM Note the COM port number (e.g., COM3)
```

### Step 3: Test Serial Port

**Using minicom (Linux):**
```bash
sudo minicom -D /dev/ttyUSB0 -b 38400
# Press Ctrl+A then Z to see menu
# Press Ctrl+A then X to exit
```

**Using Python:**
```bash
# Install pyserial
pip3 install pyserial

# Test port
python3 -c "import serial; s=serial.Serial('/dev/ttyUSB0', 38400, timeout=1); print('Port OK'); s.close()"
```

### Step 4: Configure Modbus Gateway

Access gateway web interface (typically `http://192.168.1.100`):

**Serial Port Settings:**
```
Port:           RS-485
Baud Rate:      38400
Data Bits:      8
Parity:         Even
Stop Bits:      1
Protocol:       Modbus ASCII
Flow Control:   None
```

**Network Settings:**
```
IP Address:     192.168.1.100 (static)
Subnet Mask:    255.255.255.0
Gateway:        192.168.1.1
```

**Modbus Settings:**
```
Mode:           Modbus TCP Server (Slave)
TCP Port:       502
Unit ID:        Transparent (or 1)
Timeout:        1000 ms
```

**Save and reboot the gateway.**

### Step 5: Test Gateway Connection

**Ping test:**
```bash
ping 192.168.1.100
# Should get responses
```

**Port test:**
```bash
# Check if port 502 is open
nc -zv 192.168.1.100 502
# Or:
telnet 192.168.1.100 502
```

**Modbus test:**
```bash
# Install modbus-cli
pip3 install modbus-cli

# Read from gateway (before starting simulator)
# This will likely timeout, but confirms gateway is reachable
modbus read 192.168.1.100:502 -r 1 0x0001 2
```

## Running the Simulator

### Compile Simulator

```bash
cd /path/to/openems/io.openems.edge.evse.chargepoint.abl/test

# Ensure j2mod library is in classpath
# Find it in your Maven repository or Gradle cache:
find ~/.m2/repository -name "j2mod*.jar"
find ~/.gradle/caches -name "j2mod*.jar"

# Set classpath (example)
export CLASSPATH=".:~/.m2/repository/com/ghgande/j2mod/3.2.1/j2mod-3.2.1.jar"
```

### Start Simulator (Serial ASCII Mode)

**Easiest way - using the launcher script:**

**Linux:**
```bash
cd io.openems.edge.evse.chargepoint.abl/tools

# Simplest - uses defaults (38400 8E1 as per ABL spec)
./run-simulator.sh

# Or specify port
./run-simulator.sh /dev/ttyUSB0

# Or full control
./run-simulator.sh ascii /dev/ttyUSB0 38400 1
```

**Windows:**
```cmd
cd io.openems.edge.evse.chargepoint.abl\tools

REM Simplest - uses defaults (38400 8E1)
run-simulator.bat

REM Or specify port
run-simulator.bat COM3

REM Or full control
run-simulator.bat ascii COM3 38400 1
```

**Manual way (without script):**

**Linux:**
```bash
java -cp "$CLASSPATH" \
  io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator \
  SERIAL_ASCII /dev/ttyUSB0 38400 1
```

**Windows:**
```cmd
java -cp "%CLASSPATH%" ^
  io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator ^
  SERIAL_ASCII COM3 38400 1
```

**Expected output:**
```
[ABL Modbus Simulator] Initializing Serial server...
  Port:     /dev/ttyUSB0
  Baudrate: 38400
  Config:   8E1
  Encoding: Modbus ASCII
  Unit ID:  1
[ABL Modbus Simulator] Opening serial port /dev/ttyUSB0...
[ABL Modbus Simulator] Serial server started successfully
[ABL Modbus Simulator] Listening for Modbus ASCII requests on /dev/ttyUSB0
[ABL Simulator] Simulator started in SERIAL_ASCII mode

==============================================
    ABL EVCC2/3 Modbus Simulator
==============================================
Mode:     SERIAL_ASCII
Port:     /dev/ttyUSB0
Baudrate: 38400
Encoding: Modbus ASCII
Device ID: 1
Firmware:  1.2

Simulator is running. Press Ctrl+C to stop.
```

### Start Simulator (TCP Mode - for comparison)

```bash
java -cp "$CLASSPATH" \
  io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator \
  TCP 127.0.0.1 502 1
```

## Configure OpenEMS Edge

### 1. Create Modbus/TCP Bridge

In Apache Felix Web Console (`http://localhost:8080/system/console/configMgr`):

**Create: Bridge Modbus/TCP**
```
Component-ID:                   modbus0
Alias:                          ABL Gateway Bridge
Is enabled?:                    true
IP-Address:                     192.168.1.100  ← Gateway IP, not simulator!
Port:                           502
Log-Verbosity:                  READS_AND_WRITES_VERBOSE
Invalidate after read errors:   1
```

### 2. Create ABL ChargePoint Component

**Create: Evse.ChargePoint.ABL**
```
Component-ID:                   evse0
Alias:                          ABL EVCC Simulator
Is enabled?:                    true
Modbus-ID:                      modbus0
Modbus Unit-ID:                 1
Max Current [A]:                32
Read-Only mode:                 false
Debug Mode:                     true  ← Enables detailed logging
```

## Verification & Testing

### 1. Check Communication Flow

Monitor logs at each layer:

**Layer 1: Simulator (Serial side)**
- Check simulator console for any errors
- No explicit request/response logging by default (can be added)

**Layer 2: Gateway**
- Access gateway web interface
- Enable "Debug logging" or "Protocol logging"
- Watch for ASCII frames on serial side and TCP frames on Ethernet side

**Layer 3: Network (Wireshark)**
```bash
# Capture traffic between OpenEMS and Gateway
sudo tcpdump -i any -w modbus_capture.pcap host 192.168.1.100 and port 502

# Or use Wireshark with filter:
tcp.port == 502
```

**Layer 4: OpenEMS Bridge**
Check logs for entries like:
```
[modbus0] Modbus Read [evse0/1] FC3 addr=0x0001-0x0002 (2 regs)
  TX: 01 03 00 01 00 02 C4 0B
  RX: 01 03 04 01 00 12 7E xx xx
  Duration: 45ms
```

### 2. Expected Behavior

**First few seconds:**
```
1. OpenEMS connects to gateway (TCP connection established)
2. Gateway opens serial communication to simulator
3. OpenEMS reads device info (registers 0x0001-0x0002)
   - Should return Device ID = 1, Firmware = 1.2
4. OpenEMS reads status (registers 0x0033-0x0035)
   - Should return state A1 (standby), no EV connected
```

**During operation:**
```
1. OpenEMS writes charging current (register 0x0014)
2. Simulator state machine reacts to setpoint
3. OpenEMS reads status and phase currents
4. Cycle repeats every ~1000ms
```

### 3. Test Commands

From OpenEMS UI or Apache Karaf console:

**Check component status:**
```
component:list | grep evse
component:info evse0
```

**Check channel values:**
```
channel:get evse0 ChargingState
channel:get evse0 EvConnected
channel:get evse0 PhaseCurrentL1
```

## Troubleshooting

### Problem: "Permission denied" on /dev/ttyUSB0

**Solution:**
```bash
# Temporary fix
sudo chmod 666 /dev/ttyUSB0

# Permanent fix
sudo usermod -a -G dialout $USER
# Log out and back in
```

### Problem: "Port not found" or "does not exist"

**Check available ports:**
```bash
ls -l /dev/ttyUSB* /dev/ttyACM*
dmesg | tail -20
```

**Ensure USB device is connected:**
```bash
lsusb
# Look for your USB-serial adapter
```

### Problem: "Port busy" or "already in use"

**Find what's using the port:**
```bash
sudo lsof /dev/ttyUSB0
```

**Close any terminal programs:**
```bash
killall minicom screen
```

### Problem: No response from gateway

**Check wiring:**
- Verify A-A and B-B connections
- Check for loose connections
- Measure voltage on A/B lines (should be differential)

**Check gateway config:**
- Verify baud rate matches (38400)
- Verify parity matches (Even)
- Verify protocol is Modbus ASCII

**Test with simpler tool:**
```bash
# Direct test using modbus-cli through gateway
modbus read 192.168.1.100:502 -r 1 0x0001 2
```

### Problem: Timeout errors in OpenEMS

**Check latency:**
- Serial conversion adds 30-60ms latency
- OpenEMS default timeout is 1000ms (should be OK)

**Increase timeout if needed:**
In BridgeModbusTcpImpl, default timeout is 1000ms.
This should be sufficient for serial gateway.

**Check gateway buffer:**
- Some gateways have small buffers
- May need to adjust "inter-frame delay"

### Problem: CRC or checksum errors

**Modbus ASCII vs RTU:**
- Simulator must match gateway encoding
- ABL spec uses ASCII, not RTU
- ASCII uses LRC checksum, RTU uses CRC

**Verify encoding:**
```bash
# Start simulator explicitly in ASCII mode
java ... AblModbusSimulator SERIAL_ASCII ...

# Check gateway configuration (should be "ASCII")
```

## Performance Comparison

| Metric | Direct TCP | With Serial Gateway |
|--------|------------|---------------------|
| Latency | 10-15ms | 30-60ms |
| Reliability | ★★★★★ | ★★★★☆ |
| Realism | Low | High |
| Setup complexity | Simple | Moderate |
| Hardware cost | $0 | $50-200 |

## Advanced: Monitoring RS-485 Traffic

### Using USB Logic Analyzer

If you have a logic analyzer (e.g., Saleae Logic):

1. Connect A/B lines to analyzer
2. Configure:
   - Protocol: RS-485 (or UART)
   - Baud: 38400
   - Parity: Even
   - Encoding: ASCII
3. Watch frames like:
   ```
   TX: :010300010002FA\r\n  ← ASCII frame (human-readable!)
   RX: :010304010012xx\r\n
   ```

### Using Second Serial Port (Sniffing)

If you have 2 USB-serial adapters:
1. Wire both in parallel to RS-485 bus
2. One runs simulator, other monitors
3. Use minicom or Python to log traffic

## Conclusion

This setup provides a realistic test environment that mirrors actual ABL charger deployments. It validates:

✅ RS-485 communication
✅ Modbus ASCII encoding
✅ Gateway protocol conversion
✅ Network latency handling
✅ OpenEMS Modbus bridge compatibility

For questions or issues, check:
- OpenEMS Community Forum: https://community.openems.io/
- ABL Documentation: See `doc/` folder for specification PDF
