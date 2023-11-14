package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.common.type.TypeUtils.abs;
import static io.openems.edge.common.type.TypeUtils.max;
import static io.openems.edge.common.type.TypeUtils.min;
import static io.openems.edge.common.type.TypeUtils.subtract;

import java.util.ArrayList;
import java.util.List;

public class Schedule {

	/*
	 * List of periods with charge discharge data to optimize ESS. Total number of
	 * periods are limited to the number of price periods that are existing.
	 */
	protected List<Period> periods = new ArrayList<>();

	/* ESS Usable capacity. */
	private int essUsableEnergy;

	/* Available Energy in the battery based on current SoC. */
	private int essInitialEnergy;

	/* Maximum Discharge energy of the battery in a period. */
	private int essMaxDischargeEnergyPerPeriod;

	/* Maximum Charge energy of the battery in a period. */
	private int essMaxChargeEnergyPerPeriod;

	/* Maximum Charge energy of the battery from grid in a period. */
	private int maxAllowedChargeEnergyFromGrid;

	/* Control mode defined by user while configuring. */
	private ControlMode controlMode;

	public static class Period {
		protected final int index;
		protected final float price;
		protected final int productionPrediction;
		protected final int consumptionPrediction;
		protected final int maxDischargeEnergyPerPeriod;
		protected final int requiredEnergy;

		protected Integer essInitialEnergy;
		protected Integer chargeDischargeEnergy;

		private Period(int index, Integer essInitialEnergy, Integer chargeDischargeEnergy, Integer gridEnergy,
				float price, int productionPrediction, int consumptionPrediction, int maxDischargeEnergyPerPeriod) {
			this.index = index;
			this.essInitialEnergy = essInitialEnergy;
			this.chargeDischargeEnergy = chargeDischargeEnergy;
			this.price = price;
			this.productionPrediction = productionPrediction;
			this.consumptionPrediction = consumptionPrediction;
			this.maxDischargeEnergyPerPeriod = maxDischargeEnergyPerPeriod;
			this.requiredEnergy = this.consumptionPrediction - this.productionPrediction;
		}

		/**
		 * This method returns the 'Grid energy' (Grid buy/ Grid sell) for the
		 * particular period.
		 * 
		 * @return The Grid energy.
		 */
		public Integer gridEnergy() {
			if (this.chargeDischargeEnergy == null) {
				return null;
			}
			return this.consumptionPrediction - this.productionPrediction - this.chargeDischargeEnergy;
		}

		/**
		 * Method to check if force charge is scheduled for the period.
		 * 
		 * @return True if force charge is scheduled and False otherwise.
		 */
		public boolean isChargeFromGridScheduled() {
			return this.chargeDischargeEnergy < 0 && this.gridEnergy() > 0;
		}

		/**
		 * Method to check if delay discharge is scheduled.
		 * 
		 * @return True if delaying discharge is scheduled and False otherwise.
		 */
		public boolean isDelayDischargeScheduled() {
			return this.chargeDischargeEnergy < this.essInitialEnergy //
					&& this.gridEnergy() > 0 //
					&& this.requiredEnergy < this.essInitialEnergy //
					&& this.chargeDischargeEnergy != this.maxDischargeEnergyPerPeriod;
		}

		/**
		 * Is excess PV prodcution available?.
		 *         than the Consumption.
		 */
		public boolean isExcessPvAvailable() {
			return this.requiredEnergy < 0;
		}
		/**
		 * Returns the state of the period.
		 * 
		 * @param controlMode mode enabled by the customer.
		 * @return the state.
		 */
		public StateMachine getStateMachine(ControlMode controlMode) {

			var stateMachine = StateMachine.ALLOWS_DISCHARGE;

			if (this.isExcessPvAvailable() || this.essInitialEnergy == 0) {
				stateMachine = StateMachine.CHARGE_FROM_PV;
			}
			switch (controlMode) {
			case CHARGE_CONSUMPTION:
				if (this.isChargeFromGridScheduled()) {
					stateMachine = StateMachine.CHARGE_FROM_GRID;
				}
				break;
			case DELAY_DISCHARGE:
				if (this.isDelayDischargeScheduled()) {
					stateMachine = StateMachine.DELAY_DISCHARGE;
				}
				break;
			}

			return stateMachine;
		}
	}

