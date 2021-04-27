package io.openems.common.channel;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OpenemsType;

public enum Unit {
	// ##########
	// Generic
	// ##########

	/**
	 * No Unit
	 */
	NONE(""),

	/**
	 * Percentage [%], 0-100
	 */
	PERCENT("%"),

	/**
	 * Thousandth [‰], 0-1000
	 */
	THOUSANDTH("‰"),

	/**
	 * On or Off
	 */
	ON_OFF(""),

	// ##########
	// Percolation Q
	// ##########

	/**
	 * Unit of Percolation [m³/s]
	 * */
	CUBICMETER_PER_SECOND("m³/s"),

	/**
	 * Unit of Percolation [m³/h].
	 * */
	CUBICMETER_PER_HOUR("m³/h"),

	/**
	 * Unit of Percolation [l/min].
	 * */
	LITER_PER_MINUTE("l/min"),

	/**
	 * Unit of Percolation [dl/min].
	 * */
	DECILITER_PER_MINUTE("dl/min", LITER_PER_MINUTE, -1),

	// ##########
	// Power
	// ##########

	/**
	 * Unit of Active Power [W]
	 */
	WATT("W"),

	/**
	 * Unit of Active Power [mW]
	 */
	MILLIWATT("mW", WATT, -3),

	/**
	 * Unit of Active Power [kW]
	 */
	KILOWATT("kW", WATT, 3),

	/**
	 * Unit of Energy[Ws]
	 */
	WATT_SECONDS("Ws"),

	/**
	 * Unit of Reactive Power [var]
	 */
	VOLT_AMPERE_REACTIVE("var"),

	/**
	 * Unit of Reactive Power [kvar]
	 */
	KILOVOLT_AMPERE_REACTIVE("kvar", VOLT_AMPERE_REACTIVE, 3),

	/**
	 * Unit of Apparent Power [VA]
	 */
	VOLT_AMPERE("VA"),

	/**
	 * Unit of Apparent Power [kVA]
	 */
	KILOVOLT_AMPERE("kVA", VOLT_AMPERE, 3),

	// ##########
	// Voltage
	// ##########

	/**
	 * Unit of Voltage [V]
	 */
	VOLT("V"),

	/**
	 * Unit of Voltage[dV].
	 */
	DECI_VOLT("dV", VOLT, -1),

	/**
	 * Unit of Voltage [mV]
	 */
	MILLIVOLT("mV", VOLT, -3),

	/**
	 * Unit of Voltage [tthV].
	 * (Used in Chp when transmitting Voltage).
	 */
	TEN_THOUSANDTH_VOLT("tthV", VOLT, -4),

	// ##########
	// Current
	// ##########

	/**
	 * Unit of Current [A]
	 */
	AMPERE("A"),

	/**
	 * Unit of Current [mA]
	 */
	MILLIAMPERE("mA", AMPERE, -3),

	// ##########
	// Electric Charge
	// ##########

	/**
	 * Unit of Electric Charge
	 */
	AMPERE_HOURS("Ah"),

	/**
	 * Unit of Electric Charge
	 */
	MILLIAMPERE_HOURS("mAh", AMPERE_HOURS, -3),

	/**
	 * Unit of Electric Charge
	 */
	KILOAMPERE_HOURS("kAh", AMPERE_HOURS, 3),

	// ##########
	// Energy
	// ##########

	/**
	 * Unit of Energy [Wh]
	 */
	WATT_HOURS("Wh"),

	/**
	 * Unit of Energy [hWh]
	 */
	HECTOWATT_HOURS("hWh", WATT_HOURS, 2),

	/**
	 * Unit of Energy [kWh]
	 */
	KILOWATT_HOURS("kWh", WATT_HOURS, 3),

	/**
	 * Unit of Energy [MWh]
	 */
	MEGAWATT_HOURS("MWh", WATT_HOURS, 6),

	/**
	 * Unit of Reactive Energy [varh]
	 */
	VOLT_AMPERE_REACTIVE_HOURS("varh"),

