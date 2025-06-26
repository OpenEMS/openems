package io.openems.edge.simulator.battery;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SimulatorBatteryImpl extends AbstractOpenemsComponent
		implements SimulatorBattery, Battery, OpenemsComponent, EventHandler, StartStoppable {

	private int disChargeMinVoltage;
	private int chargeMaxVoltage;
	private int disChargeMaxCurrent;
	private int chargeMaxCurrent;
	private int soc;
	private int soh;
	private int temperature;
	private int capacityKWh;
	private int voltage;
	private int minCellVoltage; // in mV

	public SimulatorBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SimulatorBattery.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.disChargeMinVoltage = config.disChargeMinVoltage();
		this.chargeMaxVoltage = config.chargeMaxVoltage();
		this.disChargeMaxCurrent = config.disChargeMaxCurrent();
		this.chargeMaxCurrent = config.chargeMaxCurrent();
		this.soc = config.soc();
		this.soh = config.soh();
		this.temperature = config.temperature();
		this.capacityKWh = config.capacityKWh();
		this.voltage = config.voltage();
		this.minCellVoltage = config.minCellVoltage_mV();
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

	private void updateChannels() {
		this._setDischargeMinVoltage(this.disChargeMinVoltage);
		this._setChargeMaxVoltage(this.chargeMaxVoltage);
		this._setDischargeMaxCurrent(this.disChargeMaxCurrent);
		this._setChargeMaxCurrent(this.chargeMaxCurrent);
		this._setSoc(this.soc);
		this._setSoh(this.soh);
		this._setMinCellTemperature(this.temperature);
		this._setMaxCellTemperature(this.temperature);
		this._setCapacity(this.capacityKWh);

		this._setVoltage(this.voltage);
		this._setMinCellVoltage(this.minCellVoltage);
		this._setMaxCellVoltage(this.minCellVoltage);

		this._setStartStop(StartStop.START);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO start stop is not implemented
		throw new NotImplementedException("Start Stop is not implemented for Soltaro SingleRack Version B");
	}

}
