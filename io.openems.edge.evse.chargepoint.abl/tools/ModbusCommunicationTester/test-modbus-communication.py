#!/usr/bin/env python3
"""
ABL eMH1 Wallbox Modbus ASCII Communication Test Script

This script tests Modbus ASCII communication with ABL eMH1 wallbox.
It provides an interactive menu to send predefined Modbus commands or custom messages.

Requirements: pyserial
Install: pip install pyserial

Usage:
    python3 test-modbus-communication.py [COM_PORT] [DEVICE_ID]

Examples:
    python3 test-modbus-communication.py /dev/ttyUSB0
    python3 test-modbus-communication.py COM3 1
    python3 test-modbus-communication.py /dev/ttyS0 2
"""

import serial
import serial.tools.list_ports
import time
import sys
import argparse

# ============================================================================
# CONFIGURATION - Modify these settings according to your setup
# ============================================================================

# Serial port configuration
COM_PORT = '/dev/ttyUSB0'      # Linux: /dev/ttyUSB0, /dev/ttyS0, etc.
                                # Windows: COM1, COM2, etc.
BAUDRATE = 38400                # Baud rate (default: 38400)
BYTESIZE = serial.EIGHTBITS    # Data bits: 8
PARITY = serial.PARITY_EVEN    # Parity: Even (8E1)
STOPBITS = serial.STOPBITS_ONE # Stop bits: 1
TIMEOUT = 2.0                   # Read timeout in seconds

# Device configuration
DEVICE_ID = 0x01                # Modbus device ID (1-16, default: 1)

# ============================================================================
# RESPONSE PARSER FUNCTIONS
# ============================================================================

def parse_device_id_firmware(resp_hex): # getestet: passed
    """Parse response from 0x0001..0x0002 - Read device-ID and firmware revision"""
    # Read device-ID, firmware-revision and config 
    if len(resp_hex) < 14:
        return ["Response too short"]

    result = []
    # Bytes: 01 03 04 01 0F 15 09 CA (example)
    # (Expected: >010304010F1509CA CRLF)
    # (Received: >010304014141363F CRLF)
    # Response format: 01 03 04 XX YY ZZ WW
    # Response Type: R2 (4 bytes)
    # For 16-bit register: bits 15..8 = high byte, bits 7..0 = low byte
    reg_0001 = int(resp_hex[6:10], 16)   # Register 0x0001 (0x010F) - Positions 6-9: XXYY (4 hex chars)
    reg_0002 = int(resp_hex[10:14], 16)  # Register 0x0002 (0x1509) - Positions 10-13: ZZWW (4 hex chars)

    # Register 0x0001 (0x010F):

    # bits 31..24 (0x01)
    register = (reg_0001 >> 8) & 0xFF   # b0000 0001
    result.append(f"Register which the answer is referring to: {register}")

    # bits 23..22 (b00) = hardware
    hw_version = (reg_0001 >>6) & 0x03
    hw_map = {0x00: "pcba: 141215", 0x01: "pcba: 160307", 0x10: "pcba: 170725", 0x11: "not used"}
    hw_str = hw_map.get(hw_version, f"Unknown (0x{hw_version:02X})")
    result.append(f"Hardware: (0b{hw_version:02b}) = {hw_str}")

    # bits 21..16 (b00ffff) = device ID
    dev_id = reg_0001 & 0x3F 
    result.append(f"Device ID: {dev_id}")

    # Register 0x0002 (0x1509): 
    
    # bits 15..12 () = major version, bits 11..8 = minor version
    fw_major = (reg_0002 >> 12) & 0x0F
    fw_minor = (reg_0002 >> 8) & 0x0F
    result.append(f"Firmware: V{fw_major}.{fw_minor}")
    
    #bits 7..0 = flags
    flags = reg_0002 & 0xFF

    if flags & 0x80: result.append("  - Coding resistor reading enabled") # Reading of coding resistor at socket-input CS for rated current I_CS  of adapter enabled 
    if flags & 0x40: result.append("  - Upstream timeout enabled")
    if flags & 0x20: result.append("  - Internal phase current meter enabled")
    if flags & 0x10: result.append("  - Internal RDC-MD enabled")
    if flags & 0x08: result.append("  - Socket enabled")
    if flags & 0x04: result.append("  - Welding detection (contactor) enabled")
    
    cable_mode = flags & 0x03
    cable_map = {0: "No function", 1: "Must be closed for transition B1->B2", 2: "Closed: Outlet enabled", 3: "Invalid"}
    result.append(f"  - Cable mode: (0b{cable_mode:02b}) = {cable_map.get(cable_mode, 'Unknown')}")

    return result

