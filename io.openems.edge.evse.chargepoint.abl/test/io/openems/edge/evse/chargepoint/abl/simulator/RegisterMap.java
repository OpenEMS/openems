package io.openems.edge.evse.chargepoint.abl.simulator;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * Virtual Modbus register map for ABL EVCC2/3 simulator.
 *
 * <p>
 * Maintains the state of all Modbus registers as defined in the ABL
 * specification.
 */
public class RegisterMap {

	private final Map<Integer, Integer> registers = new HashMap<>();
	private final int deviceId;
	private final int firmwareMajor;
	private final int firmwareMinor;

	/**
	 * Create a new register map.
	 *
	 * @param deviceId       device ID (0x01...0x10)
	 * @param firmwareMajor  firmware major version
	 * @param firmwareMinor  firmware minor version
	 */
	public RegisterMap(int deviceId, int firmwareMajor, int firmwareMinor) {
		this.deviceId = deviceId;
		this.firmwareMajor = firmwareMajor;
		this.firmwareMinor = firmwareMinor;
		this.initializeRegisters();
	}

	/**
	 * Initialize all registers with default values.
	 */
	private void initializeRegisters() {
		// 0x0001: Device ID and hardware info
		// Bits 23..22: Hardware (01 = pcba:160307)
		// Bits 21..16: Device ID
		int reg0x0001 = (0x01 << 22) | ((this.deviceId & 0x1F) << 16);
		this.registers.put(0x0001, reg0x0001);

		// 0x0002: Firmware version and config
		// Bits 15..12: Major version
		// Bits 11..8: Minor version
		// Bits 7..0: Configuration flags (typical value: 0x7E)
		int reg0x0002 = ((this.firmwareMajor & 0x0F) << 12) | ((this.firmwareMinor & 0x0F) << 8) | 0x7E;
		this.registers.put(0x0002, reg0x0002);

		// 0x0003: Modbus settings (38400 baud, 8E1)
		this.registers.put(0x0003, 0x0327); // 8E1, 38400 baud

		// 0x0014: Set Icmax (initially 0 = no charging allowed)
		this.registers.put(0x0014, 0x0000);

		// 0x0033-0x0035: Current state and phase currents
		// Initialize to state A1 (0xA1), no EV connected (bit 39=0)
		this.registers.put(0x0033, 0x0000); // Bit 39=0 (no EV), bits 38..32=0
		this.registers.put(0x0034, 0xA100); // State A1 in high byte, ICT1=0 in low byte
		this.registers.put(0x0035, 0x6464); // ICT2=0x64 (not available), ICT3=0x64
	}

	/**
	 * Read a single register.
	 *
	 * @param address register address
	 * @return register value (16-bit unsigned)
	 */
	public int readRegister(int address) {
		return this.registers.getOrDefault(address, 0);
	}

	/**
	 * Write a single register.
	 *
	 * @param address register address
	 * @param value   register value (16-bit unsigned)
	 */
	public void writeRegister(int address, int value) {
		this.registers.put(address, value & 0xFFFF);
	}

	/**
	 * Update state registers based on current charging state.
	 *
	 * @param state        current charging state
	 * @param evConnected  is EV connected (UCP <= 10V)
	 * @param currentL1    phase 1 current in Ampere (0-80)
	 * @param currentL2    phase 2 current in Ampere (0-80)
	 * @param currentL3    phase 3 current in Ampere (0-80)
	 */
	public void updateStateRegisters(ChargingState state, boolean evConnected, int currentL1, int currentL2,
			int currentL3) {

		// Register 0x0033: Bit 39 (high bit of high byte) = EV connected
		int reg0x0033 = evConnected ? 0x0100 : 0x0000;
		this.registers.put(0x0033, reg0x0033);

		// Register 0x0034: High byte = state, low byte = ICT1
		int stateValue = state.getValue() & 0xFF;
		int ict1 = (currentL1 >= 0 && currentL1 <= 80) ? currentL1 : 0x64;
		int reg0x0034 = (stateValue << 8) | ict1;
		this.registers.put(0x0034, reg0x0034);

		// Register 0x0035: High byte = ICT2, low byte = ICT3
		int ict2 = (currentL2 >= 0 && currentL2 <= 80) ? currentL2 : 0x64;
		int ict3 = (currentL3 >= 0 && currentL3 <= 80) ? currentL3 : 0x64;
		int reg0x0035 = (ict2 << 8) | ict3;
		this.registers.put(0x0035, reg0x0035);
	}

	/**
	 * Get the current Icmax setpoint from register 0x0014.
	 *
	 * @return duty cycle percentage × 10 (0x0050...0x03E8)
	 */
	public int getIcmaxSetpoint() {
		return this.registers.getOrDefault(0x0014, 0);
	}

	/**
	 * Set the Icmax setpoint in register 0x0014.
	 *
	 * @param dutyCycleX10 duty cycle percentage × 10
	 */
	public void setIcmaxSetpoint(int dutyCycleX10) {
		this.registers.put(0x0014, dutyCycleX10 & 0xFFFF);
	}

	/**
	 * Get all registers as a map (for debugging).
	 *
	 * @return copy of register map
	 */
	public Map<Integer, Integer> getAllRegisters() {
		return new HashMap<>(this.registers);
	}

	/**
	 * Get device ID.
	 *
	 * @return device ID
	 */
	public int getDeviceId() {
		return this.deviceId;
	}

	/**
	 * Get firmware version string.
	 *
	 * @return firmware version (e.g., "1.2")
	 */
	public String getFirmwareVersion() {
		return this.firmwareMajor + "." + this.firmwareMinor;
	}
}