	/**
	 * Creates the schedule with periods.
	 * 
	 * @param controlMode            the {@link ControlMode}
	 * @param riskLevel              the {@link RiskLevel}
	 * @param essUsableEnergy        the ESS usable energy (= capacity minus
	 *                               reserved energy)
	 * @param essInitialEnergy       the initial ESS available energy
	 * @param essMaxDischargePower   the ESS max discharge power
	 * @param essMaxChargePower      the ESS max discharge power
	 * @param prices                 the buy-from-grid prices
	 * @param consumptionPrediciton  the consumption prediction
	 * @param productionPrediction   the production prediction
	 * @param maxChargePowerFromGrid the max allowed buy from grid power
	 * @return a {@link Schedule}
	 */
	public static Schedule createSchedule(ControlMode controlMode, RiskLevel riskLevel, //
			int essUsableEnergy, int essInitialEnergy, int essMaxDischargePower, int essMaxChargePower, //
			Float[] prices, //
			Integer[] consumptionPrediciton, Integer[] productionPrediction, int maxChargePowerFromGrid) {
		var schedule = new Schedule(controlMode, riskLevel, //
				essUsableEnergy, essInitialEnergy, //
				essMaxDischargePower / TimeOfUseTariffController.PERIODS_PER_HOUR, // power to energy
				essMaxChargePower / TimeOfUseTariffController.PERIODS_PER_HOUR, // power to energy
				prices, consumptionPrediciton, productionPrediction, //
				maxChargePowerFromGrid / TimeOfUseTariffController.PERIODS_PER_HOUR // power to energy
		);

		// Fill the initial schedule based on balancing
		schedule.simulateSchedule();

		// Optimize Schedule
		for (var period : schedule.periods) {
			if (period.requiredEnergy >= period.essInitialEnergy) {
				// recalculate the required energy needed.
				var requiredEnergyFromGrid = period.requiredEnergy - period.essInitialEnergy;
				if (requiredEnergyFromGrid > 0) {
					schedule.calculateSchedule(requiredEnergyFromGrid, period);
				}
			}
			// Simulates and updates the schedule.
			schedule.simulateSchedule();
		}
		return schedule;
	}

	private Schedule(ControlMode controlMode, RiskLevel riskLevel, int essUsableEnergy, int essInitialEnergy,
			int essMaxDischargeEnergyPerPeriod, int essMaxChargeEnergyPerPeriod, Float[] rawPrices,
			Integer[] rawConsumptionPrediciton, Integer[] rawProductionPrediction, int maxAllowedChargeEnergyFromGrid) {
		this.controlMode = controlMode;
		this.essUsableEnergy = max(0, essUsableEnergy);
		this.essInitialEnergy = max(0, essInitialEnergy);
		this.essMaxDischargeEnergyPerPeriod = essMaxDischargeEnergyPerPeriod;
		this.essMaxChargeEnergyPerPeriod = essMaxChargeEnergyPerPeriod;
		this.maxAllowedChargeEnergyFromGrid = maxAllowedChargeEnergyFromGrid;
		var prices = Utils.interpolateArray(rawPrices);
		var consumptionPrediciton = Utils.interpolateArray(rawConsumptionPrediciton);
		var productionPrediction = Utils.interpolateArray(rawProductionPrediction);

		for (var index = 0; index < prices.length; index++) {
			var consumption = consumptionPrediciton[index] / TimeOfUseTariffController.PERIODS_PER_HOUR;
			var production = productionPrediction[index] / TimeOfUseTariffController.PERIODS_PER_HOUR;
			var price = prices[index];

			// Creating the period.
			// Initially the 'chargeDischarge','grid' and 'initialEnergy' will be 'null'.
			var period = new Period(index, null, null, null, price, production, consumption,
					this.essMaxDischargeEnergyPerPeriod);

			// Adding to the list.
			this.periods.add(period);
		}
	}

	/**
	 * Simulates and updates the schedule.
	 * 
	 * <p>
	 * Update in charge or discharge energy in a certain period changes the
	 * subsequent periods.
	 */
	private void simulateSchedule() {
		var essInitialEnergy = this.essInitialEnergy;

		for (var period : this.periods) {
			// Update the initial energy value
			period.essInitialEnergy = essInitialEnergy;

			// updates 'chargeDischarge energy'.
			this.simulatePeriod(period);

			// Calculate initial energy for next period.
			essInitialEnergy = max(0, essInitialEnergy - period.chargeDischargeEnergy);
		}
	}

