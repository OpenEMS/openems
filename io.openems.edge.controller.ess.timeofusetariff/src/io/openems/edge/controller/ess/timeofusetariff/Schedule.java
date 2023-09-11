package io.openems.edge.controller.ess.timeofusetariff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.openems.edge.common.type.TypeUtils;

public class Schedule {

	/*
	 * List of periods with charge discharge data to optimize ESS. Total number of
	 * periods are limited to the number of price periods that are existing.
	 */
	protected List<Period> periods = new ArrayList<>();

	/* ESS Usable capacity. */
	private int essUsableEnergy;

	/* Available Energy in the battery based on current SoC. */
	private int currentAvailableEnergy;

	/* Maximum Discharge energy of the battery in a period. */
	private int maxDischargeEnergyPerPeriod;

	/* Maximum Charge energy of the battery in a period. */
	private int maxChargeEnergyPerPeriod;

	/* Maximum Charge energy of the battery from grid in a period. */
	private int maxAllowedChargeEnergyFromGrid;

	/* Control mode defined by user while configuring. */
	private ControlMode controlMode;

	public static class Period {
		protected Integer essInitialEnergy;
		protected Integer chargeDischargeEnergy;
		protected Integer gridEnergy;
		protected float price;
		protected int productionPrediction;
		protected int consumptionPrediction;
		protected int maxDischargeEnergyPerPeriod;
		protected int requiredEnergy;

		public Period(Integer essInitialEnergy, Integer chargeDischargeEnergy, Integer gridEnergy, float price,
				int productionPrediction, int consumptionPrediction, int maxDischargeEnergyPerPeriod) {
			this.essInitialEnergy = essInitialEnergy;
			this.chargeDischargeEnergy = chargeDischargeEnergy;
			this.price = price;
			this.productionPrediction = productionPrediction;
			this.consumptionPrediction = consumptionPrediction;
			this.maxDischargeEnergyPerPeriod = maxDischargeEnergyPerPeriod;
			this.requiredEnergy = this.consumptionPrediction - this.productionPrediction;
		}

		/**
		 * Method to check if force charge is scheduled for the period.
		 * 
		 * @return True if force charge is scheduled and False otherwise.
		 */
		public boolean isChargeFromGridScheduled() {
			return this.chargeDischargeEnergy < 0 && this.gridEnergy > 0;
		}

		/**
		 * Method to check if delay discharge is scheduled.
		 * 
		 * @return True if delaying discharge is scheduled and False otherwise.
		 */
		public boolean isDelayDischargeScheduled() {
			return this.chargeDischargeEnergy < this.essInitialEnergy //
					&& this.gridEnergy > 0 //
					&& this.requiredEnergy < this.essInitialEnergy //
					&& this.chargeDischargeEnergy != this.maxDischargeEnergyPerPeriod;
		}

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

	public Schedule(ControlMode controlMode, RiskLevel riskLevel, int essUsableEnergy, int currentAvailableEnergy, //
			int dischargeEnergy, int chargeEnergy, Float[] prices, Integer[] consumptionPrediciton, //
			Integer[] productionPrediction, int maxAllowedChargeEnergyFromGrid) {

		this.controlMode = controlMode;
		this.essUsableEnergy = essUsableEnergy;
		this.currentAvailableEnergy = currentAvailableEnergy;
		this.maxDischargeEnergyPerPeriod = dischargeEnergy;
		this.maxChargeEnergyPerPeriod = chargeEnergy;
		this.maxAllowedChargeEnergyFromGrid = maxAllowedChargeEnergyFromGrid;

		// Filtering non null values.
		var priceValues = Arrays.stream(prices) //
				.filter(Objects::nonNull) //
				.toArray(Float[]::new);

		for (var index = 0; index < priceValues.length; index++) {
			var consumption = consumptionPrediciton[index];
			var production = productionPrediction[index];
			var price = priceValues[index];

			if (production == null) {
				production = 0;
			}

			if (consumption == null) {
				consumption = 0;
			}

			// TODO
			// Adjust 'production', 'consumption' based on RIskLevel selected by customer.

			// Creating the period.
			// Initially the 'chargeDischarge','grid' and 'initialEnergy' will be 'null'.
			var period = new Period(null, null, null, price, production, consumption, this.maxDischargeEnergyPerPeriod);

			// Adding to the list.
			this.periods.add(period);
		}

		// Creates the initial schedule based on balancing.
		this.simulateSchedule();
	}