def parse_modbus_settings(resp_hex): # tested: passed
    """Parse response from 0x0003 - Read MODBUS settings"""
    if len(resp_hex) < 10:
        return ["Response too short"]

    result = []
    # Bytes: 01 03 02 03 27 D0 (example)
    # (Expected: >0103020327D0 CRLF)
    # (Received: >0103020327D0 CRLF)
    # Response format: 01 03 02 XX YY
    # Response Type: R1 (2 bytes)
    reg_0003 = int(resp_hex[6:10], 16) # 0x0327 = b0000 0011 0010 0111

    register = (reg_0003 >> 8) & 0xFF   # b0000 0011
    result.append(f"Register which the answer is referring to: 0x{register:02X}")

    stopbits = (reg_0003 >> 6) & 0x03   # b00 (b00 = 1 stop bit, b01 = 2 stop bits)
    result.append(f"Stop bits: (0b{stopbits:02b}) = {1 if stopbits == 0 else 2}")

    parity = (reg_0003 >> 4) & 0x03   # b10 (00=None, 01=Odd, 10=Even, 11=Invalid)
    parity_map = {0: "None", 1: "Odd", 2: "Even", 3: "Invalid"}
    result.append(f"Parity: (0b{parity:02b}) = {parity_map.get(parity, 'Unknown')}")

    baudrate_code = reg_0003 & 0x0F   # b0111 (5=9600, 6=19200, 7=38400, 8=57600)
    baud_map = {5: "9600", 6: "19200", 7: "38400", 8: "57600"}
    result.append(f"Baudrate: (0b{baudrate_code:02b}) = {baud_map.get(baudrate_code, 'Invalid')} baud")

    return result

def parse_system_flags(resp_hex): # reviewed
    """Parse response from 0x0006 - Read system flags"""
    if len(resp_hex) < 14:
        return ["Response too short"]

    result = []
    # example: ???
    # Response format: 01 03 04 XX XX YY YY
    # Response Type: R2 (4 bytes)
    reg_0006 = int(resp_hex[6:10], 16)   # Register 0x0006 Positions   6-9: (4 hex chars)
    flags = int(resp_hex[10:14], 16)  # Register 0x0007 Positions 10-13: (4 hex chars)

    # Register 0x0006: 
    # bits 31..24
    register = (reg_0006 >> 8) & 0xFF   # 0x..
    result.append(f"Register which the answer is referring to: 0x{register:02X}")

    # bits 23..16 = state pointer
    state_pointer = (reg_0006 >> 8) & 0xFF
    result.append(f"State machine pointer: 0x{state_pointer:02X}")

    # Register 0x0007:
    result.append(f"System flags: (0x{flags:04X} = 0b{flags:016b})")

    # bits 15
    upstream_timeout = flags & 0x8000
    if upstream_timeout:
        result.append(f"(0b{upstream_timeout:01b}) = Upstream timeout triggers F14")
    else:
        result.append(f"(0b{upstream_timeout:01b}) = Upstream timeout triggers F4")

    # bits 14
    b1_b2_apc = flags & 0x4000
    if b1_b2_apc:
        result.append(f"(0b{b1_b2_apc:01b}) = B1->B2 enabled after power-cycle")
    else:
        result.append(f"(0b{b1_b2_apc:01b}) = B1->B2 disabled after power-cycle")

    # bits 13..12
    limits_CP = (flags >> 12) & 0x03
    if limits_CP == 0x00:
        result.append(f"(0b{limits_CP:02b}) = Use default limits for CP detection and timing")
    elif limits_CP == 0x10:
        result.append(f"(0b{limits_CP:02b}) = Use China limits for CP detection and timing")
    else:
        result.append(f"(0b{limits_CP:02b}) = (reserved)")

    # bits 11..10: reserved
    
    # bit 9
    upstream_comm = flags & 0x200       
    if upstream_comm:
        result.append(f"(0b{upstream_comm:01b}) = WARN: Upstream communication lost (BC6); timeout may trigger F4/F14 ") 
    else:
        result.append(f"(0b{upstream_comm:01b}) = OK: Upstream communication established")

    # bit 8
    load_imbalance = flags & 0x100
    if load_imbalance:
        result.append(f"(0b{load_imbalance:01b}) = WARN: Load imbalance detected (BC3)")
    else:    
        result.append(f"(0b{load_imbalance:01b}) = OK: No load imbalance detected")

    # bit 7
    onboard_temp = flags & 0x80
    if onboard_temp:
        result.append(f"(0b{onboard_temp:01b}) = WARN: Onboard Temperature 60°C < T < 80°C (BC5)")
    else:
        result.append(f"(0b{onboard_temp:01b}) = OK: Onboard Temperature T <= 60°C")

    # bit 6
    current_meter = flags & 0x40
    if current_meter:
        result.append(f"(0b{current_meter:01b}) = WARN: Internal phase current metering failed (BC4)")
    else:
        result.append(f"(0b{current_meter:01b}) = OK: Internal phase current metering available")

    # bit 5
    e0_a1 = flags & 0x20
    if e0_a1:
        result.append(f"(0b{e0_a1:01b}) = E0->A1 disabled")
    else:
        result.append(f"(0b{e0_a1:01b}) = E0->A1 enabled")
        
    # bit 4
    b1_b2_en = flags & 0x10
    if b1_b2_en:
        result.append(f"(0b{b1_b2_en:01b}) = B1->B2 disabled")
    else:
        result.append(f"(0b{b1_b2_en:01b}) = B1->B2 enabled")
    
    # bit 3
    a1_b1_en = flags & 0x08
    if a1_b1_en:
        result.append(f"(0b{a1_b1_en:01b}) = A1->B1 disabled")
    else:
        result.append(f"(0b{a1_b1_en:01b}) = A1->B1 enabled")   
    
    # bits 2..0 = EV Current status 
    
    ev_current = flags & 0x07
    if ev_current == 0b000:
        result.append(f"(0b{ev_current:03b}) = OK: EV Current <= 100% I_c") # EV Current ≤ 100% I_c
    elif ev_current == 0b001:
        result.append(f"(0b{ev_current:03b}) = WARN: EV Current 100-105% I_c") # 100% I_c < EV Current ≤ 105% I_c  
    elif ev_current == 0b101:
        result.append(f"(0b{ev_current:03b}) = WARN: EV Current 105-110% I_c (failure aft. 1000s; not for China)") # 105% I_c < EV Current ≤ 110% I_c 
    elif ev_current == 0b011:
        result.append(f"(0b{ev_current:03b}) = WARN: EV Current 110-120% I_c (failure aft. 100s; China: 5s)") # 110% I_c < EV Current ≤ 120% I_c
    elif ev_current == 0b111:
        result.append(f"(0b{ev_current:03b}) = WARN: EV Current > 120% I_c  (failure aft. 10s; China: 5s)") # EV Current > 120 % I_c 
    else: 
        result.append(f"(0b{ev_current:03b}) = ERR: Undefined EV Current status")
        
    return result

