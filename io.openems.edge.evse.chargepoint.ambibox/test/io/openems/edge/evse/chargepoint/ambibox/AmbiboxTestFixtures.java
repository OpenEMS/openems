package io.openems.edge.evse.chargepoint.ambibox;

import java.util.Arrays;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.evse.chargepoint.ambibox.enums.Connector;

/**
 * Provides {@link DummyModbusBridge} fixtures with pre-filled Modbus registers
 * of the Ambibox charger, connector 1.
 */
public class AmbiboxTestFixtures {

	private static final int INPUT_BASE = Connector.CONNECTOR_1.inputBaseAddress;
	private static final int HOLDING_BASE = Connector.CONNECTOR_1.holdingBaseAddress;

	/** Number of Input Registers of one EV charger block; offsets 0..102. */
	private static final int INPUT_REGISTERS = 104;

	private final int[] inputRegisters = new int[INPUT_REGISTERS];

	private AmbiboxTestFixtures() {
		Arrays.fill(this.inputRegisters, 0);
	}

	private AmbiboxTestFixtures setInt(int offset, int value) {
		this.inputRegisters[offset] = value >>> 16;
		this.inputRegisters[offset + 1] = value & 0xFFFF;
		return this;
	}

	private AmbiboxTestFixtures setFloat(int offset, float value) {
		return this.setInt(offset, Float.floatToIntBits(value));
	}

	private static AmbiboxTestFixtures chargingSession() {
		var f = new AmbiboxTestFixtures();

		f.setInt(0, 0) // Sleep: awake
				.setFloat(2, 30.0F) // Current AC
				.setFloat(4, 10.0F) // Current AC Phase 1
				.setFloat(6, 10.0F) // Current AC Phase 2
				.setFloat(8, 10.0F) // Current AC Phase 3
				.setFloat(10, 230.0F) // Voltage AC
				.setFloat(12, 230.0F) // Voltage AC Phase 1
				.setFloat(14, 230.0F) // Voltage AC Phase 2
				.setFloat(16, 230.0F) // Voltage AC Phase 3
				.setInt(18, 6900) // Power AC
				.setFloat(20, 0.98F) // Power Factor
				.setFloat(22, 50.0F) // Grid Frequency
				.setInt(24, 0) // Energy AC
				.setFloat(26, 123450000.0F) // Energy AC import
				.setFloat(28, 1000000.0F) // Energy AC export
				.setFloat(30, 400.0F) // Voltage DC
				.setFloat(32, 17.25F) // Current DC
				.setInt(34, 6900) // Power DC
				.setInt(36, 3) // Number Phases
				.setInt(38, 1400) // Minimum PowerAC
				.setInt(40, 11000) // Maximum PowerAC
				.setInt(42, 8) // Inverter State: STARTED
				.setInt(44, 0) // Inverter Error
				.setFloat(46, 31.2F) // Inverter Temperature
				.setFloat(48, 72000.0F) // Capacity
				.setFloat(50, 5.0F) // Min State of Charge
				.setFloat(52, 95.0F) // Max State of Charge
				.setFloat(54, 59.0F) // State of Charge
				.setFloat(56, 98.0F) // State of Health
				.setInt(58, 1257) // Time to full SoC
				.setInt(60, 109) // Number Cycles
				.setInt(62, 100) // Min ChargePower
				.setInt(64, 11000) // Max ChargePower
				.setInt(66, 200) // Min DischargePower
				.setInt(68, 7000) // Max DischargePower
				.setInt(70, 2) // Battery State: CHARGE
				.setFloat(72, 20.9F) // Battery Temperature
				.setInt(74, 0) // Battery Error
				.setInt(76, 1) // Control Mode: CONTROLLABLE
				.setInt(78, 6800) // Power DC Battery
				.setInt(80, 3) // Charge Protocol: ISO_15118_2
				.setInt(82, 5) // Session State: CHARGE_LOOP
				.setInt(84, 1) // EV connected
				.setInt(86, 7305) // Seconds to departure
				.setFloat(88, 80.0F) // Departure SoC
				.setInt(90, 4300) // Departure Energy
				.setInt(92, 4300) // Min Energy Request
				.setInt(94, 8600) // Max Energy Request
				.setInt(96, 0) // EvCharger Error
				.setFloat(98, 5000.0F) // Energy AC import session
				.setFloat(100, 0.0F) // Energy AC export session
				.setInt(102, 0); // Replug Required

		return f;
	}

	/**
	 * Creates a {@link DummyModbusBridge} of an active charging session.
	 *
	 * <p>
	 * The charger is awake, an electric vehicle is connected and charging in
	 * CHARGE_LOOP state without any errors. The 'Target PowerAC' Holding Register
	 * contains the read-back value of an 11000 W charging set-point (-11000).
	 *
	 * @return the {@link DummyModbusBridge}
	 */
	public static DummyModbusBridge createChargingBridge() {
		return chargingSession() //
				.buildBridge(-11000);
	}

	/**
	 * Creates a {@link DummyModbusBridge} of a charging session with a
	 * 'PLUG_TEMPERATURE' EvCharger Error while the charger is asleep.
	 *
	 * @return the {@link DummyModbusBridge}
	 */
	public static DummyModbusBridge createErrorBridge() {
		var f = chargingSession();
		f.setInt(0, 1); // Sleep: asleep
		f.setInt(96, 1 << 3); // EvCharger Error: PLUG_TEMPERATURE
		return f.buildBridge(0);
	}

	private DummyModbusBridge buildBridge(int targetPowerAc) {
		var bridge = new DummyModbusBridge("modbus0");
		bridge.withInputRegisters(INPUT_BASE, this.inputRegisters);
		bridge.withRegister(HOLDING_BASE, targetPowerAc >>> 16);
		bridge.withRegister(HOLDING_BASE + 1, targetPowerAc & 0xFFFF);
		return bridge;
	}
}
