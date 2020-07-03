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
		if (!this.isEnabled()) {
			this.channel(ChannelId.EXECUTION_FAILED).setNextValue(false);
			return;
		}
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
			frequency.addValue(pvInverter.getFrequencyChannel());
			minActivePower.addValue(pvInverter.getMinActivePowerChannel());
			maxActivePower.addValue(pvInverter.getMaxActivePowerChannel());
			activePower.addValue(pvInverter.getActivePowerChannel());
			reactivePower.addValue(pvInverter.getReactivePowerChannel());
			activeProductionEnergy.addValue(pvInverter.getActiveProductionEnergyChannel());
			activeConsumptionEnergy.addValue(pvInverter.getActiveConsumptionEnergyChannel());
			voltage.addValue(pvInverter.getVoltageChannel());
			current.addValue(pvInverter.getCurrentChannel());
			// SymmetricPvInverter
			maxApparentPower.addValue(pvInverter.getMaxApparentPowerChannel());
			activePowerLimit.addValue(pvInverter.getActivePowerLimitChannel());
		}

		// SymmetricMeter
		this.getFrequencyChannel().setNextValue(frequency.calculate());
		this._setMinActivePower(minActivePower.calculate());
		this._setMaxActivePower(maxActivePower.calculate());
		this._setActivePower(activePower.calculate());
		this._setReactivePower(reactivePower.calculate());
		this._setActiveProductionEnergy(activeProductionEnergy.calculate());
		this._setActiveConsumptionEnergy(activeConsumptionEnergy.calculate());
		this.getVoltageChannel().setNextValue(voltage.calculate());
		this._setCurrent(current.calculate());
		// SymmetricPvInverter
		this._setMaxApparentPower(maxApparentPower.calculate());
		this._setActivePowerLimit(activePowerLimit.calculate());
	}

	private void distributePvLimit() throws OpenemsNamedException {
		List<ManagedSymmetricPvInverter> pvInverters = this.getPvInverters();

		if (pvInverters.isEmpty()) {
			// No PV inverters?
			return;
		}

		Optional<Integer> activePowerLimitOpt = this.getActivePowerLimitChannel().getNextWriteValueAndReset();
		if (!activePowerLimitOpt.isPresent()) {
			// no value given -> set all limits to undefined.
			for (ManagedSymmetricPvInverter pvInverter : pvInverters) {
				pvInverter.setActivePowerLimit(null);
			}
			return;
		}

		int activePowerLimit = activePowerLimitOpt.get();
		int averageActivePowerLimit = activePowerLimit / pvInverters.size();
		Map<ManagedSymmetricPvInverter, Integer> values = new HashMap<>();
		int toBeDistributed = 0;
		for (ManagedSymmetricPvInverter pvInverter : pvInverters) {
			int maxPower = pvInverter.getMaxApparentPower().getOrError();
			int power = averageActivePowerLimit;
			if (maxPower < power) {
				toBeDistributed += power - maxPower;
				power = maxPower;
			}
			values.put(pvInverter, power);
		}

		for (Entry<ManagedSymmetricPvInverter, Integer> entry : values.entrySet()) {
			if (toBeDistributed > 0) {
				int maxPower = entry.getKey().getMaxApparentPower().getOrError();
				int power = entry.getValue();
				if (maxPower > power) {
					toBeDistributed -= maxPower - power;
					entry.setValue(power);
				}
			}
		}

		// Apply limit
		for (Entry<ManagedSymmetricPvInverter, Integer> entry : values.entrySet()) {
			entry.getKey().setActivePowerLimit(entry.getValue());
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