def parse_current_short(resp_hex): # tested: 
    """Parse response from 0x0033..0x0035 - Read current (short)"""
    # Read state and current of each phase (1A) 
    if len(resp_hex) < 18:
        return ["Response too short"]

    result = []
    # example: 01 03 06 33 80 C3 0A 0A 00 EC
    # (Expected: >0103063380C30A0A00EC CRLF)
    # (Received: >0103063300A1646464F6 CRLF)
    # Response format: 01 03 06 XX YY ZZ AA BB CC 
    # Response Type: R3 (3 registers = 6 bytes)
    reg_0033 = int(resp_hex[6:10], 16) # 0x3380 (= b0011 0011 1000 0000): XXYY - Positions 6-9: Register 0x0033 (UCP status + reserved)
    reg_0034 = int(resp_hex[10:14], 16) # 0xC30A (= b1100 0011 0000 1010): ZZAA - Positions 10-13: Register 0x0034 (State + ICT1)
    reg_0035 = int(resp_hex[14:18], 16) # 0x0A00 (= b0000 1010 0000 0000): BBCC - Positions 14-17: Register 0x0035 (ICT2 + ICT3)

    # Register 0x0033: 
    # bits 47..40 = Register which the answer is referring to
    register = (reg_0033 >> 8) & 0xFF   # 0x33 = b0011 0011
    result.append(f"Register which the answer is referring to: 0x{register:02X}")
    
    # bit 39 = UCP state
    ucp_state = (reg_0033 >> 7) & 0x01   # b1
    if ucp_state:
        result.append(f"(0b{ucp_state:01b}) = U_CP ≤ 10V (EV connected)")     # not valid in state Ex and Fx 
    else:
        result.append(f"(0b{ucp_state:01b}) = U_CP > 10V (no EV connected)")  # not valid in state Ex and Fx 
    # bits 38..32 = reserved (=0x00)
    
    # Register 0x0034: 
    # bits 31..24 = state of EVSE
    state_code = (reg_0034 >> 8) & 0xFF # 0xC3 = b1100 0011
    result.append(f"State of EVSE: 0x{state_code:02X}")    
    
    # bits 23..16 = I_CT1
    ict1 = reg_0034 & 0xFF  # 0x0A = b0000 1010      # I_ct1  [A] (0…80A)
    if ict1 == 0x64: 
        result.append(f"(0x{ict1:02X}) = Phase L1 current meter not available or state A")
    else:
        result.append(f"(0x{ict1:02X}) = Phase current: L1={ict1}A")
    
    # Register 0x0035: 
    # bits 15..8 = I_CT2
    ict2 = (reg_0035 >> 8) & 0xFF  # 0x0A = b0000 1010     # I_ct2  [A] (0…80A)
    if ict2 == 0x64:
        result.append(f"(0x{ict2:02X}) = Phase L2 current meter not available or state A")
    else:
        result.append(f"(0x{ict2:02X}) = Phase current: L2={ict2}A")
    
    # bits 7..0 = I_CT3
    ict3 = reg_0035 & 0xFF  # 0x00 = b0000 0000    # I_ct3  [A] (0…80A)
    if ict3 == 0x64:
        result.append(f"(0x{ict3:02X}) = Phase L3 current meter not available or state A")
    else:
        result.append(f"(0x{ict3:02X}) = Phase current: L3={ict3}A")

    return result

