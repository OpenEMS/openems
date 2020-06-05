package io.openems.edge.evcs.cluster;

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

	public AbstractEvcsCluster(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);
	}

	/**
	 * Call it in the implementations.
	 */
	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateChannelValues();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			this.limitEvcss();
			break;
		default:
			break;
		}
	}

	/**
	 * Calculates the sum of all EVCS charging power values and set it as channel
	 * values of this clustered EVCS.
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
	 * Depending on the excess power, the EVCSs will be charged. Distributing the
	 * maximum allowed charge distribution power (given by the implementation) to
	 * each evcs.
	 */
	protected void limitEvcss() {
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

			this.logInfoInDebugmode(this.log, "Maximum total power to distribute: " + totalPowerLimit);

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
					Status status = evcs.getStatus().value().asEnum();
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
						 * Sets the minimum power depending on the guaranteed and the maximum Power.
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

			/*
			 * Distributes the available Power to the active EVCSs
			 */
			for (ManagedEvcs evcs : activeEvcss) {

				int guaranteedPower = evcs.getMinimumPower().getNextValue().orElse(0);

				// Power left for the this EVCS including its guaranteed power
				final int powerLeft = totalPowerLeftMinusGuarantee + guaranteedPower;

				int nextChargePower;
				Optional<Integer> requestedPower = evcs.setChargePowerRequest().getNextWriteValue();

				// Power requested by the controller
				if (requestedPower.isPresent()) {
					this.logInfoInDebugmode(this.log,
							"Requested power ( for " + evcs.alias() + "): " + requestedPower.get());
					nextChargePower = requestedPower.get();
				} else {
					nextChargePower = evcs.getMaximumHardwarePower().value().orElse(DEFAULT_HARDWARE_LIMIT);
				}

				// It should not be charged more than possible for the current EV
				int maxPower = evcs.getMaximumPower().value().orElse(DEFAULT_HARDWARE_LIMIT);
				nextChargePower = nextChargePower > maxPower ? maxPower : nextChargePower;

				// Checks if there is enough power left and sets the charge power
				if (nextChargePower < powerLeft) {
					evcs.setChargePowerLimit().setNextWriteValue(nextChargePower);
					totalPowerLeftMinusGuarantee = totalPowerLeftMinusGuarantee - (nextChargePower - guaranteedPower);
					this.logInfoInDebugmode(this.log,
							"Charge power: " + nextChargePower + "; Power left: " + totalPowerLeftMinusGuarantee);
				} else {
					evcs.setChargePowerLimit().setNextWriteValue(powerLeft);
					totalPowerLeftMinusGuarantee = 0;
					this.logInfoInDebugmode(this.log,
							"Power Left: " + totalPowerLeftMinusGuarantee + " ; Charge power: " + powerLeft);
				}
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sorted list of the EVCSs in the cluster.
	 * 
	 * <p>
	 * List of EVCSs that should be considered in the cluster sorted by
	 * prioritisation.
	 * 
	 * @return Sorted EVCS list
	 */
	public abstract List<Evcs> getSortedEvcss();

	/**
	 * Maximum power to distribute.
	 * 
	 * <p>
	 * Calculate the maximum power to distribute, like excess power or excess power
	 * + storage.
	 * 
	 * @return Maximum Power in Watt
	 */
	public abstract int getMaximumPowerToDistribute();

	/**
	 * Guaranteed minimum charge power.
	 * 
	 * <p>
	 * Minimum charge power that will be used by every EV that is able to charge
	 * with that minimum.
	 * 
	 * @return Minimum guaranteed power in Watt
	 */
	public abstract int getMinimumChargePowerGuarantee();

	/**
	 * Debug mode.
	 * 
	 * <p>
	 * Logging a few important situations if this returns true. This value should be
	 * given by the configuration by runtime.
	 * 
	 * @return Debug mode or not
	 */
	public abstract boolean isDebugMode();

	protected void logInfoInDebugmode(Logger log, String string) {
		if (this.isDebugMode()) {
			this.logInfo(log, string);
		}
	}
}
