package io.openems.edge.evcs.cluster;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

public abstract class AbstractEvcsCluster extends AbstractOpenemsComponent
		implements OpenemsComponent, EventHandler, Evcs {

	private final Logger log = LoggerFactory.getLogger(AbstractEvcsCluster.class);

	// Default value for the hardware limit
	private static final Integer DEFAULT_HARDWARE_LIMIT = 22080;

	// Distribute the Power that is not used in a cycle
	private double totalPowerLeftInACycle = 0; // W
	private LocalDateTime lastPowerLeftDistribution = LocalDateTime.now();
	private static final int POWER_LEFT_DISTRIBUTION_MIN_TIME = 30; // sec
	private int preferredEvcsCounter = 0;

	/**
	 * Sorted list of the EVCSs in the cluster. (Sorted by prioritisation)
	 * 
	 * @return Sorted EVCS list
	 */
	public abstract List<Evcs> getSortedEvcss();

	/**
	 * Calculate the maximum power to distribute, like excess power or excess power
	 * + storage.
	 * 
	 * @return maximum Power in Watt
	 */
	public abstract int getMaximumPowerToDistribute();

	/**
	 * Minimum charge power, so if it possible every EV will be able to charge with
	 * that minimum.
	 * 
	 * @return minimum guarantee in Watt
	 */
	public abstract int getMinimumChargePowerGuarantee();

	public AbstractEvcsCluster(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);
	}

	/**
	 * Call it in the Implementations.
	 */
	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateChannelValues();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.limitEvcss();
			break;
		default:
			break;
		}
	}

	/**
	 * Calculates the sum of all EVCS charging powers and set it as channel values
	 * of this clustered EVCS.
	 */
	private void calculateChannelValues() {
		final CalculateIntegerSum chargePower = new CalculateIntegerSum();
		final CalculateIntegerSum minHardwarePower = new CalculateIntegerSum();
		final CalculateIntegerSum maxHardwarePowerOfAll = new CalculateIntegerSum();
		final CalculateIntegerSum minPower = new CalculateIntegerSum();

		for (Evcs evcs : this.getSortedEvcss()) {
			chargePower.addValue(evcs.getChargePower());
			minHardwarePower.addValue(evcs.getMinimumHardwarePower());
			maxHardwarePowerOfAll.addValue(evcs.getMaximumHardwarePower());
			minPower.addValue(evcs.getMinimumPower());
		}
		this.getChargePower().setNextValue(chargePower.calculate());
		this.getMinimumHardwarePower().setNextValue(minHardwarePower.calculate());
		Integer maximalUsedHardwarePower = maxHardwarePowerOfAll.calculate();
		if (maximalUsedHardwarePower == null) {
			maximalUsedHardwarePower = this.getMaximumPowerToDistribute();
		}
		this.getMaximumHardwarePower().setNextValue(maximalUsedHardwarePower);
		this.getMinimumPower().setNextValue(minPower.calculate());
	}

	/**
	 * Depending on the excess power, the EVCSs will be charged.
	 */
	private void limitEvcss() {
		try {
			int totalPowerLimit = this.getMaximumPowerToDistribute();

			/*
			 * If a maximum power is present, e.g. from another cluster, then the limit will
			 * be that value or lower.
			 */
			if (this.getMaximumPower().getNextValue().isDefined()) {
				if (totalPowerLimit > this.getMaximumPower().getNextValue().get()) {
					totalPowerLimit = this.getMaximumPower().getNextValue().get();
				}
			}

			this.logInfo(this.log, "Maximum Total Power of the whole system: " + totalPowerLimit);

			// Total Power that can be distributed to EVCSs minus the guaranteed power.
			int totalPowerLeftMinusGuarantee = totalPowerLimit;

			/*
			 * Defines the active charging stations that are charging.
			 */
			List<ManagedEvcs> activeEvcss = new ArrayList<>();
			for (Evcs evcs : this.getSortedEvcss()) {
				if (evcs instanceof ManagedEvcs) {
					ManagedEvcs managedEvcs = (ManagedEvcs) evcs;
					int requestedPower = managedEvcs.setChargePowerRequest().getNextWriteValue().orElse(0);
					Status status = evcs.status().value().asEnum();
					switch (status) {
					case CHARGING_FINISHED:
						if (requestedPower > 0) {
							managedEvcs.setChargePowerLimit().setNextWriteValue(requestedPower);
						}
						break;
					case ERROR:
					case STARTING:
					case UNDEFINED:
					case NOT_READY_FOR_CHARGING:
					case ENERGY_LIMIT_REACHED:
						break;

					// EVCS is active.
					case CHARGING_REJECTED:
					case READY_FOR_CHARGING:
					case CHARGING:

						activeEvcss.add(managedEvcs);

						/*
						 * Reduces the available power by the guaranteed power of each charging station.
						 * Sets the minimum power depending on the guarantee and the maximum Power.
						 */
						if (requestedPower > 0) {
							int evcsMaxPower = evcs.getMaximumPower().value().orElse(DEFAULT_HARDWARE_LIMIT);
							int minGurarantee = this.getMinimumChargePowerGuarantee();
							int guarantee = evcsMaxPower > minGurarantee ? minGurarantee : evcsMaxPower;

							totalPowerLeftMinusGuarantee -= guarantee;
							evcs.getMinimumPower().setNextValue(guarantee);
						}
					}
				}
			}

			// Sets the preferred EVCS to the first one when all have gone through.
			preferredEvcsCounter = (activeEvcss.size() - 1) < preferredEvcsCounter ? 0 : preferredEvcsCounter;

			/*
			 * Distributes the available Power to the active EVCSs
			 */
			for (int index = 0; index < activeEvcss.size(); index++) {
				ManagedEvcs evcs = activeEvcss.get(index);

				int guarantee = evcs.getMinimumPower().getNextValue().orElse(0);

				// Power left for the single EVCS including their guarantee
				final int powerLeft = totalPowerLeftMinusGuarantee + guarantee;

				int nextChargePower;
				Optional<Integer> requestedPower = evcs.setChargePowerRequest().getNextWriteValue();

				// Power requested by the controller
				if (requestedPower.isPresent()) {
					this.logInfo(this.log, "Requested Power ( for " + evcs.alias() + "): " + requestedPower.get());
					nextChargePower = requestedPower.get();
				} else {
					nextChargePower = evcs.getMaximumHardwarePower().value().orElse(DEFAULT_HARDWARE_LIMIT);
				}

				// It should not be charged more than possible for the current EV
				int maxPower = evcs.getMaximumPower().value().orElse(DEFAULT_HARDWARE_LIMIT);
				nextChargePower = nextChargePower > maxPower ? maxPower : nextChargePower;

				// Add extra power(Power not used in the cycle before) to the the EVCS
				int extraPower = calculateExtraPowerIfPrefered(index, evcs, activeEvcss.size());
				nextChargePower = nextChargePower + extraPower;

				// Checks if there is enough power left and sets the charge power
				if (nextChargePower < powerLeft) {
					evcs.setChargePowerLimit().setNextWriteValue(nextChargePower);
					totalPowerLeftMinusGuarantee = totalPowerLeftMinusGuarantee - (nextChargePower - guarantee);
					this.logInfo(this.log,
							"Power Left: " + totalPowerLeftMinusGuarantee + " ; Charge power: " + nextChargePower);
				} else {
					evcs.setChargePowerLimit().setNextWriteValue(powerLeft);
					totalPowerLeftMinusGuarantee = 0;
					this.logInfo(this.log, "Power Left: " + powerLeft + " ; Charge power: " + powerLeft);
				}
			}

			// Set another preferred EVCS
			if (this.lastPowerLeftDistribution.plusSeconds(POWER_LEFT_DISTRIBUTION_MIN_TIME / 3)
					.isBefore(LocalDateTime.now())) {
				this.preferredEvcsCounter++;
			}
			// Set the power that is left in this cycle
			if (this.lastPowerLeftDistribution.plusSeconds(POWER_LEFT_DISTRIBUTION_MIN_TIME)
					.isBefore(LocalDateTime.now())) {
				this.totalPowerLeftInACycle = totalPowerLeftMinusGuarantee;
				this.lastPowerLeftDistribution = LocalDateTime.now();
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculate extra power by the unused power in the cycle before.
	 * 
	 * @param index           index of the EVCSs
	 * @param evcs            Charging station
	 * @param activeEvcssSize Count of all active EVCSs
	 * @return calculated extra power if that EVCS is preferred
	 */
	private int calculateExtraPowerIfPrefered(int index, ManagedEvcs evcs, int activeEvcssSize) {
		int extraPower = 0;
		int leftToMaxPower = evcs.getMaximumHardwarePower().value().orElse(22800)
				- evcs.getChargePower().value().orElse(0);

		extraPower = (int) (this.totalPowerLeftInACycle < leftToMaxPower ? this.totalPowerLeftInACycle
				: leftToMaxPower);
		this.logInfo(this.log, "Extra Power calculated:  ( for " + evcs.alias() + "): " + extraPower);
		if (activeEvcssSize <= 1 || index == preferredEvcsCounter) {
			return extraPower > 0 ? extraPower : 0;
		}
		return 0;
	}

}
