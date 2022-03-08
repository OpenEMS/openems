package io.openems.edge.simulator.evcs;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Evcs", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class SimulatedEvcs extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	@Reference
	private EvcsPower evcsPower;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATED_CHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public SimulatedEvcs() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricMeterEvcs.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this._setMaximumHardwarePower(22080);
		this._setMinimumHardwarePower(4200);
		this._setPhases(3);
		this._setStatus(Status.CHARGING);
		this._setPowerPrecision(1);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
		}
	}

	private LocalDateTime lastUpdate = LocalDateTime.now();
	private double exactEnergySession = 0;

	private void updateChannels() {
		int chargePowerLimit = this.getChargePower().orElse(0);

		var chargePowerLimitOpt = this.getSetChargePowerLimitChannel().getNextWriteValueAndReset();
		if (chargePowerLimitOpt.isPresent()) {
			chargePowerLimit = chargePowerLimitOpt.get();
		}
		try {
			if (chargePowerLimit > this.getChargePower().orElse(0)) {
				this.setChargePowerLimitWithFilter(chargePowerLimit);
			} else {
				this.setChargePowerLimit(chargePowerLimit);
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		int sentPower = this.getSetChargePowerLimitChannel().getNextWriteValue().orElse(0);
		this._setSetChargePowerLimit(sentPower);
		this._setChargePower(sentPower);

		/*
		 * get and store Simulated "meter" Active Power
		 */
		this._setActivePower(sentPower);

		var simulatedActivePowerByThree = TypeUtils.divide(sentPower, 3);
		this._setActivePowerL1(simulatedActivePowerByThree);
		this._setActivePowerL2(simulatedActivePowerByThree);
		this._setActivePowerL3(simulatedActivePowerByThree);

		var timeDiff = ChronoUnit.MILLIS.between(this.lastUpdate, LocalDateTime.now());
		var energyTransfered = timeDiff / 1000.0 / 60.0 / 60.0 * this.getChargePower().orElse(0);

		this.exactEnergySession = this.exactEnergySession + energyTransfered;
		this._setEnergySession((int) this.exactEnergySession);

		this.lastUpdate = LocalDateTime.now();
	}

	@Override
	public String debugLog() {
		return this.getChargePower().asString();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public Value<Long> getActiveConsumptionEnergy() {
		return ManagedEvcs.super.getActiveConsumptionEnergy();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_NOT_METERED;
	}

	@Override
	public void _setActiveConsumptionEnergy(Long value) {
		ManagedEvcs.super._setActiveConsumptionEnergy(value);
	}

	@Override
	public void _setActiveConsumptionEnergy(long value) {
		ManagedEvcs.super._setActiveConsumptionEnergy(value);
	}

	@Override
	public LongReadChannel getActiveConsumptionEnergyChannel() {
		return ManagedEvcs.super.getActiveConsumptionEnergyChannel();
	}
}
