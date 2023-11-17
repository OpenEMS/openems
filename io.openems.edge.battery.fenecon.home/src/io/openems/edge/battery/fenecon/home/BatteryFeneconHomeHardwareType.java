package io.openems.edge.battery.fenecon.home;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.protection.BatteryProtectionDefinition;

public enum BatteryFeneconHomeHardwareType implements OptionsEnum {

	BATTERY_52(52, "Fenecon Home Battery 52Ah", 2200, 42, 49, 14, 3, new FeneconHomeBatteryProtection52(),
			"519100001009", "519110001210"), //
	BATTERY_64(64, "Fenecon Home Battery 64,4Ah", 2800, 40.6f, 49.7f, 14, 5, new FeneconHomeBatteryProtection64(),
			"519100001254", "519110001918"); //

	/**
	 * Defaults to {@link #BATTERY_52} to avoid detection failure with old firmware
	 * versions.
	 */
	public static final BatteryFeneconHomeHardwareType DEFAULT = BATTERY_52;

	public final int capacityPerModule; // [Wh]
	public final float moduleMinVoltage; // [V]; e.g. 3.0 V x 14 Cells per Module
	public final float moduleMaxVoltage; // [V]; e.g. 3.5 V x 14 Cells per Module
	public final int cellsPerModule;
	public final int tempSensorsPerModule;
	public final BatteryProtectionDefinition batteryProtection;
	public final String serialNrPrefixBms;
	public final String serialNrPrefixModule;

	private final int value;
	private final String type;

	private BatteryFeneconHomeHardwareType(int value, String type, int capacityPerModule, float moduleMinVoltage,
			float moduleMaxVoltage, int cellsPerModule, int tempSensorsPerModule,
			BatteryProtectionDefinition batteryProtection, String serialNrPrefixBms, String serialNrPrefixModule) {
		this.value = value;
		this.type = type;
		this.capacityPerModule = capacityPerModule;
		this.moduleMinVoltage = moduleMinVoltage;
		this.moduleMaxVoltage = moduleMaxVoltage;
		this.cellsPerModule = cellsPerModule;
		this.tempSensorsPerModule = tempSensorsPerModule;
		this.batteryProtection = batteryProtection;
		this.serialNrPrefixBms = serialNrPrefixBms;
		this.serialNrPrefixModule = serialNrPrefixModule;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.type;
	}

	@Override
	public OptionsEnum getUndefined() {
		return DEFAULT;
	}
}