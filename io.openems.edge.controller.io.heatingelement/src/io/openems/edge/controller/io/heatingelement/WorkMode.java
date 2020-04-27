
package io.openems.edge.controller.io.heatingelement;

public enum WorkMode {
	/**
	 * Time (= run at least Minimum Time).
	 */
	TIME,
	/**
	 * kWh (= use at least Minimum kWh).
	 */
	KWH,
	/**
	 * None (= only run on excess power, no guaranteed Minimum Time or kWh)
	 */
	NONE;
}