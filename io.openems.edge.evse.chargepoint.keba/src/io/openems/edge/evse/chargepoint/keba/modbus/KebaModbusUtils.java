package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.TypeUtils.getAsType;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbus.FIRMWARE_ENERGY_SCALE_MIN_BUG;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbus.FIRMWARE_OUTDATED_WARNING;
import static java.lang.Math.round;

import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.chargepoint.keba.common.KebaModbus;
import io.openems.edge.meter.api.ElectricityMeter;

public final class KebaModbusUtils {

	public final ElementToChannelConverter energyScaleFactor;

	public KebaModbusUtils(KebaModbus keba) {
		this.energyScaleFactor = createEnergyScaleFactor(keba);
	}

	/**
	 * Converts an unsigned 32-bit integer value to a firmware version string and
	 * their individual firmware parts & sets the individual channels.
	 * 
	 * <ul>
	 * <li>{@link KebaModbus.ChannelId#FIRMWARE}
	 * <li>{@link KebaModbus.ChannelId#FIRMWARE_MAJOR}
	 * <li>{@link KebaModbus.ChannelId#FIRMWARE_MINOR}
	 * <li>{@link KebaModbus.ChannelId#FIRMWARE_PATCH}
	 * <li>{@link KebaModbus.ChannelId#DEVICE_SOFTWARE_OUTDATED}
	 * </ul>
	 * 
	 * @param parent the {@link KebaModbus} component
	 * @param obj    value
	 */
	public static void handleFirmwareVersion(KebaModbus parent, Object obj) {
		// Register is defined in its protocol with a maximum 6 digits
		final var value = obj != null //
				? (int) getAsType(INTEGER, obj) //
				: null;

		// Parse major, minor and patch version parts
		final var major = value != null //
				? value / 10000 //
				: null;
		final var minor = value != null //
				? (value / 100) % 100 //
				: null;
		final var patch = value != null //
				? value % 100 //
				: null;
		setValue(parent, KebaModbus.ChannelId.FIRMWARE_MAJOR, major);
		setValue(parent, KebaModbus.ChannelId.FIRMWARE_MINOR, minor);
		setValue(parent, KebaModbus.ChannelId.FIRMWARE_PATCH, patch);

		// Create full version string
		final SemanticVersion firmware;
		if (major != null && minor != null && patch != null) {
			firmware = new SemanticVersion(major, minor, patch);
		} else {
			firmware = null;
		}
		final var string = firmware != null //
				? firmware.toString() //
				: null;
		setValue(parent, KebaModbus.ChannelId.FIRMWARE, string);

		// Set DEVICE_SOFTWARE_OUTDATED Channel
		final boolean deviceSoftwareOutdated;
		if (firmware != null) {
			deviceSoftwareOutdated = !firmware.isAtLeast(FIRMWARE_OUTDATED_WARNING);
		} else {
			deviceSoftwareOutdated = false;
		}
		setValue(parent, KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED, deviceSoftwareOutdated);
	}

	/**
	 * Creates a {@link ElementToChannelConverter} that applies the correct
	 * scale-factor for a Modbus Energy Register value.
	 * 
	 * <p>
	 * This handles a "known bug" in KEBA P40: In software versions below 1.2.1 the
	 * registers 1502 and 1036 falsely report the value in [Wh] instead of [0.1 Wh]
	 * 
	 * @param parent the {@link KebaModbus} component
	 * @return a {@link ElementToChannelConverter}
	 */
	private static ElementToChannelConverter createEnergyScaleFactor(KebaModbus parent) {
		return new ElementToChannelConverter(t -> {
			final var firmware = SemanticVersion.fromStringOrZero(parent.getFirmware().get());
			if (firmware == SemanticVersion.ZERO) {
				return null; // Firmware string is null or invalid
			}
			final int value = TypeUtils.getAsType(INTEGER, t);
			if (firmware.isAtLeast(FIRMWARE_ENERGY_SCALE_MIN_BUG)) {
				return value * 0.1;
			} else {
				return value;
			}
		});
	}

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
