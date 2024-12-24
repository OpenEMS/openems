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

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class SimulatorEvcsImpl extends AbstractManagedEvcsComponent
		implements SimulatorEvcs, ManagedEvcs, Evcs, ElectricityMeter, OpenemsComponent, EventHandler {

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private ConfigurationAdmin cm;

	private Config config;

	public SimulatorEvcsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				SimulatorEvcs.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
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
		this._setActivePower(chargePowerLimit);

		/*
		 * Set Simulated "meter" Active Power
		 */
		this._setActivePower(chargePowerLimit);

		/*
		 * Set calculated energy
		 */
		var timeDiff = ChronoUnit.MILLIS.between(this.lastUpdate, LocalDateTime.now());
		var energyTransfered = timeDiff / 1000.0 / 60.0 / 60.0 * this.getActivePower().orElse(0);

		this.exactEnergySession = this.exactEnergySession + energyTransfered;
		this._setEnergySession((int) this.exactEnergySession);

		this.lastUpdate = LocalDateTime.now();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
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
		this._setActivePower(power);
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
}