def parse_current_full(resp_hex): # tested:
    """Parse response from 0x002E..0x0032 - Read current (full)"""
    # Read state, digital inputs, max charge current I_C  and current of each phase (resolution 0.1A) 
    if len(resp_hex) < 26:
        return ["Response too short"]

    result = []
    # example: 01 03 0A 2E C3 81 0A 00 64 00 64 00 00 1F
    # (Expected: >01030A2EC3810A0064006400001F CRLF)
    # (Received: >01030A2EA1121503E803E803E83B CRLF) 
    # Response format: 01 03 0A XX YY ZZ AA BB CC DD EE FF GG
    # Response Type: R5 (5 registers = 10 bytes)
    reg_002E = int(resp_hex[6:10], 16)  # (0x2EC3) Positions   6-9: Register 0x002E - XXYY (State + UCP flags)
    reg_002F = int(resp_hex[10:14], 16) # (0x810A) Positions 10-13: Register 0x002F - ZZAA (Icmax duty cycle)
    ict1 = int(resp_hex[14:18], 16)     # (0x0064) Positions 14-17: Register 0x0030 - BBCC (ICT1)
    ict2 = int(resp_hex[18:22], 16)     # (0x0064) Positions 18-21: Register 0x0031 - DDEE (ICT2)
    ict3 = int(resp_hex[22:26], 16)     # (0x0000) Positions 22-25: Register 0x0032 - FFGG (ICT3)

    # Register 0x002E:
    # bits 79..72 = Register which the answer is referring to
    register = (reg_002E >> 8) & 0xFF   # 0x2E = b0010 1110
    result.append(f"Register which the answer is referring to: 0x{register:02X}")
       
    # bits 71..64 = State of EVSE 
    state_code = reg_002E & 0xFF  # 0xC3 = b1100 0011
    result.append(f"State: 0x{state_code:02X}")

    # Register 0x002F:    
    # bits 7..0 = UCP flags
    ucp_flags = (reg_002F >> 12) & 0x0F   # 0x8 = b1000
    result.append(f"UCP flags: (0x{ucp_flags:01X}) = 0b{ucp_flags:04b})")
    if ucp_flags & 0x8: # b1
        result.append(f"({ucp_flags & 0x8}) = U_CP ≤ 10V (EV connected)") # not valid in state Ex and Fx
    else:
        result.append(f"({ucp_flags & 0x8}) = U_CP > 10V (no EV connected)") # not valid in state Ex and Fx

    if ucp_flags & 0x4: # b0
        result.append(f"({ucp_flags & 0x4}) = WARN: Duty cycle reduced due to BC3-BC6")
    else:    
        result.append(f"({ucp_flags & 0x4}) = Duty cycle not reduced due to BC3-BC6")

    if ucp_flags & 0x2: # b0
        result.append(f"({ucp_flags & 0x2}) = EN2 closed")
    else:
        result.append(f"({ucp_flags & 0x2}) = EN2 open")

    if ucp_flags & 0x1: # b0
        result.append(f"({ucp_flags & 0x1}) = EN1 closed")
    else:
        result.append(f"({ucp_flags & 0x1}) = EN1 open")
    
    # Duty cycle I_c  [%]*10 (0…100%)   
    icmax = reg_002F & 0x0FFF  # 0x10A = b0001 0000 1010
    result.append(f"(0x{icmax:03X}) = Icmax (duty cycle): {icmax/10:.1f}%") # values: 0x000…0x3E8

    # I_ct1  [0,1A] (0…80A)    
    if ict1 == 0x03E8:
        result.append(f"(0x{ict1:04X}) = Phase L1 current meter not available or state A")
    else:
        result.append(f"(0x{ict1:04X}) = Phase currents: L1={ict1/10:.1f}A") # values: 0x0000…0x0320 

    # I_ct2  [0,1A] (0…80A) 
    if ict2 == 0x03E8:
        result.append(f"(0x{ict2:04X}) = Phase L2 current meter not available or state A")
    else:
        result.append(f"(0x{ict2:04X}) = Phase currents: L2={ict2/10:.1f}A") # values: 0x0000…0x0320

    # I_ct3  [0,1A] (0…80A) 
    if ict3 == 0x03E8: 
        result.append(f"(0x{ict3:04X}) = Phase L3 current meter not available or state A")
    else:
        result.append(f"(0x{ict3:04X}) = Phase currents: L3={ict3/10:.1f}A") # values: 0x0000…0x0320

    return result

