package io.openems.edge.simulator.pvinverter;

import java.io.IOException;

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

import io.openems.common.channel.Unit;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.PvInverter", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PvInverter extends AbstractOpenemsComponent
		implements ManagedSymmetricPvInverter, SymmetricMeter, OpenemsComponent, TimedataProvider, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SimulatorDatasource datasource;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public PvInverter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private void updateChannels() {
		// copy write value to read value
		this._setActivePowerLimit(this.getActivePowerLimitChannel().getNextWriteValueAndReset().orElse(null));

		/*
		 * get and store Simulated Active Power
		 */
		Integer simulatedActivePower = this.datasource.getValue(OpenemsType.INTEGER,
				new ChannelAddress(this.id(), "ActivePower"));
		this.channel(ChannelId.SIMULATED_ACTIVE_POWER).setNextValue(simulatedActivePower);

		// Apply Active Power Limit
		var activePowerLimitOpt = this.getActivePowerLimit();
		if (simulatedActivePower != null && activePowerLimitOpt.isDefined()) {
			int activePowerLimit = activePowerLimitOpt.get();
			simulatedActivePower = Math.min(simulatedActivePower, activePowerLimit);
		}

		this._setActivePower(simulatedActivePower);
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Production
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Consumption (or in this context: "Negative Production")
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
