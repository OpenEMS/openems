# ABL Modbus Simulator
to replace the real ABL eMH1 wallbox device with a simulation

## Quick Start
### ASCII Mode
Just run without arguments! Uses ABL specification defaults (if not configured explicitly different).

NOTE: Requires USB-RS485 Adapter + Gateway. 
* Adapter to create electric Modbus/ASCII signals on RS485 from the computer's USB port (to replace a real ABL eMH1 wallbox)
* Cabling between Adapter & Gateway (3 wires)
* Gateway to transform the (simulated) ABL eMH1 Modbus/ASCII signals to Modbus/TCP that can then be read by the OpenEMS Modbus/TCP bridge connected via network (e.g. Ethernet).

on Port `/dev/ttyUSB0` (Linux) or `COM3` (Windows):
- Baudrate: 38400, 8E1 (8 data bits, Even parity, 1 stop bit)
- Encoding: Modbus ASCII
- Device ID: 1

**Linux/Mac:**
```bash
# Uses default port /dev/ttyUSB0 @ 38400
./run-simulator.sh

# Or specify port
./run-simulator.sh /dev/ttyUSB1
```

**Windows:**
```cmd
REM Uses default port COM3 @ 38400
run-simulator.bat

REM Or specify port
run-simulator.bat COM4
```

### TCP Mode (No Hardware Required)
This allows to directly connect to OpenEMS modbus/tcp bridge. It avoids the gateway and the physical layer, to make sure it works directly with the bridge (e.g. to test the io.openems.edge.evse.chargepoint.abl component behind the bridge with (simulated) Modbus traffic.)

**Linux/Mac:**
```bash
./run-simulator.sh tcp
```

**Windows:**
```cmd
run-simulator.bat tcp
```

This starts the simulator on `localhost:502`.

## Command Reference

### run-simulator.sh (Linux/Mac)

```bash
# Default ASCII Mode (38400 baud, 8E1 as per ABL spec)
./run-simulator.sh                              # /dev/ttyUSB0 @ 38400
./run-simulator.sh [port]                       # Custom port @ 38400
./run-simulator.sh [port] [baudrate] [deviceId] # Full customization

# Explicit Mode Selection
./run-simulator.sh tcp [ip] [port] [deviceId]
./run-simulator.sh ascii [port] [baudrate] [deviceId]
./run-simulator.sh rtu [port] [baudrate] [deviceId] # probably not needed
```

**Examples:**
```bash
# ASCII mode (simplest - ABL spec defaults)
./run-simulator.sh                      # /dev/ttyUSB0 @ 38400
./run-simulator.sh /dev/ttyUSB1         # /dev/ttyUSB1 @ 38400
./run-simulator.sh /dev/ttyUSB0 19200   # /dev/ttyUSB0 @ 19200

# TCP mode
./run-simulator.sh tcp                  # localhost:502
./run-simulator.sh tcp 0.0.0.0 502 1   # All interfaces

# Explicit ASCII mode
./run-simulator.sh ascii /dev/ttyUSB0 38400 1

# RTU mode
./run-simulator.sh rtu /dev/ttyUSB0 9600 1
```

### run-simulator.bat (Windows)

```cmd
REM Default ASCII Mode (38400 baud, 8E1 as per ABL spec)
run-simulator.bat                              REM COM3 @ 38400
run-simulator.bat [port]                       REM Custom port @ 38400
run-simulator.bat [port] [baudrate] [deviceId] REM Full customization

REM Explicit Mode Selection
run-simulator.bat tcp [ip] [port] [deviceId]
run-simulator.bat ascii [port] [baudrate] [deviceId]
run-simulator.bat rtu [port] [baudrate] [deviceId]   REM probably not needed
```

**Examples:**
```cmd
REM ASCII mode (simplest - ABL spec defaults)
run-simulator.bat                      REM COM3 @ 38400
run-simulator.bat COM4                 REM COM4 @ 38400
run-simulator.bat COM3 19200           REM COM3 @ 19200

REM TCP mode
run-simulator.bat tcp                  REM localhost:502
run-simulator.bat tcp 0.0.0.0 502 1   REM All interfaces

REM Explicit ASCII mode
run-simulator.bat ascii COM3 38400 1

REM RTU mode
run-simulator.bat rtu COM3 9600 1
```

