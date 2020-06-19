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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
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

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.EssSymmetric.Reacting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class EssSymmetric extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssSymmetric.class);

	/**
	 * Current state of charge.
	 */
	private float soc = 0;

	private Config config;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BMS_AMPERE_HOURS(Doc.of(OpenemsType.LONG).unit(Unit.AMPERE_HOURS));
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

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;
		this.soc = config.initialSoc();
		this._setSoc(config.initialSoc());
		this._setMaxApparentPower(config.maxApparentPower());
		this._setAllowedChargePower(config.maxApparentPower() * -1);
		this._setAllowedDischargePower(config.maxApparentPower());
		this._setGridMode(config.gridMode());
		this._setCapacity(config.capacity());
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
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
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
		// TODO timedelta
		float watthours = (float) activePower * 60 / 3600;
		// float watthours = (float) activePower * this.datasource.getTimeDelta() /
		// 3600;
		float socChange = watthours / this.config.capacity();
		this.soc -= socChange;
		if (this.soc > 100) {
			this.soc = 100;
		} else if (this.soc < 0) {
			this.soc = 0;
		}
		this._setSoc(Math.round(this.soc));
		/*
		 * Apply Active/Reactive power to simulated channels
		 */
		if (soc == 0 && activePower > 0) {
			activePower = 0;
		}
		if (soc == 100 && activePower < 0) {
			activePower = 0;
		}
		this._setActivePower(activePower);
		if (soc == 0 && reactivePower > 0) {
			reactivePower = 0;
		}
		if (soc == 100 && reactivePower < 0) {
			reactivePower = 0;
		}
		this._setReactivePower(reactivePower);
		/*
		 * Set AllowedCharge / Discharge based on SoC
		 */
		if (this.soc == 100) {
			this._setAllowedChargePower(0);
		} else {
			this._setAllowedChargePower(this.config.maxApparentPower() * -1);
		}
		if (this.soc == 0) {
			this._setAllowedDischargePower(0);
		} else {
			this._setAllowedDischargePower(this.config.maxApparentPower());
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

			this.logDebug(this.log, "time elpsed in ms: " + passedTimeInMilliSeconds);
			this.logDebug(this.log, "last power value :" + this.lastPowerValue);
			double energy = this.lastPowerValue * (passedTimeInMilliSeconds / 1000) / 3600;
			// calculate energy in watt hours

			this.logDebug(this.log, "energy in wh: " + energy);

			if (this.lastPowerValue < 0) {
				this.accumulatedChargeEnergy = this.accumulatedChargeEnergy + energy;
				this._setActiveChargeEnergy((long) accumulatedChargeEnergy);
			} else if (this.lastPowerValue > 0) {
				this.accumulatedDischargeEnergy = this.accumulatedDischargeEnergy + energy;
				this._setActiveDischargeEnergy((long) accumulatedDischargeEnergy);
			}
			this.channel(ChannelId.BMS_AMPERE_HOURS).setNextValue((accumulatedChargeEnergy + accumulatedDischargeEnergy) / 110);

			this.logDebug(this.log, "accumulated charge energy :" + accumulatedChargeEnergy);
			this.logDebug(this.log, "accumulated discharge energy :" + accumulatedDischargeEnergy);

		} else {
			this.lastPowerValuesTimestamp = LocalDateTime.now();
		}

		this.lastPowerValue = this.getActivePower().orElse(0);
	}
}
