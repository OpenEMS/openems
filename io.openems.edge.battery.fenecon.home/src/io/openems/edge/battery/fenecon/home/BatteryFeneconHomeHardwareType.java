package io.openems.edge.battery.fenecon.home;

import static io.openems.common.utils.FunctionUtils.alwaysReturn;

import java.time.LocalDate;
import java.util.function.Function;

import io.openems.common.types.OptionsEnum;

public enum BatteryFeneconHomeHardwareType implements OptionsEnum {

	BATTERY_52(52, "Fenecon Home Battery 52Ah", //
			2200, 42, 49, 14, 3, //
			alwaysReturn("519100001009"), //
			alwaysReturn("519110001210")), //
	BATTERY_64(64, "Fenecon Home Battery 64,4Ah", //
			2800, 40.6f, 49.7f, 14, 5, //
			alwaysReturn("519100001254"), //
			date -> {
				// prefix changed with new modules, we distinguish them by their production date
				if (!date.isAfter(LocalDate.of(2025, 5, 30))) {
					return "519110001918";
				}
				return "519110002567";
			}); //

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
	public final Function<LocalDate, String> serialNrPrefixBms;
	public final Function<LocalDate, String> serialNrPrefixModule;
	public final int value;

	private final String type;

	private BatteryFeneconHomeHardwareType(int value, String type, int capacityPerModule, float moduleMinVoltage,
			float moduleMaxVoltage, int cellsPerModule, int tempSensorsPerModule,
			Function<LocalDate, String> serialNrPrefixBms, Function<LocalDate, String> serialNrPrefixModule) {
		this.value = value;
		this.type = type;
		this.capacityPerModule = capacityPerModule;
		this.moduleMinVoltage = moduleMinVoltage;
		this.moduleMaxVoltage = moduleMaxVoltage;
		this.cellsPerModule = cellsPerModule;
		this.tempSensorsPerModule = tempSensorsPerModule;
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