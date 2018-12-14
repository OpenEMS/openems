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
@Component( //
		name = "Bms.Simulated", //
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
	private int minCellVoltage_mV;
	private int maximalPower_W;

	public BatteryDummy() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		disChargeMinVoltage = config.disChargeMinVoltage();
		chargeMaxVoltage = config.chargeMaxVoltage();
		disChargeMaxCurrent = config.disChargeMaxCurrent();
		chargeMaxCurrent = config.chargeMaxCurrent();
		soc = config.soc();
		soh = config.soh();
		temperature = config.temperature();
		capacityKWh = config.capacityKWh();
		voltage = config.voltage();
		minCellVoltage_mV = config.minCellVoltage_mV();
		maximalPower_W = config.maximalPower_W();
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
		this.getDischargeMinVoltage().setNextValue(disChargeMinVoltage);
		this.getChargeMaxVoltage().setNextValue(chargeMaxVoltage);
		this.getDischargeMaxCurrent().setNextValue(disChargeMaxCurrent);
		this.getChargeMaxCurrent().setNextValue(chargeMaxCurrent);
		this.getSoc().setNextValue(soc);
		this.getSoh().setNextValue(soh);
		this.getMinCellTemperature().setNextValue(temperature);
		this.getMaxCellTemperature().setNextValue(temperature);
		this.getCapacity().setNextValue(capacityKWh);

		this.getVoltage().setNextValue(voltage);
		this.getMinCellVoltage().setNextValue(minCellVoltage_mV);
		this.getMaxCellVoltage().setNextValue(minCellVoltage_mV);

		this.getMaxPower().setNextValue(maximalPower_W);
	}

}
