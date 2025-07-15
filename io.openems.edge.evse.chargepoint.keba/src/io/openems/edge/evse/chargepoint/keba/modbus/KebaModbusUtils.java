package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.common.type.TypeUtils.getAsType;
import static java.lang.Math.round;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.meter.api.ElectricityMeter;

public final class KebaModbusUtils {

	private KebaModbusUtils() {
	}

	/**
	 * Converts an unsigned 32-bit integer value to a firmware version string.
	 * 
	 * @param value the unsigned 32-bit integer value representing the firmware
	 *              version
	 * @return the firmware version string in the format "major.minor.patch" or null
	 */
	public static final ElementToChannelConverter CONVERT_FIRMWARE_VERSION = new ElementToChannelConverter(obj -> {
		if (obj == null) {
			return null;
		}
		// Register is defined in its protocol with a maximum 6 digits
		var value = (int) getAsType(INTEGER, obj);

		// Extract major, minor, and patch versions using bit manipulation
		return new StringBuilder() //
				.append(value / 10000) //
				.append(".") //
				.append((value / 100) % 100) //
				.append(".") //
				.append(value % 100) //
				.toString();
	});

	/**
	 * On Update of ACTIVE_POWER, calculates ACTIVE_POWER_L1, L2 and L3 from CURRENT
	 * and VOLTAGE values and distributes the power to match the sum.
	 * 
	 * @param meter       the parent {@link ElectricityMeter}
	 * @param activePower the ActivePower read from Modbus
	 */
	public static void calculateActivePowerL1L2L3(ElectricityMeter meter, Long activePower) {
		var currentL1 = meter.getCurrentL1Channel().getNextValue().get();
		var currentL2 = meter.getCurrentL2Channel().getNextValue().get();
		var currentL3 = meter.getCurrentL3Channel().getNextValue().get();
		var voltageL1 = meter.getVoltageL1Channel().getNextValue().get();
		var voltageL2 = meter.getVoltageL2Channel().getNextValue().get();
		var voltageL3 = meter.getVoltageL3Channel().getNextValue().get();
		final Integer activePowerL1;
		final Integer activePowerL2;
		final Integer activePowerL3;
		if (activePower == null || currentL1 == null || currentL2 == null || currentL3 == null || voltageL1 == null
				|| voltageL2 == null || voltageL3 == null) {
			activePowerL1 = null;
			activePowerL2 = null;
			activePowerL3 = null;
		} else {
			var pL1 = (voltageL1 / 1000F) * (currentL1 / 1000F);
			var pL2 = (voltageL2 / 1000F) * (currentL2 / 1000F);
			var pL3 = (voltageL3 / 1000F) * (currentL3 / 1000F);
			var pSum = pL1 + pL2 + pL3;
			var factor = activePower / pSum / 1000F; // distribute power to match sum
			activePowerL1 = round(pL1 * factor);
			activePowerL2 = round(pL2 * factor);
			activePowerL3 = round(pL3 * factor);
		}
		meter._setActivePowerL1(activePowerL1);
		meter._setActivePowerL2(activePowerL2);
		meter._setActivePowerL3(activePowerL3);
	}
}
