package io.openems.edge.goodwe.common.enums;

import java.util.function.Predicate;
import java.util.stream.Stream;

import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType;

public enum GoodWeType implements OptionsEnum {
	UNDEFINED(-1, "Undefined", Series.UNDEFINED, 25), //
	GOODWE_10K_BT(10, "GoodWe GW10K-BT", Series.BT, 25), //
	GOODWE_8K_BT(11, "GoodWe GW8K-BT", Series.BT, 25), //
	GOODWE_5K_BT(12, "GoodWe GW5K-BT", Series.BT, 25), //
	GOODWE_10K_ET(20, "GoodWe GW10K-ET", Series.ET, 25), //
	GOODWE_8K_ET(21, "GoodWe GW8K-ET", Series.ET, 25), //
	GOODWE_5K_ET(22, "GoodWe GW5K-ET", Series.ET, 25), //
	FENECON_FHI_10_DAH(30, "FENECON FHI 10 DAH", Series.ET, 25, position2Filter("10"),
			(batteryType) -> batteryType != BatteryFeneconHomeHardwareType.BATTERY_52), //
	FENECON_FHI_20_DAH(120, "FENECON FHI 20 DAH", Series.ET, 50, position2Filter("20"),
			(batteryType) -> batteryType != BatteryFeneconHomeHardwareType.BATTERY_64), //
	FENECON_FHI_29_9_DAH(130, "FENECON FHI 30 DAH", Series.ET, 50, home30Filter("29K9", "30"),
			(batteryType) -> batteryType != BatteryFeneconHomeHardwareType.BATTERY_64); //

	public static enum Series {
		UNDEFINED, BT, ET;
	}

	// TODO: Change logic of isValidHomeBattery to invalidBattery

	private final int value;
	private final String option;
	private final Series series;
	public final int maxDcCurrent; // [A]
	public final ThrowingFunction<String, Boolean, Exception> serialNrFilter;
	public final Predicate<BatteryFeneconHomeHardwareType> isInvalidBattery;

	private GoodWeType(int value, String option, Series series, int maxDcCurrent,
			ThrowingFunction<String, Boolean, Exception> serialNrFilter,
			Predicate<BatteryFeneconHomeHardwareType> isInvalidBattery) {
		this.value = value;
		this.option = option;
		this.series = series;
		this.maxDcCurrent = maxDcCurrent;
		this.serialNrFilter = serialNrFilter;
		this.isInvalidBattery = isInvalidBattery;
	}

	private GoodWeType(int value, String option, Series series, int maxDcCurrent) {
		// No serial number filter and battery dependency
		this(value, option, series, maxDcCurrent, (t) -> false, (t) -> false);
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	/**
	 * Is this GoodWe a ET-Series or BT-Series.
	 *
	 * @return the Series or UNDEFINED if unknown
	 */
	public Series getSeries() {
		return this.series;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	/**
	 * Position two filter.
	 * 
	 * <p>
	 * Check if a serialNr matches a given string at the common position.
	 * 
	 * @param match string identifier
	 * @return filter function
	 */
	public static ThrowingFunction<String, Boolean, Exception> position2Filter(String match) {
		return serialNr -> match.equals(serialNr.substring(2, 4));
	}

	/**
	 * Home 30 filter.
	 * 
	 * <p>
	 * Check if a serialNr matches a given string at the common position defined for
	 * goodwe30.
	 * 
	 * @param match string identifier
	 * @return filter function
	 */
	public static ThrowingFunction<String, Boolean, Exception> home30Filter(String... match) {
		return serialNr -> Stream.of(match) //
				.filter(t -> t.equals(serialNr.substring(2, 4)) || serialNr.substring(1, 5).contains(t)) //
				.findFirst() //
				.isPresent();
	}
}