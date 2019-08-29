package io.openems.edge.evcs.cluster;

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
	private int totalcurrentPowerLimit;
	private Integer maximalUsedHardwarePower;
	private String[] evcsIds = new String[0];
	private final List<ManagedEvcs> sortedEvcss = new ArrayList<>();
	private Map<String, ManagedEvcs> _evcss = new ConcurrentHashMap<>();
	private double totalPowerLeftInACycle = 0;
	private final int MINIMUM_EVCS_CHARGING_POWER = 4500;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(ManagedEvcs evcs) {
		// Do not add myself
		if (evcs == this) {
			return;
		}
		this._evcss.put(evcs.id(), evcs);
		this.updateSortedEvcss();
	}

	protected void removeEvcs(ManagedEvcs evcs) {
		if (evcs == this) {
			return;
		}
		this._evcss.remove(evcs.id());
		evcs.getMaximumPower().setNextValue(null);
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
		this.totalcurrentPowerLimit = currentHWLimit * 230 * 3;

		// update filter for 'evcss' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "evcss", config.evcs_ids())) {
			return;
		}
		for (Evcs evcs : sortedEvcss) {
			if (evcs instanceof ManagedEvcs) {
				((ManagedEvcs) evcs).isClustered().setNextValue(true);
			}
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
		this.maximalUsedHardwarePower = maxHardwarePowerOfAll.calculate();
		if (this.maximalUsedHardwarePower == null) {
			this.maximalUsedHardwarePower = this.totalcurrentPowerLimit;
		}
		this.getMaximumHardwarePower().setNextValue(this.maximalUsedHardwarePower);
		;
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
				if (this.totalcurrentPowerLimit > this.getMaximumPower().value().get()) {
					this.totalcurrentPowerLimit = this.getMaximumPower().value().get();
				}
			}

			this.logInfo(this.log, "Maximum Total Power of the whole system: " + this.totalcurrentPowerLimit);

			int evcssCanBeCharged = this.totalcurrentPowerLimit / MINIMUM_EVCS_CHARGING_POWER;

			if (evcssCanBeCharged < 1) {
				this.logInfo(this.log, "Not enough excess power for charging");
				return;
			}

			// Defines the active charging stations that are charging
			List<ManagedEvcs> activeEvcss = new ArrayList<>();
			for (ManagedEvcs evcs : this.sortedEvcss) {
				int requestedPower = evcs.setChargePowerRequest().getNextWriteValue().orElse(0);
				if (requestedPower > 0) {
					activeEvcss.add(evcs);
				} else {
					evcs.setChargePower().setNextWriteValue(0);
				}
			}

			int totalPowerLeftMinusGuarantee = this.totalcurrentPowerLimit - (activeEvcss.size() * MINIMUM_EVCS_CHARGING_POWER);

			for (ManagedEvcs evcs : activeEvcss) {
				int powerLeft = totalPowerLeftMinusGuarantee + MINIMUM_EVCS_CHARGING_POWER;
				
				int nextChargePower;
				Optional<Integer> requestedPower = evcs.setChargePowerRequest().getNextWriteValue();

				if (requestedPower.isPresent()) {
					this.logInfo(this.log, "Requested Power ( for " + evcs.alias() + "): " + requestedPower.get());
					nextChargePower = requestedPower.get();
				} else {
					nextChargePower = evcs.getMaximumHardwarePower().value().orElse(22080);
				}

				int extraPower = (int) (this.totalPowerLeftInACycle / activeEvcss.size());
				nextChargePower = nextChargePower + extraPower;

				if (nextChargePower < powerLeft) {
					evcs.setChargePower().setNextWriteValue(nextChargePower);
					this.logInfo(this.log, "Power Left: " + totalPowerLeftMinusGuarantee + " ; Charge power: " + nextChargePower);
					totalPowerLeftMinusGuarantee = totalPowerLeftMinusGuarantee - (nextChargePower-MINIMUM_EVCS_CHARGING_POWER);
				} else {
					evcs.setChargePower().setNextWriteValue(powerLeft);
					this.logInfo(this.log, "Power Left: " + powerLeft + " ; Charge power: "+ powerLeft);
					totalPowerLeftMinusGuarantee = 0;
				}
			}
			if (totalPowerLeftMinusGuarantee > 0) {
				this.totalPowerLeftInACycle = totalPowerLeftMinusGuarantee;
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}
}