def parse_write_response(resp_hex):
    """Parse write response (0x10 function code)"""
    if len(resp_hex) < 6:
        return ["Response too short"]

    result = []
    func_code = resp_hex[2:4]

    if func_code == '10':
        result.append("OK: Write operation successful")
    elif func_code == '90':
        error_code = resp_hex[4:6] if len(resp_hex) >= 6 else '??'
        result.append(f"ERR: Write failed with exception code 0x{error_code}")

    return result

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def calculate_lrc(message):
    """Calculate LRC (Longitudinal Redundancy Check) for Modbus ASCII"""
    # Remove : and CRLF, convert hex string to bytes
    hex_str = message.strip().strip(':').strip('\r\n')

    # Calculate LRC
    lrc = 0
    for i in range(0, len(hex_str), 2):
        lrc = (lrc + int(hex_str[i:i+2], 16)) & 0xFF

    lrc = ((lrc ^ 0xFF) + 1) & 0xFF
    return lrc

def format_message(message):
    """Ensure message has proper format with : prefix and CRLF suffix"""
    message = message.strip()

    # Remove existing : and CRLF if present
    if message.startswith(':'):
        message = message[1:]
    if message.endswith('\r\n'):
        message = message[:-2]

    # Add them back
    return f':{message}\r\n'

def list_available_ports():
    """List all available serial ports on the system"""
    ports = serial.tools.list_ports.comports()

    if not ports:
        return None

    port_list = []
    for port in sorted(ports):
        desc = port.description if port.description else "No description"
        hwid = port.hwid if port.hwid else "No hardware ID"
        port_list.append(f"  - {port.device}: {desc} [{hwid}]")

    return port_list

def send_message(ser, message):
    """Send a message via serial port"""
    formatted_msg = format_message(message)

    # Display what we're sending
    display_msg = formatted_msg.replace('\r\n', ' CRLF')
    print(f"\n-> Sending: {display_msg}")

    # Send the message
    ser.write(formatted_msg.encode('ascii'))
    ser.flush()

    return formatted_msg

def receive_response(ser, timeout=2.0):
    """Receive response from serial port"""
    start_time = time.time()
    response = b''

    while (time.time() - start_time) < timeout:
        if ser.in_waiting > 0:
            chunk = ser.read(ser.in_waiting)
            response += chunk

            # Check if we received a complete message (ends with CRLF)
            if response.endswith(b'\r\n'):
                break
        time.sleep(0.01)

    return response.decode('ascii', errors='replace') if response else None

def display_response(response, expected=None, interpreter=None):
    """Display received response with interpretation"""
    if response:
        display_resp = response.replace('\r\n', ' CRLF')
        print(f"<- Received: {display_resp}")

        if expected:
            print(f"<- Expected: {expected}")

        # Parse response
        if response.startswith('>'):
            resp_hex = response[1:].strip()
            if len(resp_hex) >= 4:
                dev_id = resp_hex[0:2]
                func_code = resp_hex[2:4]

                if func_code == '03':
                    print(f"   OK: Read response from device 0x{dev_id}")
                elif func_code == '10':
                    print(f"   OK: Write successful to device 0x{dev_id}")
                elif func_code == '90':
                    error_code = resp_hex[4:6] if len(resp_hex) >= 6 else '??'
                    print(f"   ERR: Error response: Exception code 0x{error_code:02X} (0x04: 'Writing registers failed')" ) # Exception code “Writing registers failed” 

                # Interpret the response if interpreter function is provided
                if interpreter:
                    print("\n   Interpretation:")
                    try:
                        interpreted = interpreter(resp_hex)
                        for line in interpreted:
                            print(f"   {line}")
                    except Exception as e:
                        print(f"   ERR: Error interpreting response: {e}")
    else:
        print("<- No response received (timeout)\n")
        print("No response is returned, if :")
        print("• device receives a request, but detects a communication error (parity, LRC, CRC, ...)") 
        print("• function code is neither 0x03 (read) nor 0x10 (write) ")
        print("• write-request does not access a valid register for writing")
        print("• qty of registers of a write-request does not meet requirements of accessed register")
        print("• number of value-bytes write-request does not meet qty of registers")
        print("• read-request does not access a valid register for reading")
        