## Parameters

| Parameter | Description | Default | Notes |
|-----------|-------------|---------|-------|
| mode | `tcp`, `ascii`, or `rtu` | `ascii` | Optional - defaults to ASCII if omitted |
| ip | IP address (TCP mode only) | 127.0.0.1 | Use 0.0.0.0 for all interfaces |
| port | TCP port (TCP mode) | 502 | Standard Modbus port |
| port | Serial port (ASCII/RTU modes) | /dev/ttyUSB0 (Linux)<br>COM3 (Windows) | Check with `ls /dev/ttyUSB*` or Device Manager |
| baudrate | Serial baud rate | 38400 (ASCII)<br>9600 (RTU) | ABL spec uses 38400 for ASCII |
| deviceId | Modbus device/unit ID | 1 | Must match OpenEMS config |

## Serial Port Setup

### Linux

**1. Find Serial Port:**
```bash
# List all serial devices
ls -l /dev/ttyUSB* /dev/ttyACM*

# Show device info
dmesg | grep ttyUSB
```

**2. Set Permissions:**
```bash
# Temporary (until reboot)
sudo chmod 666 /dev/ttyUSB0

# Permanent (add user to dialout group)
sudo usermod -a -G dialout $USER
# Then log out and back in
```

**3. Test Port:**
```bash
# Install screen or minicom
sudo apt-get install screen

# Test port
screen /dev/ttyUSB0 38400
# Press Ctrl+A then K to exit
```

### Windows

**1. Find COM Port:**
- Open Device Manager (`devmgmt.msc`)
- Expand "Ports (COM & LPT)"
- Note the COM port number (e.g., COM3)

**2. Install Driver (if needed):**
- For FTDI chips: https://ftdichip.com/drivers/
- For CH340 chips: http://www.wch-ic.com/downloads/CH341SER_ZIP.html
- For CP2102 chips: https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers

**3. Test Port:**
```cmd
REM Open in PuTTY or similar
putty -serial COM3 -sercfg 38400,8,1,e
```

## Troubleshooting

### Script shows "j2mod library not found"

**Solution:**
```bash
# Install via Maven
mvn dependency:get -Dartifact=com.ghgande:j2mod:3.2.1

# Or manually download and place in a known location
wget https://repo1.maven.org/maven2/com/ghgande/j2mod/3.2.1/j2mod-3.2.1.jar
# Then edit CLASSPATH in the script
```

### Script shows "Class files not found"

**Solution:**
```bash
# Compile manually
cd ../test
javac -cp ".:~/.m2/repository/com/ghgande/j2mod/*/j2mod*.jar" \
  io/openems/edge/evse/chargepoint/abl/simulator/*.java
```

### "Permission denied" on serial port (Linux)

See "Serial Port Setup" section above.

### "Port already in use"

**Find what's using the port:**

**Linux:**
```bash
# For serial port
sudo lsof /dev/ttyUSB0

# For TCP port
sudo lsof -i :502
```

**Windows:**
```cmd
REM For TCP port
netstat -ano | findstr :502
```

### Simulator starts but OpenEMS can't connect

**Check:**
1. IP address is correct (gateway IP, not simulator!, except you run simulator in tcp-mode)
2. Port matches (502 for gateway)
3. Firewall allows port 502
4. Gateway is configured correctly
5. RS-485 wiring is correct (A-A, B-B, GND-GND)

## Hardware Setup

For serial mode with Modbus gateway, see:
- **[SERIAL_GATEWAY_SETUP.md](../doc/SERIAL_GATEWAY_SETUP.md)** - Complete setup guide

**Quick topology:**
```
[Simulator] --USB-Serial--> [Gateway] --Ethernet--> [OpenEMS Edge]
 ASCII          RS-485        Modbus TCP            Bridge.Modbus.Tcp
```

## Additional Resources

- **Simulator Documentation**: See `../test/io/openems/edge/evse/chargepoint/abl/simulator/`
- **ABL Specification**: `../doc/Schnittstellenbeschreibung_Modbus-ASCII_ABL-eMH1.pdf`
- **Testing Guide**: `../doc/tests.adoc`
- **OpenEMS Documentation**: https://openems.github.io/openems.io/

## License

This simulator is part of the OpenEMS project and follows the same license (EPL-2.0).