	/**
	 * Creates the schedule with periods.
	 */
	public void createSchedule() {
		for (int index = 0; index < this.periods.size(); index++) {
			final var period = this.periods.get(index);
			final var requiredEnergy = period.requiredEnergy;
			final var essInitialEnergy = period.essInitialEnergy;

			if (requiredEnergy >= essInitialEnergy) {
				// recalculate the required energy needed.
				var requiredEnergyFromGrid = requiredEnergy - essInitialEnergy;
				if (requiredEnergyFromGrid > 0) {
					this.calculateSchedule(requiredEnergyFromGrid, index);
				}
			}
			// Simulates and updates the schedule.
			this.simulateSchedule();
		}

		System.out.println(this.toString());
	}

	/**
	 * Simulates and updates the schedule.
	 * 
	 * <p>
	 * Update in charge or discharge energy in a certain period changes the
	 * subsequent periods.
	 * 
	 */
	private void simulateSchedule() {
		var essInitialEnergy = this.currentAvailableEnergy;

		for (int index = 0; index < this.periods.size(); index++) {

			// Current period.
			final var period = this.periods.get(index);

			// calculate initial energy for current period.
			final int essInitialEnergyForCurrentPeriod = TypeUtils.max(0, essInitialEnergy);

			// Update the value.
			period.essInitialEnergy = essInitialEnergyForCurrentPeriod;

			// updates 'chargeDischarge energy' and 'Grid energy' values.
			this.updateEssAndGridEnergyInPeriod(period);

			// estimating initial Energy for next period.
			final int essInitialEnergyForNextPeriod = TypeUtils.max(0, essInitialEnergy - period.chargeDischargeEnergy);

			// Store the value for next period.
			essInitialEnergy = essInitialEnergyForNextPeriod;
		}
	}

	/**
	 * Updates the 'chargeDischarge energy' and 'Grid energy' values for the
	 * appropriate period mentioned by index.
	 * 
	 * @param period The {@link Period}.
	 */
	private void updateEssAndGridEnergyInPeriod(Period period) {
		final var chargeDischargeEnergy = period.chargeDischargeEnergy;
		final var requiredEnergy = period.requiredEnergy;
		final var essInitialEnergy = period.essInitialEnergy;
		final var gridEnergy = period.gridEnergy;

		// Calculate maximum allowed charge and discharge energies
		final var maximumAllowedChargeInBattery = TypeUtils.max(this.maxChargeEnergyPerPeriod,
				(essInitialEnergy - this.essUsableEnergy));
		final var maximumAllowedDischargeInBattery = TypeUtils.min(this.maxDischargeEnergyPerPeriod, essInitialEnergy);

		// simulate 'Balancing'.
		var chargeDischargeEnergyUpdated = period.isExcessPvAvailable()
				// Excess PV energy is present
				? TypeUtils.max(maximumAllowedChargeInBattery, requiredEnergy)
				// Normal Discharging.
				: TypeUtils.min(maximumAllowedDischargeInBattery, requiredEnergy);

		if (gridEnergy != null && chargeDischargeEnergy != null) {
			// Not initial run.
			switch (this.controlMode) {
			case CHARGE_CONSUMPTION:
				if (period.isChargeFromGridScheduled()) {
					// if force charge is scheduled.
					chargeDischargeEnergyUpdated = TypeUtils.max(maximumAllowedChargeInBattery, chargeDischargeEnergy);
				}
				break;
			case DELAY_DISCHARGE:
				if (period.isDelayDischargeScheduled()) {
					// Delay discharge is already set.
					chargeDischargeEnergyUpdated = TypeUtils.min(maximumAllowedDischargeInBattery,
							requiredEnergy - gridEnergy);
				}
				break;
			}
		}

		// update the chargeDischargeEnergy and grid energy values.
		period.chargeDischargeEnergy = chargeDischargeEnergyUpdated;
		period.gridEnergy = requiredEnergy - chargeDischargeEnergyUpdated;
	}

	/**
	 * Calculates and schedules the required energy before the index period.
	 * 
	 * @param requiredEnergy       The energy needed to be scheduled in battery.
	 * @param expensivePeriodIndex The index of the expensive period.
	 */
	private void calculateSchedule(int requiredEnergy, int expensivePeriodIndex) {

		// Cheapest period index with available charge energy.
		var cheapHour = this.getCheapestHourIndexBeforePeriod(expensivePeriodIndex, this.periods, this.essUsableEnergy);

		if (cheapHour == null) {
			// no cheap hour Calculated
			return;
		}

		var cheapPeriod = this.periods.get(cheapHour);
		var expensivePeriod = this.periods.get(expensivePeriodIndex);

		// schedule
		if (cheapPeriod.price < expensivePeriod.price) {

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
				this.calculateSchedule(remainingEnergy, expensivePeriodIndex);
			} else {
				// update schedule
				this.updateSchedule(requiredEnergy, cheapPeriod);
			}
		}
	}

