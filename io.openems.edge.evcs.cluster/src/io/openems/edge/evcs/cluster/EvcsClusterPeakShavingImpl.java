package io.openems.edge.evcs.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Cluster.PeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
})
public class EvcsClusterPeakShavingImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, Evcs, EventHandler, EvcsClusterPeakShaving,
		/*
		 * Cluster is not a Controller, but we need to be placed at the correct position
		 * in the Cycle by the Scheduler to be able to read the actually available ESS
		 * power
		 */
		Controller {

	/**
	 * Guaranteed minimum charge power.
	 *
	 * <p>
	 * Minimum charge power that will be used by every EV that is able to charge
	 * with that minimum.
	 *
	 * @return Minimum guaranteed power in Watt
	 */
	private static final int MINIMUM_CHARGE_POWER_GUARANTEE = 4500;

	private final Logger log = LoggerFactory.getLogger(EvcsClusterPeakShavingImpl.class);

	// Used EVCSs
	private String[] evcsIds = {};
	private final List<Evcs> sortedEvcss = new ArrayList<>();
	private final Map<String, Evcs> evcss = new ConcurrentHashMap<>();

	private Config config;

	// Total status calculated from the individual EVCSs
	private EvcsClusterStatus currentEvcsClusterState;

	// Last used limit to reduce this limit by unavailable grid power as a fallback.
	private Integer lastLimit = null;

	// The maximum discharge power of the energy storage system
	private int maxEssDischargePower = 0;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference
	private SymmetricEss ess;

	@Reference
	private SymmetricMeter meter;

	public EvcsClusterPeakShavingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsClusterPeakShaving.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this.setClusteredState(evcs);
		this.evcss.put(evcs.id(), evcs);
		this.updateSortedEvcss();
	}

	protected void removeEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this.resetClusteredState(evcs);
		this.evcss.remove(evcs.id());
		this.updateSortedEvcss();
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.evcsIds = config.evcs_ids();
		this.updateSortedEvcss();
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;

		// update filter for 'evcs' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Evcs", config.evcs_ids())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		for (Evcs evcs : this.sortedEvcss) {
			this.resetClusteredState(evcs);
			evcs._setMaximumPower(null);
		}
		super.deactivate();
	}

	/**
	 * Fills sortedEvcss using the order of evcs_ids property in the configuration.
	 */
	private synchronized void updateSortedEvcss() {
		this.sortedEvcss.clear();
		for (String id : this.evcsIds) {
			var evcs = this.evcss.get(id);
			if (evcs == null) {
				this.logWarn(this.log, "Required Evcs [" + id + "] is not available.");
			} else {
				this.sortedEvcss.add(evcs);
			}
		}
	}

	/**
	 * Sets the cluster channel to false and resets all depending channels.
	 *
	 * @param evcs Electric Vehicle Charging Station
	 */
	private void resetClusteredState(Evcs evcs) {
		if (evcs instanceof ManagedEvcs) {
			((ManagedEvcs) evcs)._setIsClustered(false);
			((ManagedEvcs) evcs)._setSetChargePowerRequest(null);
		}
		evcs._setMaximumPower(null);
	}

	/**
	 * Sets the cluster channel to true.
	 *
	 * @param evcs Electric Vehicle Charging Station
	 */
	private void setClusteredState(Evcs evcs) {
		if (evcs instanceof ManagedEvcs) {
			((ManagedEvcs) evcs)._setIsClustered(true);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			this.calculateChannelValues();
			this.limitEvcss();
			break;
		default:
			break;
		}
	}

	/**
	 * Calculates the sum of all EVCS values and set it to this EVCS Cluster.
	 */
	private void calculateChannelValues() {
		this.currentEvcsClusterState = EvcsClusterStatus.REGULAR;
		final var chargePower = new CalculateIntegerSum();
		final var blockedChargePower = new CalculateIntegerSum();
		final var minHardwarePower = new CalculateIntegerSum();
		final var maxHardwarePowerOfAll = new CalculateIntegerSum();
		final var minFixedHardwarePower = new CalculateIntegerSum();
		final var maxFixedHardwarePower = new CalculateIntegerSum();
		final var minPower = new CalculateIntegerSum();
		final var evcsClusterStatus = new CalculateEvcsClusterStatus();

		for (Evcs evcs : this.getSortedEvcss()) {
			chargePower.addValue(evcs.getChargePowerChannel());
			blockedChargePower.addValue(evcs.getChargePowerChannel(), value -> {

				// Calculate the blocked power using all 3 phases for now
				if (value != null) {
					switch (evcs.getPhases()) {
					case ONE_PHASE:
						return value * Phases.THREE_PHASE.getValue();
					case TWO_PHASE:
						return Math.round(value / Phases.TWO_PHASE.getValue() * Phases.THREE_PHASE.getValue());
					case THREE_PHASE:
						return value;
					}
				}
				return null;
			});
			minHardwarePower.addValue(evcs.getMinimumHardwarePowerChannel());
			maxHardwarePowerOfAll.addValue(evcs.getMaximumHardwarePowerChannel());
			minFixedHardwarePower.addValue(evcs.getFixedMinimumHardwarePowerChannel());
			maxFixedHardwarePower.addValue(evcs.getFixedMaximumHardwarePowerChannel());
			minPower.addValue(evcs.getMinimumPowerChannel());
			if (evcs instanceof ManagedEvcs) {
				evcsClusterStatus.addValue(((ManagedEvcs) evcs).getChargeState().asEnum());
			}
		}

		this._setChargePower(chargePower.calculate());
		this._setEvcsBlockedChargePower(blockedChargePower.calculate());
		this._setFixedMinimumHardwarePower(minFixedHardwarePower.calculate());
		this._setFixedMaximumHardwarePower(maxFixedHardwarePower.calculate());
		this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER).setNextValue(minHardwarePower.calculate());
		var maximalUsedHardwarePower = maxHardwarePowerOfAll.calculate();
		if (maximalUsedHardwarePower == null) {
			maximalUsedHardwarePower = this.getMaximumPowerToDistribute();
		}
		this.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER).setNextValue(maximalUsedHardwarePower);
		this._setMinimumPower(minPower.calculate());
		this.currentEvcsClusterState = evcsClusterStatus.calculate();
		this._setEvcsClusterStatus(this.currentEvcsClusterState);
	}

	/**
	 * Depending on the excess power, the EVCSs will be charged. Distributing the
	 * maximum allowed charge distribution power (given by the implementation) to
	 * each evcs.
	 */
	protected void limitEvcss() {

		// Wait at least the EVCS-specific response time, required to increase and
		// decrease the charging power
		if (awaitLastChanges(this.currentEvcsClusterState, this.getAvailableGridPower())) {
			// Still waiting for increasing, decreasing the power or undefined
			return;
		}

		var totalPowerLimit = this.getMaximumPowerToDistribute();

		/*
		 * Make sure that only the allowed power at the grid meter is used.
		 *
		 * TODO:
		 * 
		 * Could be shifted to EvcsClusterPeakshaving and needs a more intelligent way
		 * of reducing the last limit. e.g. States, representing the current "risk"
		 * level with a defined buffer.
		 * 
		 * The lastLimit should not reduce every cycle (EVCSs cannot react that fast)
		 * unless the available Power is reducing further.
		 */
		int unavailablePower = this.getAvailableGridPower() * -1;
		if (unavailablePower > 0 && this.lastLimit != null) {
			this.logInfoInDebugmode("Reducing last limit by " + unavailablePower + " W");
			totalPowerLimit = this.lastLimit.intValue() - unavailablePower;
		}

		this.lastLimit = Integer.valueOf(totalPowerLimit);
		this.channel(EvcsClusterPeakShaving.ChannelId.MAXIMUM_POWER_TO_DISTRIBUTE)
				.setNextValue(Integer.valueOf(totalPowerLimit));

		// Minimum of the current limit and the maximum power if present, e.g. from
		// another cluster
		totalPowerLimit = Math.min(totalPowerLimit, this.getMaximumPower().orElse(Integer.MAX_VALUE));

		this.logInfoInDebugmode("Maximum total power to distribute: " + totalPowerLimit);

		// Total Power that can be distributed to EVCSs minus the guaranteed power.
		var totalPowerLeftMinusGuarantee = totalPowerLimit;

		// ChargePower that is used for the initial charge in this cycle
		int initialChargePower = 0;

		try {

			/*
			 * Defines the active charging stations that are charging.
			 */
			List<ManagedEvcs> activeEvcss = new ArrayList<>();
			for (Evcs evcs : this.getSortedEvcss()) {
				if (evcs instanceof ManagedEvcs) {
					var managedEvcs = (ManagedEvcs) evcs;
					int requestedPower = managedEvcs.getSetChargePowerRequestChannel().getNextWriteValue().orElse(0);

					// Ignore evcs with no request
					if (requestedPower <= 0) {
						managedEvcs.setChargePowerLimit(0);
						continue;
					}

					var guaranteedPower = this.getGuaranteedPower(managedEvcs);
					var status = managedEvcs.getStatus();
					switch (status) {
					case CHARGING_FINISHED:
						managedEvcs.setChargePowerLimitWithFilter(requestedPower);
						/*
						 * TODO: Change state from CHARGING_FINISHED to CHARGING should increase the
						 * minimumPower. e.g. ZOE that is nearly on 100 percent, does not charge with a
						 * set point of 13kW, but 22kW by charging with less power.
						 */
						break;
					case ERROR:
					case STARTING:
					case UNDEFINED:
					case NOT_READY_FOR_CHARGING:
					case ENERGY_LIMIT_REACHED:
						managedEvcs.setChargePowerLimit(0);
						break;
					case READY_FOR_CHARGING:

						// Check if there is enough power for an initial charge
						// if (totalPowerLimit - this.getChargePower().orElse(0) >= guaranteedPower) {
						if (totalPowerLimit - initialChargePower - this.getChargePower().orElse(0) >= guaranteedPower) {

							this.logInfoInDebugmode("Set initial power " + guaranteedPower + " to " + evcs.id());
							managedEvcs.setChargePowerLimit(guaranteedPower);
							initialChargePower += guaranteedPower;

							// TODO: managedEvcs._setStatus(Status.UNCONFIRMED_CHARGING); or put this in the
							// setChargePowerLimit
						} else {
							managedEvcs.getChargeStateHandler()
									.applyNewChargeState(ChargeState.WAITING_FOR_AVAILABLE_POWER);
							managedEvcs.setChargePowerLimit(0);
						}

						// Reduce the total power by the initial power to be able to send an initial
						// charge request in the next cycles
						totalPowerLeftMinusGuarantee -= guaranteedPower;
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
						break;
					}
				}
			}

			/*
			 * Distributes the available Power to the active EVCSs
			 */
			for (ManagedEvcs evcs : activeEvcss) {

				// int guaranteedPower = evcs.getMinimumPowerChannel().getNextValue().orElse(0);
				int guaranteedPower = evcs.getMinimumPowerChannel().getNextValue().orElse(0);

				// Power left for the this EVCS including its guaranteed power
				final var powerLeft = totalPowerLeftMinusGuarantee + guaranteedPower;

				int maximumHardwareLimit = evcs.getMaximumHardwarePower().orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER);

				int nextChargePower;
				var requestedPower = evcs.getSetChargePowerRequestChannel().getNextWriteValue();

				// Power requested by the controller
				if (requestedPower.isPresent()) {
					this.logInfoInDebugmode("Requested power ( for " + evcs.alias() + "): " + requestedPower.get());
					nextChargePower = requestedPower.get();
				} else {
					nextChargePower = maximumHardwareLimit;
				}

				// Total power should be only reduced by the maximum power, that EV is charging.
				int maximumChargePower = evcs.getMaximumPower().orElse(nextChargePower);

				nextChargePower = nextChargePower > maximumHardwareLimit ? maximumHardwareLimit : nextChargePower;

				// Checks if there is enough power left and sets the charge power
				if (maximumChargePower < powerLeft) {
					totalPowerLeftMinusGuarantee = totalPowerLeftMinusGuarantee
							- (maximumChargePower - guaranteedPower);
				} else {
					nextChargePower = powerLeft;
					totalPowerLeftMinusGuarantee = 0;
				}

				/**
				 * Set the next charge power of the EVCS
				 */
				evcs.setChargePowerLimitWithFilter(nextChargePower);
				this.logInfoInDebugmode("Next charge power: " + nextChargePower + "; Max charge power: "
						+ maximumChargePower + "; Power left: " + totalPowerLeftMinusGuarantee);
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
	public List<Evcs> getSortedEvcss() {
		return this.sortedEvcss;
	}

	/**
	 * Maximum power to distribute.
	 *
	 * <p>
	 * Calculate the maximum power to distribute, like excess power or excess power
	 * + storage.
	 *
	 * @return Maximum Power in Watt
	 */
	public int getMaximumPowerToDistribute() {
		// Calculate maximum ess power
		var essDischargePower = this.sum.getEssActivePower().orElse(0);
		var essActivePowerDC = this.sum.getProductionDcActualPower().orElse(0);
		var maxAvailableStoragePower = this.maxEssDischargePower - (essDischargePower - essActivePowerDC);
		this.channel(EvcsClusterPeakShaving.ChannelId.MAXIMUM_AVAILABLE_ESS_POWER)
				.setNextValue(maxAvailableStoragePower);

		// Calculate maximum grid power
		var gridPower = this.getGridPower();
		var maxAvailableGridPower = (this.config.hardwarePowerLimitPerPhase() * Phases.THREE_PHASE.getValue())
				- gridPower;
		this.channel(EvcsClusterPeakShaving.ChannelId.MAXIMUM_AVAILABLE_GRID_POWER).setNextValue(maxAvailableGridPower);

		// Current charge power blocked by all EVCS's
		int evcsCharge = this.getEvcsBlockedChargePowerChannel().getNextValue().orElse(0);

		var allowedChargePower = (int) (evcsCharge + maxAvailableStoragePower + maxAvailableGridPower);

		this.logInfoInDebugmode(this.log,
				"Calculation of the maximum charge Power: EVCS Charge [" + evcsCharge
						+ "]  +  Max. available storage power [" + maxAvailableStoragePower
						+ "]  +  ( Configured Hardware Limit * 3 ["
						+ this.config.hardwarePowerLimitPerPhase() * Phases.THREE_PHASE.getValue()
						+ "]  -  Maximum of all three phases * 3 [" + gridPower + "]");

		return allowedChargePower > 0 ? allowedChargePower : 0;
	}

	/**
	 * Calculates the current grid power depending on the phases if possible.
	 *
	 * @return calculated grid power
	 */
	private int getGridPower() {
		int gridPower = this.meter.getActivePower().orElse(0);

		if (this.meter instanceof AsymmetricMeter) {
			var asymmetricMeter = (AsymmetricMeter) this.meter;

			int gridPowerL1 = asymmetricMeter.getActivePowerL1().orElse(0);
			int gridPowerL2 = asymmetricMeter.getActivePowerL2().orElse(0);
			int gridPowerL3 = asymmetricMeter.getActivePowerL3().orElse(0);

			var maxPowerOnPhase = Math.max(Math.max(gridPowerL1, gridPowerL2), gridPowerL3);
			gridPower = maxPowerOnPhase * 3;
		}
		return gridPower;
	}

	/**
	 * Maximum available grid power.
	 * 
	 * <p>
	 * Calculate the maximum available power from the grid. This value is used as a
	 * fallback option when it becomes negative.
	 * 
	 * @return Current grid power in W
	 */
	public int getAvailableGridPower() {
		// Calculate maximum grid power
		int gridPower = this.getGridPower();
		int maxAvailableGridPower = (this.config.hardwarePowerLimitPerPhase() * Phases.THREE_PHASE.getValue())
				- gridPower;
		return maxAvailableGridPower;
	}

	/**
	 * Check if the cluster should wait for last changes.
	 * 
	 * <p>
	 * Since the charging stations and each car have their own response time until
	 * they charge at the set power, the cluster waits until everything runs
	 * normally or exceptionally wrong.
	 * 
	 * @param clusterState       current evcs cluster state
	 * @param availableGridPower available grid power
	 * @return The cluster should await or not
	 */
	private static boolean awaitLastChanges(EvcsClusterStatus clusterState, int availableGridPower) {
		if (availableGridPower < 0 || clusterState.equals(EvcsClusterStatus.REGULAR)) {
			return false;
		}
		// Still waiting for increasing, decreasing the power or undefined
		return true;
	}

	/**
	 * Results the power that should be guaranteed for one EVCS.
	 *
	 * @param evcs EVCS whose limits should be used.
	 * @return Guaranteed power that should/can be used.
	 */
	private int getGuaranteedPower(Evcs evcs) {
		var minGuarantee = MINIMUM_CHARGE_POWER_GUARANTEE;
		int minHW = evcs.getMinimumHardwarePower().orElse(minGuarantee);
		int evcsMaxPower = evcs.getMaximumPower().orElse(//
				evcs.getMaximumHardwarePower().orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER));
		minGuarantee = evcsMaxPower > minGuarantee ? minGuarantee : evcsMaxPower;
		return minHW > minGuarantee ? minHW : minGuarantee;
	}

	protected void logInfoInDebugmode(Logger log, String string) {
		if (this.config.debugMode()) {
			this.logInfo(log, string);
		}
	}

	private void logInfoInDebugmode(String string) {
		this.logInfoInDebugmode(this.log, string);
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Read maximum ESS Discharge power at the current position in the Cycle
		if (this.ess instanceof ManagedSymmetricEss) {
			var e = (ManagedSymmetricEss) this.ess;
			this.maxEssDischargePower = e.getPower().getMaxPower(e, Phase.ALL, Pwr.ACTIVE);

		} else {
			this.maxEssDischargePower = this.ess.getMaxApparentPower().orElse(0);
		}
	}
}
