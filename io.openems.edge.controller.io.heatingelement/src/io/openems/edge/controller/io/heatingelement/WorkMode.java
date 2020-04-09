
package io.openems.edge.controller.io.heatingelement;

public enum WorkMode {
	/**
	 * Time (= run at least Minimum Time).
	 */
	TIME,
	/**
	 * kWh (= use at least Minimum kWh).
	 */
	KWH;
}