	/**
	 * Updates the 'chargeDischarge energy' for given {@link Period}.
	 * 
	 * @param p the {@link Period}.
	 */
	protected void simulatePeriod(Period p) {
		// Calculate max possible charge/discharge energy
		// - essMaxChargeEnergy is positive
		// - essMaxDischargeEnergy is negative
		final var essMaxChargeEnergy = max(//
				this.essMaxChargeEnergyPerPeriod, //
				p.essInitialEnergy - this.essUsableEnergy);
		final var essMaxDischargeEnergy = min(this.essMaxDischargeEnergyPerPeriod, p.essInitialEnergy);

		// Fallback: simulate 'Balancing'.
		final var fallbackToBalancing = p.requiredEnergy < 0
				// Prod > Cons -> get ESS charge energy
				? max(essMaxChargeEnergy, p.requiredEnergy)
				// Cons > Prod -> get ESS discharge energy
				: min(essMaxDischargeEnergy, p.requiredEnergy);

		final int setChargeDischargeEnergy;
		if (p.gridEnergy() == null || p.chargeDischargeEnergy == null) {
			// Initial run
			setChargeDischargeEnergy = fallbackToBalancing;

		} else {
			setChargeDischargeEnergy = switch (this.controlMode) {
			case CHARGE_CONSUMPTION -> p.isChargeFromGridScheduled() //
					? max(essMaxChargeEnergy, p.chargeDischargeEnergy) //
					: fallbackToBalancing;

			case DELAY_DISCHARGE -> p.isDelayDischargeScheduled() //
					? min(essMaxDischargeEnergy, p.requiredEnergy - p.gridEnergy()) //
					: fallbackToBalancing;
			};
		}

		// updating 'chargeDischargeEnergy'.
		p.chargeDischargeEnergy = setChargeDischargeEnergy;
	}

	/**
	 * Calculates and schedules the required energy before the target period.
	 * 
	 * @param requiredEnergy the energy needed to be scheduled in battery.
	 * @param targetPeriod   the target (expensive) period.
	 */
	private void calculateSchedule(int requiredEnergy, Period targetPeriod) {
		// Cheapest period with available charge energy.
		var cheapPeriod = this.getCheapestPeriodBeforePeriod(targetPeriod);

		if (cheapPeriod == null) {
			// no cheap hour found
			return;
		}

		if (cheapPeriod.price < targetPeriod.price) {
			// available 'charge' energy when the mode is CHARGE_CONSUMPTION.
			// available 'discharge' energy when the mode is DELAY_DISCAHRGE.
			var availableEnergy = this.getAvailableEnergy(cheapPeriod);

			// check if the required energy to charge/discharge is more than available
			// energy.
			if (requiredEnergy > availableEnergy) {
				// update schedule
				this.updateSchedule(availableEnergy, cheapPeriod);

				// Calculate to schedule remaining energy.
				var remainingEnergy = requiredEnergy - availableEnergy;
				this.calculateSchedule(remainingEnergy, targetPeriod);

			} else {
				// update schedule
				this.updateSchedule(requiredEnergy, cheapPeriod);
			}
		}
	}

	/**
	 * Returns the cheapest period with available charge energy before the specified
	 * period index. Returns null if no cheaper period is found.
	 * 
	 * @param targetPeriod the period before which the cheap hour should be found.
	 * @return the cheapest period, or null if none is found.
	 */
	private Period getCheapestPeriodBeforePeriod(Period targetPeriod) {
		Period result = null;
		for (int thisIndex = 0; thisIndex < targetPeriod.index; thisIndex++) {
			var thisPeriod = this.periods.get(thisIndex);
			var availableEnergy = this.getAvailableEnergy(thisPeriod);
			if (availableEnergy <= 0) {
				continue;
			}

			// Checks if the battery gets full after the cheap period and before the current
			// period.
			if (this.isBatteryCapacityGetsFull(thisIndex, targetPeriod.index, this.periods)) {
				continue;
			}

			if (result == null || thisPeriod.price < result.price) {
				result = thisPeriod;
			}
		}
		return result;
	}

