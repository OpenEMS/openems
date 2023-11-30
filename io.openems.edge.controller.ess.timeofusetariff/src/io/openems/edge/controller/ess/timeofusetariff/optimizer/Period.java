package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public record Period(ZonedDateTime time, int production, int consumption, int essInitial, int essMaxCharge,
		int essMaxDischarge, StateMachine state, int essChargeDischarge, int grid, float price, double cost) {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	/**
	 * Gets the {@link Period#toString()} header.
	 * 
	 * @return header
	 */
	public static String header() {
		return "Time Production Consumption EssInitial EssMaxCharge EssMaxDischarge State State EssChargeDischarge Grid Price Cost";
	}

	/**
	 * Gets the Period as String.
	 */
	public String toString() {
		return String.format(Locale.GERMAN, "%s %d %d %d %d %d %d %s %d %d %.2f %.4f", //
				this.time.format(FORMATTER), this.production, this.consumption, this.essInitial, this.essMaxCharge,
				this.essMaxDischarge, //
				this.state.getValue(), this.state, //
				this.essChargeDischarge, this.grid, this.price, this.cost);
	}
}