	/**
	 * Returns the cheapest period with available charge energy before the specified
	 * period index. Returns null if cheapest hour is not found.
	 * 
	 * @param expensivePeriodIndex The period index before which the cheap hour
	 *                             should be found.
	 * @param periods              The list of periods.
	 * @param essUsableEnergy      The usable energy in the battery.
	 * @return The index of the cheapest hour, or null if none is found.
	 */
	private Integer getCheapestHourIndexBeforePeriod(int expensivePeriodIndex, List<Period> periods,
			int essUsableEnergy) {

		Integer cheapHourIndex = null;
		for (int index = 0; index < expensivePeriodIndex; index++) {
			var period = this.periods.get(index);
			var availableEnergy = this.getAvailableEnergy(period);
			if (availableEnergy <= 0) {
				continue;
			}

			// Checks if the battery gets full after the cheap period and before the current
			// period.
			var batteryCapacityisFull = this.batteryCapacityGetsFull(index, expensivePeriodIndex, periods,
					essUsableEnergy);
			if (batteryCapacityisFull) {
				continue;
			}

			if (cheapHourIndex == null || periods.get(index).price < periods.get(cheapHourIndex).price) {
				cheapHourIndex = index;
			}
		}

		return cheapHourIndex;
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
		final var availableCapacity = TypeUtils.subtract(this.essUsableEnergy, period.essInitialEnergy);

		// No space for charging. (SoC is already 100.)
		if (availableCapacity == 0) {
			return 0;
		}

		final var maxChargeEnergy = (period.requiredEnergy > 0)
				? -TypeUtils.max(0, this.maxAllowedChargeEnergyFromGrid - period.consumptionPrediction)
				: this.maxChargeEnergyPerPeriod;

		if (period.chargeDischargeEnergy < 0) {
			// charge is already set for this period.
			return TypeUtils.min(availableCapacity, TypeUtils.abs(maxChargeEnergy - period.chargeDischargeEnergy));
		} else {
			// No charge set.
			return TypeUtils.min(availableCapacity, TypeUtils.abs(maxChargeEnergy));
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
			return TypeUtils.min(this.maxDischargeEnergyPerPeriod, currentChargeDischargeEnergy);
		}
	}

	/**
	 * Updates the 'chargeDischarge energy' and 'Grid energy' values for cheap hour
	 * period.
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

			// Update grid energy.
			period.gridEnergy = -period.chargeDischargeEnergy;
		}

		case DELAY_DISCHARGE -> {
			period.chargeDischargeEnergy = TypeUtils.max(period.chargeDischargeEnergy - energy, 0);

			// Update grid energy.
			period.gridEnergy += energy;
		}
		}
	}

	/**
	 * Checks if the battery gets full within cheap period and the current period
	 * index.
	 * 
	 * @param from            Index of the cheap hour.
	 * @param to              The current index.
	 * @param periods         The list of periods.
	 * @param essUsableEnergy The Usable energy in the battery.
	 * @return True if the battery is full within the index range, False otherwise.
	 */
	private boolean batteryCapacityGetsFull(int from, int to, List<Period> periods, int essUsableEnergy) {
		// Exclude the cheap hour index (first index) from the search.
		return periods.subList(from + 1, to).stream() //
				.anyMatch(p -> p.essInitialEnergy == essUsableEnergy);
	}

	@Override
	public String toString() {
		var b = new StringBuilder();
		b.append("%10s %10s %10s %10s %10s %10s %10s \n".formatted("Index.", "Product.", "Consumpt.", "price.",
				"Battery.", "Grid.", "EssEnegery.", "State."));
		final var iterator = this.periods.listIterator();
		while (iterator.hasNext()) {
			final var index = iterator.nextIndex();
			final var period = iterator.next();
			b.append("%10d %10d %10d %10f %10d %10d %10d %10s\n".formatted(index, //
					period.productionPrediction, period.consumptionPrediction, //
					period.price, period.chargeDischargeEnergy, //
					period.gridEnergy, period.essInitialEnergy, //
					period.getStateMachine(this.controlMode).toString()));
		}
		return b.toString();
	}
}