	/**
	 * Redirects to the appropriate method based on the control mode selected.
	 * 
	 * <p>
	 * Redirected to {@link Schedule#getAvailableChargeEnergyForPeriod(Period)} when
	 * {@link ControlMode#CHARGE_CONSUMPTION}.
	 * 
	 * <p>
	 * Redirected to {@link Schedule#getAvailableDischargeEnergyForPeriod(Period)}
	 * when {@link ControlMode#DELAY_DISCHARGE}.
	 * 
	 * @param period The {@link Period}.
	 * @return the available charge/Discharge energy.
	 */
	private int getAvailableEnergy(Period period) {
		return switch (this.controlMode) {
		case CHARGE_CONSUMPTION -> this.getAvailableChargeEnergyForPeriod(period);
		case DELAY_DISCHARGE -> this.getAvailableDischargeEnergyForPeriod(period);
		};
	}

	/**
	 * Returns the available charge energy for the period.
	 * 
	 * @param period The {@link Period}.
	 * @return the available charge energy.
	 */
	private int getAvailableChargeEnergyForPeriod(Period period) {
		final var availableCapacity = subtract(this.essUsableEnergy, period.essInitialEnergy);

		// No space for charging. (SoC is already 100.)
		if (availableCapacity == 0) {
			return 0;
		}

		final var maxChargeEnergy = (period.requiredEnergy > 0)
				? -max(0, this.maxAllowedChargeEnergyFromGrid - period.consumptionPrediction)
				: this.essMaxChargeEnergyPerPeriod;

		if (period.chargeDischargeEnergy < 0) {
			// charge is already set for this period.
			return min(availableCapacity, abs(maxChargeEnergy - period.chargeDischargeEnergy));
		} else {
			// No charge set.
			return min(availableCapacity, abs(maxChargeEnergy));
		}

	}

	/**
	 * Returns the available Discharge energy for the period.
	 * 
	 * @param period The {@link Period}.
	 * @return the available discharge energy.
	 */
	private int getAvailableDischargeEnergyForPeriod(Period period) {
		final var initialEnergyForCurrentPeriod = period.essInitialEnergy;
		final var currentChargeDischargeEnergy = period.chargeDischargeEnergy;

		if (initialEnergyForCurrentPeriod == 0 || currentChargeDischargeEnergy < 0) {
			// Empty Battery || Excess PV available..
			return 0;
		} else {
			return min(this.essMaxDischargeEnergyPerPeriod, currentChargeDischargeEnergy);
		}
	}

	/**
	 * Updates the 'chargeDischarge energy' for cheap hour period.
	 * 
	 * @param energy the energy to schedule.
	 * @param period The {@link Period}.
	 */
	private void updateSchedule(int energy, Period period) {
		switch (this.controlMode) {
		case CHARGE_CONSUMPTION -> {
			energy = -energy; // Charge energy is always negative.

			if (period.chargeDischargeEnergy <= 0) {
				// If already charge scheduled for period, add to it.
				period.chargeDischargeEnergy += energy;
			} else {
				// if charge scheduling for first time.
				period.chargeDischargeEnergy = energy;
			}
		}

		case DELAY_DISCHARGE -> {
			period.chargeDischargeEnergy = max(period.chargeDischargeEnergy - energy, 0);
		}
		}
	}

	/**
	 * Checks if the battery gets full within cheap period and the current period
	 * index.
	 * 
	 * @param from    Index of the cheap hour.
	 * @param to      The current index.
	 * @param periods The list of periods.
	 * @return True if the battery is full within the index range, False otherwise.
	 */
	private boolean isBatteryCapacityGetsFull(int from, int to, List<Period> periods) {
		// Exclude the cheap hour index (first index) from the search.
		return periods.subList(from + 1, to).stream() //
				.anyMatch(p -> p.essInitialEnergy == this.essUsableEnergy);
	}

	@Override
	public String toString() {
		var b = new StringBuilder();
		b.append("\n %10s %10s %10s %10s   %10s %10s  %10s %10s\n".formatted("Index.", "Product.", "Consumpt.",
				"price.", "Battery.", "Grid.", "EssEnergy.", "State."));
		this.periods.forEach(period -> {
			b.append("%10d %10d %10d   %10f %10d %10d %10d      %10s\n".formatted(//
					period.index, //
					period.productionPrediction, period.consumptionPrediction, //
					period.price, period.chargeDischargeEnergy, //
					period.gridEnergy(), period.essInitialEnergy, //
					period.getStateMachine(this.controlMode).toString()));
		});
		return b.toString();
	}
}