def display_config(com_port, device_id):
    """Display current COM port configuration"""
    print("\n" + "="*70)
    print("COM PORT CONFIGURATION")
    print("="*70)
    print(f"Port:       {com_port}")
    print(f"Baudrate:   {BAUDRATE} baud")
    print(f"Data bits:  {BYTESIZE}")
    print(f"Parity:     {PARITY} (Even)")
    print(f"Stop bits:  {STOPBITS}")
    print(f"Timeout:    {TIMEOUT} seconds")
    print("="*70)
    print("MODBUS CONFIGURATION")  
    print("="*70)
    print(f"Format:     Modbus ASCII")
    print(f"Device ID:  0x{device_id:02X} ({device_id})")
    print("="*70)

def display_menu(messages):
    """Display message menu"""
    print("\n" + "="*70)
    print("AVAILABLE MESSAGES")
    print("="*70)

    for num, msg in sorted(messages.items()):
        print(f"\n{num:2d}. {msg['name']}")
        print(f"    Register:    {msg['register']}")
        print(f"    Description: {msg['description']}")
        request_display = msg['request'].replace('\r\n', ' CRLF')
        print(f"    Request:     {request_display}")
        response_display = msg['response'].replace('\r\n', ' CRLF')
        print(f"    Expected:    {response_display}")

    print("\n" + "="*70)
    print("Enter message number (1-12) to send")
    print("Enter ':' followed by hex string to send custom message")
    print("Enter 'q' to quit")
    print("="*70)

# ============================================================================
# MAIN PROGRAM
# ============================================================================

def build_messages(device_id):
    """Build message dictionary with the specified device ID"""
    return {
        1: {
            'name': 'Read device-ID and firmware revision',
            'register': '0x0001-0x0002',
            'request': f':{device_id:02X}030001000{2:01X}F9\r\n',
            'response': f'>{device_id:02X}0304010F1509CA\r\n (example)',
            'description': 'Returns device ID, firmware version, and configuration flags',
            'interpreter': parse_device_id_firmware
        },
        2: {
            'name': 'Read MODBUS settings',
            'register': '0x0003',
            'request': f':{device_id:02X}030003000{1:01X}F8\r\n',
            'response': f'>{device_id:02X}03020327D0\r\n (example)',
            'description': 'Returns baudrate, parity, and stop bit configuration',
            'interpreter': parse_modbus_settings
        },
        3: {
            'name': 'Read system flags',
            'register': '0x0006-0x0007',
            'request': f':{device_id:02X}030006000{2:01X}F5\r\n',
            'response': f'>{device_id:02X}03040600....\r\n',
            'description': 'Returns system status flags and state machine pointer',
            'interpreter': parse_system_flags
        },
        4: {
            'name': 'Read current (short) - State & 3-phase current',
            'register': '0x0033-0x0035',
            'request': f':{device_id:02X}030033000{3:01X}C6\r\n',
            'response': f'>{device_id:02X}0306{3:01X}380C30A0A00EC\r\n (example: State C3, 10A/10A/0A)',
            'description': 'Returns UCP status, state, and current per phase (1A resolution)',
            'interpreter': parse_current_short
        },
        5: {
            'name': 'Read current (full) - State, Icmax & 3-phase current',
            'register': '0x002E-0x0032',
            'request': f':{device_id:02X}03002E000{5:01X}C9\r\n',
            'response': f'>{device_id:02X}030A2EC3810A0064006400001F\r\n (example)',
            'description': 'Returns state, digital inputs, max current Icmax, and phase currents (0.1A resolution)',
            'interpreter': parse_current_full
        },
        6: {
            'name': 'Set Icmax to 6A (minimum)',
            'register': '0x0014',
            'request': f':{device_id:02X}1000140001020050DA\r\n',
            'response': f'>{device_id:02X}1000140001DA\r\n (OK) or >{device_id:02X}90046B\r\n (FAIL)',
            'description': 'Sets maximum charge current to 6A (duty cycle 10.0% = 0x0050)',
            'interpreter': parse_write_response
        },
        7: {
            'name': 'Set Icmax to 10A',
            'register': '0x0014',
            'request': f':{device_id:02X}1000140001020{0x00A6:04X}32\r\n',
            'response': f'>{device_id:02X}1000140001DA\r\n (OK)',
            'description': 'Sets maximum charge current to 10A (duty cycle 16.6% = 0x00A6)',
            'interpreter': parse_write_response
        },
        8: {
            'name': 'Set Icmax to 16A',
            'register': '0x0014',
            'request': f':{device_id:02X}1000140001020{0x010B:04X}CD\r\n',
            'response': f'>{device_id:02X}1000140001DA\r\n (OK)',
            'description': 'Sets maximum charge current to 16A (duty cycle 27.1% = 0x010B)',
            'interpreter': parse_write_response
        },
        9: {
            'name': 'Set Icmax to 32A',
            'register': '0x0014',
            'request': f':{device_id:02X}10001400010202{0x1E:02X}9C\r\n',
            'response': f'>{device_id:02X}1000140001DA\r\n (OK)',
            'description': 'Sets maximum charge current to 32A (duty cycle 53.3% = 0x021E)',
            'interpreter': parse_write_response
        },
        10: {
            'name': 'Modify state - Jump to E0 (disabled)',
            'register': '0x0005',
            'request': f':{device_id:02X}10000500010{2:01X}E0E0FB\r\n',
            'response': f'>{device_id:02X}10000500{0x01:02X}D7\r\n (OK)',
            'description': 'Disables the outlet (state E0)',
            'interpreter': parse_write_response
        },
        11: {
            'name': 'Modify state - Jump to E2 (setup mode)',
            'register': '0x0005',
            'request': f':{device_id:02X}10000500010{2:01X}E2E2F9\r\n',
            'response': f'>{device_id:02X}10000500{0x01:02X}D7\r\n (OK)',
            'description': 'Enables setup mode (state E2)',
            'interpreter': parse_write_response
        },
        12: {
            'name': 'Set device ID to 2',
            'register': '0x002C',
            'request': f':{device_id:02X}10002C00010{2:01X}000{2:01X}CF\r\n',
            'response': f'>{device_id:02X}10002C0001A5\r\n (OK)',
            'description': 'Changes device ID to 2 (requires state E2)',
            'interpreter': parse_write_response
        }
    }

