package io.openems.edge.pvinverter.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "PvInverter.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
				"type=PRODUCTION" //
		})
public class PvInverterCluster extends AbstractOpenemsComponent
		implements ManagedSymmetricPvInverter, SymmetricMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(PvInverterCluster.class);

	@Reference
	protected ComponentManager componentManager;

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		EXECUTION_FAILED(Doc.of(Level.FAULT).text("Execution failed"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public PvInverterCluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		try {
			switch (event.getTopic()) {

			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
				this.calculateChannelValues();
				break;

			case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
				this.distributePvLimit();
				break;
			}

			this.channel(ChannelId.EXECUTION_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(ChannelId.EXECUTION_FAILED).setNextValue(true);
			this.logError(this.log, "Failed to distribute PV-Limit: " + e.getMessage());
		}
	}

	/**
	 * Calculates the sum-value for each Channel.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void calculateChannelValues() throws OpenemsNamedException {
		List<ManagedSymmetricPvInverter> pvInverters = this.getPvInverters();

		// SymmetricMeter
		final CalculateAverage frequency = new CalculateAverage();
		final CalculateIntegerSum minActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum maxActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum activePower = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePower = new CalculateIntegerSum();
		final CalculateLongSum activeProductionEnergy = new CalculateLongSum();
		final CalculateLongSum activeConsumptionEnergy = new CalculateLongSum();
		final CalculateAverage voltage = new CalculateAverage();
		final CalculateIntegerSum current = new CalculateIntegerSum();
		// SymmetricPvInverter
		final CalculateIntegerSum maxApparentPower = new CalculateIntegerSum();
		final CalculateIntegerSum activePowerLimit = new CalculateIntegerSum();

		for (ManagedSymmetricPvInverter pvInverter : pvInverters) {
			// SymmetricMeter
			frequency.addValue(pvInverter.getFrequency());
			minActivePower.addValue(pvInverter.getMinActivePower());
			maxActivePower.addValue(pvInverter.getMaxActivePower());
			activePower.addValue(pvInverter.getActivePower());
			reactivePower.addValue(pvInverter.getReactivePower());
			activeProductionEnergy.addValue(pvInverter.getActiveProductionEnergy());
			activeConsumptionEnergy.addValue(pvInverter.getActiveConsumptionEnergy());
			voltage.addValue(pvInverter.getVoltageChannel());
			current.addValue(pvInverter.getCurrentChannel());
			// SymmetricPvInverter
			maxApparentPower.addValue(pvInverter.getMaxApparentPower());
			activePowerLimit.addValue(pvInverter.getActivePowerLimit());
		}

		// SymmetricMeter
		this.getFrequency().setNextValue(frequency.calculate());
		this.getMinActivePower().setNextValue(minActivePower.calculate());
		this.getMaxActivePower().setNextValue(maxActivePower.calculate());
		this.getActivePower().setNextValue(activePower.calculate());
		this.getReactivePower().setNextValue(reactivePower.calculate());
		this.getActiveProductionEnergy().setNextValue(activeProductionEnergy.calculate());
		this.getActiveConsumptionEnergy().setNextValue(activeConsumptionEnergy.calculate());
		this.getVoltageChannel().setNextValue(voltage.calculate());
		this._setCurrent(current.calculate());
		// SymmetricPvInverter
		this.getMaxApparentPower().setNextValue(maxApparentPower.calculate());
		this.getActivePowerLimit().setNextValue(activePowerLimit.calculate());
	}

	private void distributePvLimit() throws OpenemsNamedException {
		List<ManagedSymmetricPvInverter> pvInverters = this.getPvInverters();

		if (pvInverters.isEmpty()) {
			// No PV inverters?
			return;
		}

		Optional<Integer> activePowerLimitOpt = this.getActivePowerLimit().getNextWriteValueAndReset();
		if (!activePowerLimitOpt.isPresent()) {
			// no value given -> set all limits to undefined.
			for (ManagedSymmetricPvInverter pvInverter : pvInverters) {
				pvInverter.getActivePowerLimit().setNextWriteValue(null);
			}
			return;
		}

		int activePowerLimit = activePowerLimitOpt.get();
		int averageActivePowerLimit = activePowerLimit / pvInverters.size();
		Map<ManagedSymmetricPvInverter, Integer> values = new HashMap<>();
		int toBeDistributed = 0;
		for (ManagedSymmetricPvInverter pvInverter : pvInverters) {
			int maxPower = pvInverter.getMaxApparentPower().value().getOrError();
			int power = averageActivePowerLimit;
			if (maxPower < power) {
				toBeDistributed += power - maxPower;
				power = maxPower;
			}
			values.put(pvInverter, power);
		}

		for (Entry<ManagedSymmetricPvInverter, Integer> entry : values.entrySet()) {
			if (toBeDistributed > 0) {
				int maxPower = entry.getKey().getMaxApparentPower().value().getOrError();
				int power = entry.getValue();
				if (maxPower > power) {
					toBeDistributed -= maxPower - power;
					entry.setValue(power);
				}
			}
		}

		// Apply limit
		for (Entry<ManagedSymmetricPvInverter, Integer> entry : values.entrySet()) {
			entry.getKey().getActivePowerLimit().setNextWriteValue(entry.getValue());
		}
	}

	private List<ManagedSymmetricPvInverter> getPvInverters() throws OpenemsNamedException {
		List<ManagedSymmetricPvInverter> result = new ArrayList<>();
		for (String pvInverterId : this.config.pvInverter_ids()) {
			ManagedSymmetricPvInverter pvInverter = this.componentManager.getComponent(pvInverterId);
			result.add(pvInverter);
		}
		return result;
	}
}
