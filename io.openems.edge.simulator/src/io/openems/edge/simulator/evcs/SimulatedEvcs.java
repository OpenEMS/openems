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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Evcs", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class SimulatedEvcs extends AbstractManagedEvcsComponent
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

		// TODO: Remove AsymmetricMeterEvcs if the EVCS Nature already implements a new
		// or parts of the Meter Nature
		// Therefore, some of the EVCS Nature Channels have to be changed or removed.
		// Omit SymmetricMeter and add AsymmetricMeterEvcs because of duplicated default
		// set and get methods in EVCS and SymmetricMeter.
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

	private Config config;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this._setPhases(3);
		this._setPowerPrecision(1);
		this._setStatus(Status.READY_FOR_CHARGING);
		this._setChargingstationCommunicationFailed(false);
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
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
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			this.updateChannels();
			break;
		}
	}

	private LocalDateTime lastUpdate = LocalDateTime.now();
	private double exactEnergySession = 0;

	private void updateChannels() {
		int chargePowerLimit = this.getSetChargePowerLimit().orElse(0);
		this._setChargePower(chargePowerLimit);

		/*
		 * Set Simulated "meter" Active Power
		 */
		this._setActivePower(chargePowerLimit);
		var simulatedActivePowerByThree = TypeUtils.divide(chargePowerLimit, 3);
		this._setActivePowerL1(simulatedActivePowerByThree);
		this._setActivePowerL2(simulatedActivePowerByThree);
		this._setActivePowerL3(simulatedActivePowerByThree);

		/*
		 * Set calculated energy
		 */
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
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		this._setSetChargePowerLimit(power);
		this._setChargePower(power);
		this._setStatus(power > 0 ? Status.CHARGING : Status.CHARGING_REJECTED);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 10;
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public Value<Long> getActiveConsumptionEnergy() {
		return this.getActiveConsumptionEnergyChannel().getNextValue();
	}

	@Override
	public void _setActiveConsumptionEnergy(long value) {
		this.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY).setNextValue(value);
	}

	@Override
	public void _setActiveConsumptionEnergy(Long value) {
		this.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY).setNextValue(value);
	}

	@Override
	public LongReadChannel getActiveConsumptionEnergyChannel() {
		return this.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}

	@Override
	public MeterType getMeterType() {
		// TODO: This should be `MeterType.CONSUMPTION_METERED`, once Evcs actually
		// implements Meter. For now this quick fix solves issues with calculating
		// `_sum/GridActivePower`.
		return MeterType.GRID;
	}
}
