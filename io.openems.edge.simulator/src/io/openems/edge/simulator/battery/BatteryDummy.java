package io.openems.edge.simulator.battery;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Bms", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
)
public class BatteryDummy extends AbstractOpenemsComponent implements Battery, OpenemsComponent, EventHandler {

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

	public BatteryDummy() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
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
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		this.getDischargeMinVoltage().setNextValue(this.disChargeMinVoltage);
		this.getChargeMaxVoltage().setNextValue(this.chargeMaxVoltage);
		this.getDischargeMaxCurrent().setNextValue(this.disChargeMaxCurrent);
		this.getChargeMaxCurrent().setNextValue(this.chargeMaxCurrent);
		this.getSoc().setNextValue(this.soc);
		this.getSoh().setNextValue(this.soh);
		this.getMinCellTemperature().setNextValue(this.temperature);
		this.getMaxCellTemperature().setNextValue(this.temperature);
		this.getCapacity().setNextValue(this.capacityKWh);

		this.getVoltage().setNextValue(this.voltage);
		this.getMinCellVoltage().setNextValue(this.minCellVoltage);
		this.getMaxCellVoltage().setNextValue(this.minCellVoltage);
	}

}