	/**
	 * Unit of Reactive Energy [kVArh]
	 */
	KILOVOLT_AMPERE_REACTIVE_HOURS("kvarh", VOLT_AMPERE_REACTIVE_HOURS, 3),

	/**
	 * Unit of Energy [Wh/Wp]
	 */
	WATT_HOURS_BY_WATT_PEAK("Wh/Wp"),

	/**
	 * Unit of Apparent Energy [VAh]
	 */
	VOLT_AMPERE_HOURS("VAh"),

	// ##########
	// Frequency
	// ##########

	/**
	 * Unit of Frequency [Hz]
	 */
	HERTZ("Hz"),

	/**
	 * Unit of Frequency [mHz]
	 */
	MILLIHERTZ("mHz", HERTZ, -3),

	// ##########
	// Temperature
	// ##########

	/**
	 * Unit of Temperature [C]
	 */
	DEGREE_CELSIUS("C"),

	/**
	 * Unit of Temperature [dC]
	 */
	DEZIDEGREE_CELSIUS("dC", DEGREE_CELSIUS, -1),

	/**
	 * Unit of Temperature [K]
	 */
	DEGREE_KELVIN("K"),

	/**
	 * Unit of Temperature [dK]
	 */
	DEZIDEGREE_KELVIN("dK", DEGREE_KELVIN, -1),

	// ##########
	// Time
	// ##########

	/**
	 * Unit of Time [s]
	 */
	SECONDS("sec"),

	/**
	 * Unit of Time [cs]
	 */
	CENTISECONDS("cs", SECONDS, -2),

	/**
	 * Unit of Time [ms]
	 */
	MILLISECONDS("ms", SECONDS, -3),

	/**
	 * Unit of Time
	 */
	MINUTE("min"),

	/**
	 * Unit of Time
	 */
	HOUR("h"),

	// ##########
	// Cumulated Time
	// ##########

	/**
	 * Unit of cumulated time [s]
	 */
	CUMULATED_SECONDS("sec"),


	// ##########
	// Resistance
	// ##########

	/**
	 * Unit of Resistance [Ohm]
	 */
	OHM("Ohm"),

	/**
	 * Unit of Resistance [kOhm]
	 */
	KILOOHM("kOhm", OHM, 3),

	/**
	 * Unit of Resistance [mOhm]
	 */
	MILLIOHM("mOhm", OHM, -3),

	/**
	 * Unit of Resistance [uOhm]
	 */
	MICROOHM("uOhm", OHM, -6),

	// ##########
	// Pressure
	// ##########
	/**
	 * Unit of Pressure[Pa].
	 */
	PASCAL("Pa"),

	/**
	 * Unit of Pressure[hPa].
	 */
	HECTO_PASCAL("hPa", PASCAL, 100),

	/**
	 * Unit of Pressure [bar].
	 */
	BAR("bar"),

	/**
	 * Unit of Pressure [dbar]
	 */
	DECI_BAR("dbar", BAR, -1),

	/**
	 * Unit of Pressure [cbar]
	 */
	CENTI_BAR("cbar", BAR, -2),

	// ##########
	// Rotation
	// ##########
	/**
	 * Unit of Rotation per seconds.
	 */
	ROTATION_PER_SECONDS("R/sec"),

	/**
	 * Unit of Rotation per minute.
	 */

	ROTATION_PER_MINUTE("R/min"),

	// ##########
	// Angle
	// ##########

	/**
	 * Unit of Degree [°].
	 *
	 * */
	DEGREE("°"),

	MILLI_DEGREE("m°", DEGREE, -3),

	// #########
	// Volume
	// ########

	/**
	 * Unit volume [m³]
	 * */
	CUBIC_METER("m³"),

	/**
	 * Unit volume [l]
	 * */
	LITRES("l", CUBIC_METER, -3),

	// #########
	// Wireless signal strength
	// ########

