package io.openems.edge.evcs.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MAXIMUM_POWER_TO_DISTRIBUTE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Maximum power to distribute, for all given Evcss"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

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
		if (!this.isEnabled()) {
			return;
		}
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
			chargePower.addValue(evcs.getChargePowerChannel());
			minHardwarePower.addValue(evcs.getMinimumHardwarePowerChannel());
			maxHardwarePowerOfAll.addValue(evcs.getMaximumHardwarePowerChannel());
			minPower.addValue(evcs.getMinimumPowerChannel());
		}

		this._setChargePower(chargePower.calculate());
		this._setMinimumHardwarePower(minHardwarePower.calculate());
		Integer maximalUsedHardwarePower = maxHardwarePowerOfAll.calculate();
		if (maximalUsedHardwarePower == null) {
			maximalUsedHardwarePower = this.getMaximumPowerToDistribute();
		}
		this._setMaximumHardwarePower(maximalUsedHardwarePower);
		this._setMinimumPower(minPower.calculate());
	}

	/**
	 * Depending on the excess power, the EVCSs will be charged. Distributing the
	 * maximum allowed charge distribution power (given by the implementation) to
	 * each evcs.
	 */
	protected void limitEvcss() {
		try {
			int totalPowerLimit = this.getMaximumPowerToDistribute();
			this.channel(ChannelId.MAXIMUM_POWER_TO_DISTRIBUTE).setNextValue(totalPowerLimit);

			/*
			 * If a maximum power is present, e.g. from another cluster, then the limit will
			 * be that value or lower.
			 */
			if (this.getMaximumPower().isDefined()) {
				if (totalPowerLimit > this.getMaximumPower().get()) {
					totalPowerLimit = this.getMaximumPower().get();
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
					int requestedPower = managedEvcs.getSetChargePowerRequestChannel().getNextWriteValue().orElse(0);

					if (requestedPower <= 0) {
						managedEvcs.setChargePowerLimit(0);
						continue;
					}

					int guaranteedPower = getGuaranteedPower(managedEvcs);
					Status status = managedEvcs.getStatus();
					switch (status) {
					case CHARGING_FINISHED:
						managedEvcs.setChargePowerLimit(requestedPower);
						break;
					case ERROR:
					case STARTING:
					case UNDEFINED:
					case NOT_READY_FOR_CHARGING:
					case ENERGY_LIMIT_REACHED:
						break;
					case READY_FOR_CHARGING:

						// Check if there is enough power for an initial charge
						if (totalPowerLimit - this.getChargePower().orElse(0) >= guaranteedPower) {
							managedEvcs.setChargePowerLimit(guaranteedPower);
							// TODO: managedEvcs._setStatus(Status.UNCONFIRMED_CHARGING); or put this in the
							// setChargePowerLimit
							totalPowerLeftMinusGuarantee -= guaranteedPower;
						}
						break;

					// EVCS is active.
					case CHARGING_REJECTED:
					case CHARGING:
						/*
						 * Reduces the available power by the guaranteed power of each charging station.
						 * Sets the minimum power depending on the guaranteed and the maximum Power.
						 */
						if (totalPowerLeftMinusGuarantee - guaranteedPower >= 0) {
							totalPowerLeftMinusGuarantee -= guaranteedPower;
							managedEvcs._setMinimumPower(guaranteedPower);
							activeEvcss.add(managedEvcs);
						} else {
							managedEvcs.setChargePowerLimit(0);
						}
					}
				}
			}

			/*
			 * Distributes the available Power to the active EVCSs
			 */
			for (ManagedEvcs evcs : activeEvcss) {
				
				//int guaranteedPower = evcs.getMinimumPowerChannel().getNextValue().orElse(0); 
				int guaranteedPower = evcs.getMinimumPowerChannel().getNextValue().orElse(0);

				// Power left for the this EVCS including its guaranteed power
				final int powerLeft = totalPowerLeftMinusGuarantee + guaranteedPower;

				int nextChargePower;
				Optional<Integer> requestedPower = evcs.getSetChargePowerRequestChannel().getNextWriteValue();

				// Power requested by the controller
				if (requestedPower.isPresent()) {
					this.logInfoInDebugmode(this.log,
							"Requested power ( for " + evcs.alias() + "): " + requestedPower.get());
					nextChargePower = requestedPower.get();
				} else {
					nextChargePower = evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT);
				}

				// It should not be charged more than possible for the current EV
				int maxPower = evcs.getMaximumPower()
						.orElse(evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT));
				nextChargePower = nextChargePower > maxPower ? maxPower : nextChargePower;

				// Checks if there is enough power left and sets the charge power
				if (nextChargePower < powerLeft) {
					evcs.setChargePowerLimit(nextChargePower);
					totalPowerLeftMinusGuarantee = totalPowerLeftMinusGuarantee - (nextChargePower - guaranteedPower);
					this.logInfoInDebugmode(this.log,
							"Charge power: " + nextChargePower + "; Power left: " + totalPowerLeftMinusGuarantee);
				} else {
					evcs.setChargePowerLimit(powerLeft);
					totalPowerLeftMinusGuarantee = 0;
					this.logInfoInDebugmode(this.log,
							"Power Left: " + totalPowerLeftMinusGuarantee + " ; Charge power: " + powerLeft);
				}
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	private int getGuaranteedPower(Evcs evcs) {
		int minGuarantee = this.getMinimumChargePowerGuarantee();
		int evcsMaxPower = evcs.getMaximumPower().orElse(evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT));
		return evcsMaxPower > minGuarantee ? minGuarantee : evcsMaxPower;
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
