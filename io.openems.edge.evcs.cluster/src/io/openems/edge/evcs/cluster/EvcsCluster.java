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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;

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
	private final List<Evcs> sortedEvcss = new ArrayList<>();
	private Map<String, Evcs> _evcss = new ConcurrentHashMap<>();
	private double totalPowerLeftInACycle = 0;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(Evcs evcs) {
		// Do not add myself
		if (evcs == this) {
			return;
		}
		this._evcss.put(evcs.id(), evcs);
		this.updateSortedEvcss();
	}

	protected void removeEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this._evcss.remove(evcs.id());
		evcs.getMaximumPower().setNextValue(null);
		if (evcs instanceof ManagedEvcs) {
			((ManagedEvcs) evcs).setChargePowerRequest().setNextValue(null);
			((ManagedEvcs) evcs).isClustered().setNextValue(false);
		}
		evcs.getMaximumPower().setNextValue(null);
		this.updateSortedEvcss();
	}

	public EvcsCluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
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
	}

	/**
	 * Fills sortedEvcss using the order of evcs_ids property in the config
	 */
	private synchronized void updateSortedEvcss() {
		this.sortedEvcss.clear();
		for (String id : this.evcsIds) {
			Evcs evcs = this._evcss.get(id);
			if (evcs == null) {
				this.logWarn(this.log, "Required Evcs [" + id + "] is not available.");
			} else {
				this.sortedEvcss.add(evcs);
			}
		}
	}

	@Deactivate
	protected void deactivate() {
		for (Evcs evcs : this.sortedEvcss) {
			if (evcs instanceof ManagedEvcs) {
				((ManagedEvcs) evcs).setChargePowerRequest().setNextValue(null);
				((ManagedEvcs) evcs).isClustered().setNextValue(false);
			}
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

			this.logDebug(this.log, "Total current powerlimit: " + this.totalcurrentPowerLimit);
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

		for (Evcs evcs : this.sortedEvcss) {
			chargePower.addValue(evcs.getChargePower());
			minHardwarePower.addValue(evcs.getMinimumHardwarePower());
			maxHardwarePowerOfAll.addValue(evcs.getMaximumHardwarePower());
		}
		this.getChargePower().setNextValue(chargePower.calculate());
		this.getMinimumHardwarePower().setNextValue(minHardwarePower.calculate());
		this.maximalUsedHardwarePower = maxHardwarePowerOfAll.calculate();
		if (this.maximalUsedHardwarePower == null) {
			this.maximalUsedHardwarePower = this.totalcurrentPowerLimit;
		}
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

			double totalPowerLeft = this.totalcurrentPowerLimit;

			this.logInfo(this.log, "Maximum Total Power of the whole system: " + totalPowerLeft);

			for (Evcs evcs : this.sortedEvcss) {

				if (evcs instanceof ManagedEvcs) {
					int extraPower = 0;
					((ManagedEvcs) evcs).isClustered().setNextValue(true);

					int nextChargePower;
					Optional<Integer> requestedPower = ((ManagedEvcs) evcs).setChargePowerRequest().getNextWriteValue();

					if (requestedPower.isPresent()) {
						this.logInfo(this.log, "Requested Power ( for " + evcs.alias() + "): " + requestedPower.get());
						nextChargePower = requestedPower.get();
					} else {
						nextChargePower = evcs.getMaximumHardwarePower().value().orElse(22080);
					}

					if (nextChargePower > 0) {
						int powerTillMaximum = evcs.getMaximumHardwarePower().value().orElse(22080) - nextChargePower;
						if (powerTillMaximum < this.totalPowerLeftInACycle) {
							extraPower = powerTillMaximum;
							this.totalPowerLeftInACycle -= extraPower;
						}
						nextChargePower = nextChargePower + extraPower;
					}
					if (nextChargePower < totalPowerLeft) {
						((ManagedEvcs) evcs).setChargePower().setNextWriteValue(nextChargePower);
						this.logInfo(this.log, "Power Left: " + totalPowerLeft + " ; Charge power: " + nextChargePower);
						totalPowerLeft = totalPowerLeft - nextChargePower;
					} else {
						((ManagedEvcs) evcs).setChargePower().setNextWriteValue((int) totalPowerLeft);
						this.logInfo(this.log, "Power Left: " + totalPowerLeft + " ; Charge power: " + totalPowerLeft);
						totalPowerLeft = 0;
					}
				} else {
					// Not Managed EVCS
					int evcsHardwarePower = evcs.getMaximumHardwarePower().value().orElse(22080);
					if (evcsHardwarePower < totalPowerLeft) {
						evcs.getMaximumPower().setNextValue(evcsHardwarePower);
						totalPowerLeft = totalPowerLeft - evcsHardwarePower;
					} else {
						evcs.getMaximumPower().setNextValue(totalPowerLeft);
						totalPowerLeft = 0;
					}
				}
			}
			if (totalPowerLeft > 0) {
				this.totalPowerLeftInACycle = totalPowerLeft;
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}
}