	/**
	 * Unit of wireless signal strength [dBm]
	 * */
	DECIBEL_MILLIWATT("dBm");

	private final Unit baseUnit;
	private final int scaleFactor;
	private final String symbol;

	private Unit(String symbol) {
		this(symbol, null, 0);
	}

	private Unit(String symbol, Unit baseUnit, int scaleFactor) {
		this.symbol = symbol;
		this.baseUnit = baseUnit;
		this.scaleFactor = scaleFactor;
	}

	public Unit getBaseUnit() {
		return baseUnit;
	}

	public int getScaleFactor() {
		return this.scaleFactor;
	}

	public int getAsBaseUnit(int value) {
		return (int) (value * Math.pow(10, this.scaleFactor));
	}

	public String getSymbol() {
		return symbol;
	}

	public String format(Object value, OpenemsType type) {
		switch (this) {
			case NONE:
				return value.toString();
			case PERCENT:
			case THOUSANDTH:
			case CUBICMETER_PER_SECOND:
			case CUBICMETER_PER_HOUR:
			case LITER_PER_MINUTE:
			case DECILITER_PER_MINUTE:
			case WATT:
			case MILLIWATT:
			case KILOWATT:
			case WATT_SECONDS:
			case VOLT_AMPERE_REACTIVE:
			case KILOVOLT_AMPERE_REACTIVE:
			case VOLT_AMPERE:
			case KILOVOLT_AMPERE:
			case VOLT:
			case DECI_VOLT:
			case MILLIVOLT:
			case TEN_THOUSANDTH_VOLT:
			case AMPERE:
			case MILLIAMPERE:
			case AMPERE_HOURS:
			case MILLIAMPERE_HOURS:
			case KILOAMPERE_HOURS:
			case WATT_HOURS:
			case HECTOWATT_HOURS:
			case KILOWATT_HOURS:
			case MEGAWATT_HOURS:
			case VOLT_AMPERE_REACTIVE_HOURS:
			case KILOVOLT_AMPERE_REACTIVE_HOURS:
			case WATT_HOURS_BY_WATT_PEAK:
			case VOLT_AMPERE_HOURS:
			case HERTZ:
			case MILLIHERTZ:
			case DEGREE_CELSIUS:
			case DEZIDEGREE_CELSIUS:
			case DEGREE_KELVIN:
			case DEZIDEGREE_KELVIN:
			case SECONDS:
			case CENTISECONDS:
			case MILLISECONDS:
			case MINUTE:
			case HOUR:
			case CUMULATED_SECONDS:
			case OHM:
			case KILOOHM:
			case MILLIOHM:
			case MICROOHM:
			case PASCAL:
			case HECTO_PASCAL:
			case BAR:
			case DECI_BAR:
			case CENTI_BAR:
			case ROTATION_PER_SECONDS:
			case ROTATION_PER_MINUTE:
			case DEGREE:
			case MILLI_DEGREE:
			case CUBIC_METER:
			case LITRES:
			case DECIBEL_MILLIWATT:
				return value + " " + this.symbol;
			case ON_OFF:
				boolean booleanValue = (Boolean) value;
				return booleanValue ? "ON" : "OFF";
		}
		return "FORMAT_ERROR"; // should never happen, if 'switch' is complete
	}

	public String formatAsBaseUnit(Object value, OpenemsType type) {
		if (this.baseUnit != null) {
			switch (type) {
				case SHORT:
				case INTEGER:
				case LONG:
				case FLOAT:
				case DOUBLE:
					return this.baseUnit.formatAsBaseUnit(this.getAsBaseUnit((int) value), type);
				case BOOLEAN:
				case STRING:
					return this.baseUnit.formatAsBaseUnit(value, type);
			}
		} else {
			this.format(value, type);
		}
		return "FORMAT_ERROR"; // should never happen, if 'switch' is complete
	}

	@Override
	public String toString() {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name()) + //
				(this.symbol.isEmpty() ? "" : " [" + this.symbol + "]");
	}
}