def parse_arguments():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(
        description='ABL eMH1 Wallbox Modbus ASCII Communication Test Script',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s                              # Use default settings
  %(prog)s /dev/ttyUSB0                 # Specify COM port
  %(prog)s COM3 1                       # Specify COM port and device ID
  %(prog)s /dev/ttyS0 2                 # Linux serial port with device ID 2
        """
    )

    parser.add_argument('com_port', nargs='?', default=COM_PORT,
                        help=f'Serial port (default: {COM_PORT})')
    parser.add_argument('device_id', nargs='?', type=int, default=DEVICE_ID,
                        help=f'Modbus device ID 1-16 (default: {DEVICE_ID})')

    args = parser.parse_args()

    # Validate device ID
    if args.device_id < 1 or args.device_id > 16:
        parser.error(f"Device ID must be between 1 and 16, got {args.device_id}")

    return args.com_port, args.device_id

def main():
    """Main program loop"""
    # Parse command line arguments
    com_port, device_id = parse_arguments()

    print("\n" + "="*70)
    print("ABL eMH1 WALLBOX - MODBUS ASCII TEST SCRIPT")
    print("="*70)

    # Display configuration
    display_config(com_port, device_id)

    # Build messages with the specified device ID
    messages = build_messages(device_id)

    # Display available messages
    display_menu(messages)

    # Open serial port
    try:
        ser = serial.Serial(
            port=com_port,
            baudrate=BAUDRATE,
            bytesize=BYTESIZE,
            parity=PARITY,
            stopbits=STOPBITS,
            timeout=TIMEOUT
        )

        print(f"\nOK: Successfully opened {com_port}")

        # Send first message automatically (Read device ID and firmware)
        print("\n" + "-"*70)
        print("SENDING INITIAL MESSAGE: Read device-ID and firmware revision")
        print("-"*70)

        msg = messages[1]
        send_message(ser, msg['request'])
        #jk send_message(ser, msg['request'])
        time.sleep(0.1)
        response = receive_response(ser)
        display_response(response, msg['response'], msg.get('interpreter'))

        # Interactive loop
        while True:
            try:
                print("\n" + "-"*70)
                user_input = input("\nEnter command: ").strip()

                if user_input.lower() in ['q', 'quit', 'exit']:
                    print("\nExiting...")
                    break

                # Custom message starting with ':'
                if user_input.startswith(':'):
                    custom_msg = user_input[1:].strip()
                    send_message(ser, custom_msg)
                    time.sleep(0.1)
                    response = receive_response(ser)
                    display_response(response)

                # Numbered message selection
                elif user_input.isdigit():
                    msg_num = int(user_input)

                    if msg_num in messages:
                        msg = messages[msg_num]
                        print(f"\n-> Sending: {msg['name']}")
                        print(f"   {msg['description']}")

                        send_message(ser, msg['request'])
                        time.sleep(0.1)
                        response = receive_response(ser)
                        display_response(response, msg['response'], msg.get('interpreter'))
                    else:
                        print(f"ERR: Invalid message number: {msg_num}")
                        print(f"   Valid range: 1-{len(messages)}")

                else:
                    print("ERR: Invalid input. Enter a number (1-12), ':' for custom message, or 'q' to quit")

            except KeyboardInterrupt:
                print("\n\nInterrupted by user")
                break
            except Exception as e:
                print(f"\nERR: Error: {e}")

        # Close serial port
        ser.close()
        print(f"\nOK: Closed {com_port}")

    except serial.SerialException as e:
        print(f"\nERROR: Failed to open {com_port}: {e}")
        print("\nTroubleshooting:")
        print("- Check if the port name is correct")
        print("- Verify the device is connected")
        print("- Ensure you have permission to access the port (Linux: add user to dialout group)")

        # List available ports
        available_ports = list_available_ports()
        if available_ports:
            print("\nAvailable serial ports on this system:")
            for port in available_ports:
                print(port)
        else:
            print("\nNo serial ports detected on this system.")

        sys.exit(1)
    except Exception as e:
        print(f"\nERROR: Unexpected error: {e}")
        sys.exit(1)

if __name__ == '__main__':
    """
    Script Entry Point

    Enhancements Made:

    1. Command-line Arguments:
       - Added argparse module for proper argument parsing
       - COM port can be specified as first argument (optional)
       - Device ID can be specified as second argument (optional)
       - Both parameters default to the hardcoded values if not provided

    2. Dynamic Message Generation:
       - Created build_messages(device_id) function that generates all messages
         with the correct device ID
       - All messages now adapt to the device ID specified via command line or default

    3. Updated Function Signatures:
       - display_config() now accepts com_port and device_id parameters
       - display_menu() now accepts messages parameter

    4. Help System:
       - Added -h or --help flag for usage information
       - Includes examples of how to use the script

    5. Response Interpretation:
       - Added parser functions for each message type to interpret responses
       - Displays human-readable interpretation below raw response data
       - Shows decoded values like firmware version, state, currents, flags, etc.
       - Available COM port listing when connection fails
            ---

            Enhancements Made:

            1. Response Parser Functions (lines 42-245)

            Added specialized parser functions for each message type:
            - parse_device_id_firmware() - Decodes hardware version, device ID, firmware version, and configuration
            flags
            - parse_modbus_settings() - Interprets baudrate, parity, and stop bits
            - parse_system_flags() - Decodes system status flags, state machine pointer, and error conditions
            - parse_current_short() - Parses state and 3-phase currents (1A resolution)
            - parse_current_full() - Parses state, Icmax, digital inputs, and 3-phase currents (0.1A resolution)
            - parse_write_response() - Interprets write operation success/failure

            2. Updated display_response() (line 323)

            - Added optional interpreter parameter
            - Calls the interpreter function if provided
            - Displays human-readable interpretation below the raw response

            3. Enhanced build_messages() (lines 402-501)

            - Added 'interpreter' field to each message definition
            - Links each message to its appropriate parser function

            4. Updated Function Calls

            - Modified all display_response() calls to pass the interpreter function
            - Uses msg.get('interpreter') to safely retrieve the parser

            Example Output:

            When you send message #1 (Read device-ID and firmware), you'll now see:

            ← Received: >01030401011237AD CRLF
            ← Expected: >010304010F1509CA CRLF (example)
                ✓ Read response from device 0x01

                Interpretation:
                Hardware: pcba: 160307
                Device ID: 1
                Firmware: V1.2
                - Coding resistor reading enabled
                - Upstream timeout enabled
                - Internal phase current meter enabled
                - Internal RDC-MD enabled
                - Socket enabled
                - Welding detection enabled
                - Cable mode: Closed: Outlet enabled

            The script now provides complete human-readable interpretation of all Modbus responses!

            ---
    Usage Examples:
        # Use default settings (COM_PORT=/dev/ttyUSB0, DEVICE_ID=1)
        python3 test-modbus-communication.py

        # Specify custom COM port
        python3 test-modbus-communication.py /dev/ttyS0

        # Specify both COM port and device ID
        python3 test-modbus-communication.py COM3 2

        # On Windows
        python3 test-modbus-communication.py COM4 1

        # Show help
        python3 test-modbus-communication.py --help
    """
    main()
