package io.openems.edge.evcs.cluster;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class EvcsCluster extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, Evcs {

	private final Logger log = LoggerFactory.getLogger(EvcsCluster.class);
	
	// Default value for the hardware limit
	private static final int DEFAULT_HARDWARE_LIMIT = 22080;
	
	// Total power limit for the whole cluster
	private int totalPowerLimit;
	
	// Used EVCSs
	private String[] evcsIds = new String[0];
	private final List<ManagedEvcs> sortedEvcss = new ArrayList<>();
	private Map<String, ManagedEvcs> _evcss = new ConcurrentHashMap<>();

	// Default minimum charge power, so if it possible every EV will be able to charge
	private final int MINIMUM_EVCS_CHARGING_POWER = 4500; // W

	// Distribute the Power that is not used in a cycle
	private double totalPowerLeftInACycle = 0; // W
	private LocalDateTime lastPowerLeftDistribution = LocalDateTime.now();
	private final int POWER_LEFT_DISTRIBUTION_MIN_TIME = 30; // sec
	private int lastPreferredEvcsCounter = 0;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(ManagedEvcs evcs) {
		if (evcs == this) {
			return;
		}
		this._evcss.put(evcs.id(), evcs);
		evcs.isClustered().setNextValue(true);
		this.updateSortedEvcss();
	}

	protected void removeEvcs(ManagedEvcs evcs) {
		if (evcs == this) {
			return;
		}
		this._evcss.remove(evcs.id());
		evcs.setChargePowerRequest().setNextValue(null);
		evcs.isClustered().setNextValue(false);
		evcs.getMaximumPower().setNextValue(null);
		this.updateSortedEvcss();
	}

	public EvcsCluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.evcsIds = config.evcs_ids();
		updateSortedEvcss();
		super.activate(context, config.id(), config.alias(), config.enabled());

		// Depending on the user inputs, the minimum of the limits will be used;
		int currentHWLimit = config.hardwareCurrentLimit();
		this.totalPowerLimit = currentHWLimit * 230 * 3;

		// update filter for 'evcss' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "evcss", config.evcs_ids())) {
			return;
		}
	}

	/**
	 * Fills sortedEvcss using the order of evcs_ids property in the config
	 */
	private synchronized void updateSortedEvcss() {
		this.sortedEvcss.clear();
		for (String id : this.evcsIds) {
			ManagedEvcs evcs = this._evcss.get(id);
			if (evcs == null) {
				this.logWarn(this.log, "Required Evcs [" + id + "] is not available.");
			} else {
				this.sortedEvcss.add(evcs);
			}
		}
	}

	@Deactivate
	protected void deactivate() {
		for (ManagedEvcs evcs : this.sortedEvcss) {
			evcs.setChargePowerRequest().setNextValue(null);
			evcs.isClustered().setNextValue(false);

			evcs.getMaximumPower().setNextValue(null);
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateChannelValues();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.limitEvcss();
			break;
		}
	}

	/**
	 * Calculates the sum of all EVCS charging powers and set it as channel values
	 * of this clustered EVCS
	 */
	private void calculateChannelValues() {
		final CalculateIntegerSum chargePower = new CalculateIntegerSum();
		final CalculateIntegerSum minHardwarePower = new CalculateIntegerSum();
		final CalculateIntegerSum maxHardwarePowerOfAll = new CalculateIntegerSum();
		final CalculateIntegerSum minPower = new CalculateIntegerSum();

		for (Evcs evcs : this.sortedEvcss) {
			chargePower.addValue(evcs.getChargePower());
			minHardwarePower.addValue(evcs.getMinimumHardwarePower());
			maxHardwarePowerOfAll.addValue(evcs.getMaximumHardwarePower());
			minPower.addValue(evcs.getMinimumPower());
		}
		this.getChargePower().setNextValue(chargePower.calculate());
		this.getMinimumHardwarePower().setNextValue(minHardwarePower.calculate());
		Integer maximalUsedHardwarePower = maxHardwarePowerOfAll.calculate();
		if (maximalUsedHardwarePower == null) {
			maximalUsedHardwarePower = this.totalPowerLimit;
		}
		this.getMaximumHardwarePower().setNextValue(maximalUsedHardwarePower);
		this.getMinimumPower().setNextValue(minPower.calculate());
	}

	/**
	 * Depending on the excess power, the evcss will be charged
	 */
	private void limitEvcss() {
		try {

			// this.totalcurrentPowerLimit = pvMinusConsumtion();

			// If a maximum power is present, e.g. from another cluster, then the limit will
			// be that value or lower
			if (this.getMaximumPower().value().isDefined()) {
				if (this.totalPowerLimit > this.getMaximumPower().value().get()) {
					this.totalPowerLimit = this.getMaximumPower().value().get();
				}
			}

			this.logInfo(this.log, "Maximum Total Power of the whole system: " + this.totalPowerLimit);

			// Total Power that can be distributed to EVCSs minus the guaranteed power
			int totalPowerLeftMinusGuarantee = this.totalPowerLimit;
			
			// Defines the active charging stations that are charging
			List<ManagedEvcs> activeEvcss = new ArrayList<>();
			for (ManagedEvcs evcs : this.sortedEvcss) {
				int requestedPower = evcs.setChargePowerRequest().getNextWriteValue().orElse(0);
				Status status = evcs.status().value().asEnum();
				switch (status) {
				case CHARGING_FINISHED:
					if (requestedPower > 0) {
						evcs.setChargePowerLimit().setNextWriteValue(requestedPower);
					}
					break;
				case ERROR:
				case STARTING:
				case UNDEFINED:
				case NOT_READY_FOR_CHARGING:
				case ENERGY_LIMIT_REACHED:
					break;
				case CHARGING_REJECTED:
				case READY_FOR_CHARGING:
				case CHARGING:
					activeEvcss.add(evcs);
					requestedPower = evcs.setChargePowerRequest().getNextWriteValue().orElse(0);
					if (requestedPower > 0) {

						int evcsMaxPower = evcs.getMaximumPower().value().orElse(DEFAULT_HARDWARE_LIMIT);
						int guarantee = evcsMaxPower > MINIMUM_EVCS_CHARGING_POWER ? MINIMUM_EVCS_CHARGING_POWER
								: evcsMaxPower;
						totalPowerLeftMinusGuarantee -= guarantee;
						evcs.getMinimumPower().setNextValue(guarantee);
					}
				}
			}

			// If one ore more EVCSs no longer active, than change the preferred Evcs
			lastPreferredEvcsCounter = (activeEvcss.size() - 1) < lastPreferredEvcsCounter ? 0 : lastPreferredEvcsCounter;

			this.logInfo(this.log, "Total Power to distribute: " + totalPowerLeftMinusGuarantee);

			// Distributes the available Power to the active EVCSs
			for (int index = 0; index < activeEvcss.size(); index++) {
				ManagedEvcs evcs = activeEvcss.get(index);

				int guarantee = evcs.getMinimumPower().value().orElse(0);

				// Power left for the single EVCS including their guarantee
				int powerLeft = totalPowerLeftMinusGuarantee + guarantee;

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

				int extraPower = calculateExtraPowerIfPrefered(index, evcs);
				this.totalPowerLeftInACycle -= extraPower;
				nextChargePower = nextChargePower + extraPower;

				// Checks if there is enough power left and sets the charge power
				if (nextChargePower < powerLeft) {
					evcs.setChargePowerLimit().setNextWriteValue(nextChargePower);
					totalPowerLeftMinusGuarantee = totalPowerLeftMinusGuarantee - (nextChargePower - guarantee);
					this.logInfo(this.log,
							"Power Left: " + totalPowerLeftMinusGuarantee + " ; Charge power: " + nextChargePower);
				} else {
					evcs.setChargePowerLimit().setNextWriteValue(powerLeft);
					this.logInfo(this.log, "Power Left: " + powerLeft + " ; Charge power: " + powerLeft);
					totalPowerLeftMinusGuarantee = 0;
				}
			}
			if (this.lastPowerLeftDistribution.plusSeconds(POWER_LEFT_DISTRIBUTION_MIN_TIME / 3)
					.isBefore(LocalDateTime.now())) {
				this.lastPreferredEvcsCounter++;
			}
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
	 * Adds extra power that is calculated by the unused power in the cycle before
	 * 
	 * @param index
	 * @param evcs
	 * @return
	 */
	private int calculateExtraPowerIfPrefered(int index, ManagedEvcs evcs) {
		int extraPower = 0;
		if (index == lastPreferredEvcsCounter) {
			int leftToMaxPower = evcs.getMaximumHardwarePower().value().orElse(22800)
					- evcs.getChargePower().value().orElse(0);
			extraPower = (int) (this.totalPowerLeftInACycle - leftToMaxPower);
		}
		return extraPower > 0 ? extraPower : 0;
	}
}
