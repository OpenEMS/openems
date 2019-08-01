package io.openems.edge.simulator.ess.symmetric.reacting;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.EssSymmetric.Reacting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class EssSymmetric extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	/**
	 * Current state of charge.
	 */
	private float soc = 0;

	/**
	 * Total configured capacity in Wh.
	 */
	private int capacity = 0;

	/**
	 * Configured max Apparent Power in VA.
	 */
	private int maxApparentPower = 0;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SimulatorDatasource datasource;

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}

		this.getSoc().setNextValue(config.initialSoc());
		this.soc = config.initialSoc();
		this.capacity = config.capacity();
		this.maxApparentPower = config.maxApparentPower();
		this.getMaxApparentPower().setNextValue(config.maxApparentPower());
		this.getAllowedCharge().setNextValue(this.maxApparentPower * -1);
		this.getAllowedDischarge().setNextValue(this.maxApparentPower);
		this.getGridMode().setNextValue(config.gridMode());
		this.getCapacity().setNextValue(this.capacity);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssSymmetric() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.updateChannels();
			this.calculateEnergy();
			break;
		}
	}

	private void updateChannels() {
		// nothing to do
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsException {
		/*
		 * calculate State of charge
		 */
		float watthours = (float) activePower * this.datasource.getTimeDelta() / 3600;
		float socChange = watthours / this.capacity;
		this.soc -= socChange;
		if (this.soc > 100) {
			this.soc = 100;
		} else if (this.soc < 0) {
			this.soc = 0;
		}
		this.getSoc().setNextValue(this.soc);
		/*
		 * Apply Active/Reactive power to simulated channels
		 */
		if (soc == 0 && activePower > 0) {
			activePower = 0;
		}
		if (soc == 100 && activePower < 0) {
			activePower = 0;
		}
		this.getActivePower().setNextValue(activePower);
		if (soc == 0 && reactivePower > 0) {
			reactivePower = 0;
		}
		if (soc == 100 && reactivePower < 0) {
			reactivePower = 0;
		}
		this.getReactivePower().setNextValue(reactivePower);
		/*
		 * Set AllowedCharge / Discharge based on SoC
		 */
		if (this.soc == 100) {
			this.getAllowedCharge().setNextValue(0);
		} else {
			this.getAllowedCharge().setNextValue(this.maxApparentPower * -1);
		}
		if (this.soc == 0) {
			this.getAllowedDischarge().setNextValue(0);
		} else {
			this.getAllowedDischarge().setNextValue(this.maxApparentPower);
		}
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssSymmetric.class, accessMode, 300) //
						.build());
	}

	// These variables are used to calculate the energy
	LocalDateTime lastPowerValuesTimestamp = null;
	double lastPowerValue = 0;
	double accumulatedChargeEnergy = 0;
	double accumulatedDischargeEnergy = 0;

	private void calculateEnergy() {
		if (this.lastPowerValuesTimestamp != null) {

			long passedTimeInMilliSeconds = Duration.between(this.lastPowerValuesTimestamp, LocalDateTime.now())
					.toMillis();
			this.lastPowerValuesTimestamp = LocalDateTime.now();

			log.debug("time elpsed in ms: " + passedTimeInMilliSeconds);
			log.debug("last power value :" + this.lastPowerValue);
			double energy = this.lastPowerValue * (passedTimeInMilliSeconds / 1000) / 3600;
			// calculate energy in watt hours

			log.debug("energy in wh: " + energy);

			if (this.lastPowerValue < 0) {
				this.accumulatedChargeEnergy = this.accumulatedChargeEnergy + energy;
				this.getActiveChargeEnergy().setNextValue(accumulatedChargeEnergy);
			} else if (this.lastPowerValue > 0) {
				this.accumulatedDischargeEnergy = this.accumulatedDischargeEnergy + energy;
				this.getActiveDischargeEnergy().setNextValue(accumulatedDischargeEnergy);
			}

			log.debug("accumulated charge energy :" + accumulatedChargeEnergy);
			log.debug("accumulated discharge energy :" + accumulatedDischargeEnergy);

		} else {
			this.lastPowerValuesTimestamp = LocalDateTime.now();
		}

		this.lastPowerValue = this.getActivePower().value().orElse(0);
	}
}
