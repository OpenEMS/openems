package io.openems.edge.goodwe.common.enums;

import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType.BATTERY_52;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType.BATTERY_64;

import java.util.function.Function;
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
	FENECON_FHI_10_DAH(30, "FENECON FHI 10 DAH", Series.ET, //
			authorisedLimit(25, 25, 0), serialNrFilter("010K", "ETU"), notHomeBattery52Ah()), //
	FENECON_FHI_20_DAH(120, "FENECON FHI 20 DAH", Series.ETT, //
			authorisedLimit(50, 0, 50), serialNrFilter("020K", "ETT"), notHomeBattery64Ah()), //
	FENECON_FHI_29_9_DAH(130, "FENECON FHI 30 DAH", Series.ETT, //
			authorisedLimit(50, 0, 50), home30Filter("29K9", "030K"), notHomeBattery64Ah()),
	FENECON_GEN2_6K(140, "FENECON ET Gen2 6K", Series.EUB, //
			authorisedLimit(40, 25, 40), serialNrFilter("6000", "EUB"), notHomeBattery52Or64Ah()),
	FENECON_GEN2_10K(150, "FENECON ET Gen2 10K", Series.EUB, //
			authorisedLimit(40, 25, 40), serialNrFilter("010K", "EUB"), notHomeBattery52Or64Ah()),
	FENECON_GEN2_15K(160, "FENECON ET Gen2 15K", Series.EUB, //
			authorisedLimit(40, 25, 40), serialNrFilter("015K", "EUB"), notHomeBattery52Or64Ah()); //

	public static enum Series {
		UNDEFINED, BT, ET, ETT, EUB;
	}

	private final int value;
	private final String option;
	private final Series series;
	public final Function<BatteryFeneconHomeHardwareType, Integer> maxDcCurrent;
	public final ThrowingFunction<String, Boolean, Exception> serialNrFilter;
	public final Predicate<BatteryFeneconHomeHardwareType> isInvalidBattery;

	private GoodWeType(int value, String option, Series series,
			Function<BatteryFeneconHomeHardwareType, Integer> maxDcCurrent,
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
		this(value, option, series, (notUsed) -> maxDcCurrent, (t) -> false, (t) -> false);
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
				.filter(t -> {
					try {
						return serialNrFilter(t, "ETT").apply(serialNr);
					} catch (Exception e) {
						return false;
					}
				}) //
				.findFirst() //
				.isPresent();
	}

	/**
	 * GoodWe serial number filter.
	 * 
	 * <p>
	 * Check if a serialNr matches a given string at the common position.
	 * 
	 * @param ratedPower rated power of the inverter
	 * @param seriesCode internal inverter model series code
	 * @return filter function
	 */
	public static ThrowingFunction<String, Boolean, Exception> serialNrFilter(String ratedPower, String seriesCode) {
		return serialNr -> serialNr.substring(1, 5).equals(ratedPower) && serialNr.substring(5, 8).equals(seriesCode);
	}

	private static Predicate<BatteryFeneconHomeHardwareType> notHomeBattery52Ah() {
		return (batteryType) -> batteryType != BATTERY_52;
	}

	private static Predicate<BatteryFeneconHomeHardwareType> notHomeBattery64Ah() {
		return (batteryType) -> batteryType != BATTERY_64;
	}

	private static Predicate<BatteryFeneconHomeHardwareType> notHomeBattery52Or64Ah() {
		return (batteryType) -> batteryType != BATTERY_52 && batteryType != BATTERY_64;
	}

	/**
	 * Maximum authorized DC current for a given battery type.
	 * 
	 * @param defaultLimit     default limit if it is not a known battery
	 * @param limit52AhBattery maximum DC current using a 52Ah battery
	 * @param limit64AhBattery maximum DC current using a 64Ah battery
	 * @return maximum DC current
	 */
	public static Function<BatteryFeneconHomeHardwareType, Integer> authorisedLimit(int defaultLimit,
			int limit52AhBattery, int limit64AhBattery) {
		return (batteryType) -> {
			if (batteryType == null) {
				return defaultLimit;
			}
			return switch (batteryType) {
			case BATTERY_52 -> limit52AhBattery;
			case BATTERY_64 -> limit64AhBattery;
			};
		};
	}
}
