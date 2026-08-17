package io.openems.edge.fronius.enums;

import java.util.Map;

import io.openems.common.types.OptionsEnum;

/**
 * Predefined battery models with voltage limits per number of modules.
 *
 * <p>
 * Select CUSTOM to enter voltage values manually in the configuration.
 */
public enum BatteryPreset implements OptionsEnum {
	CUSTOM(0, "Custom"), //
	BYD_HVS(1, "BYD Battery-Box Premium HVS"), //
	BYD_HVM(2, "BYD Battery-Box Premium HVM"), //
	FRONIUS_RESERVA(3, "Fronius Reserva"), //
	FRONIUS_RESERVA_PRO(4, "Fronius Reserva Pro"), //
	LG_BATTERY_FLEX(5, "LG Battery Flex"); //

	private final int value;
	private final String name;

	/**
	 * Voltage limits: key = number of modules, value = [minVoltage, maxVoltage].
	 */
	private static final Map<BatteryPreset, Map<Integer, int[]>> VOLTAGE_MAP = Map.of(//
			BYD_HVS, Map.of(//
					2, new int[] { 160, 240 }, //
					3, new int[] { 240, 360 }, //
					4, new int[] { 320, 480 }, //
					5, new int[] { 400, 600 }), //
			BYD_HVM, Map.of(//
					3, new int[] { 120, 177 }, //
					4, new int[] { 160, 236 }, //
					5, new int[] { 200, 295 }, //
					6, new int[] { 240, 354 }, //
					7, new int[] { 280, 413 }, //
					8, new int[] { 320, 472 }), //
			FRONIUS_RESERVA, Map.of(//
					2, new int[] { 179, 231 }, //
					3, new int[] { 269, 346 }, //
					4, new int[] { 358, 461 }, //
					5, new int[] { 448, 576 }), //
			FRONIUS_RESERVA_PRO, Map.of(//
					3, new int[] { 165, 241 }, //
					4, new int[] { 220, 321 }, //
					5, new int[] { 275, 402 }, //
					6, new int[] { 330, 482 }, //
					7, new int[] { 385, 562 }, //
					8, new int[] { 440, 642 }), //
			LG_BATTERY_FLEX, Map.of(//
					2, new int[] { 192, 266 }, //
					3, new int[] { 288, 398 }, //
					4, new int[] { 384, 531 }) //
	);

	BatteryPreset(int value, String name) {
		this.value = value;
		this.name = name;
	}

	/**
	 * Returns the minimum discharge voltage for the given number of modules, or
	 * null if the preset is CUSTOM or the module count is not valid.
	 *
	 * @param numberOfModules the number of battery modules
	 * @return the minimum discharge voltage in V, or null
	 */
	public Integer getDischargeMinVoltage(int numberOfModules) {
		var moduleMap = VOLTAGE_MAP.get(this);
		if (moduleMap == null) {
			return null;
		}
		var voltages = moduleMap.get(numberOfModules);
		if (voltages == null) {
			return null;
		}
		return voltages[0];
	}

	/**
	 * Returns the maximum charge voltage for the given number of modules, or null
	 * if the preset is CUSTOM or the module count is not valid.
	 *
	 * @param numberOfModules the number of battery modules
	 * @return the maximum charge voltage in V, or null
	 */
	public Integer getChargeMaxVoltage(int numberOfModules) {
		var moduleMap = VOLTAGE_MAP.get(this);
		if (moduleMap == null) {
			return null;
		}
		var voltages = moduleMap.get(numberOfModules);
		if (voltages == null) {
			return null;
		}
		return voltages[1];
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return CUSTOM;
	}
}
