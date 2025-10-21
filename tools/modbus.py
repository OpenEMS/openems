from pymodbus.client import ModbusTcpClient
import struct

HOST, PORT = "192.168.0.7", 26
ADDR = 0x5002  # Voltage L1 (Float ABCD), FC3

client = ModbusTcpClient(HOST, port=PORT)
if not client.connect():
    raise SystemExit("connection failed")

try:
    resp = client.read_holding_registers(address=ADDR, count=2)
    if hasattr(resp, "isError") and not resp.isError():
        regs = resp.registers  # ex: [17258, 39322]
        raw = (regs[0] << 16) | regs[1]    # ABCD (MSW|LSW)
        v_l1 = struct.unpack(">f", raw.to_bytes(4, "big"))[0]
        print(f"Voltage L1: {v_l1:.2f} V")
    else:
        print("Error :", resp)
finally:
    client.close()
