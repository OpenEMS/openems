package io.openems.edge.simulator.battery;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.IntegerWriteChannel;
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

	private final Logger log = LoggerFactory.getLogger(BatteryDummy.class);

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
		IntegerWriteChannel disChargeMinVoltageChannel = this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE);
		IntegerWriteChannel chargeMaxVoltageChannel = this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE);
		IntegerWriteChannel disChargeMaxCurrentChannel = this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT);
		IntegerWriteChannel chargeMaxCurrentChannel = this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT);
		IntegerWriteChannel socChannel = this.channel(Battery.ChannelId.SOC);
		IntegerWriteChannel sohChannel = this.channel(Battery.ChannelId.SOH);
		IntegerWriteChannel tempChannel = this.channel(Battery.ChannelId.BATTERY_TEMP);
		IntegerWriteChannel capacityChannel = this.channel(Battery.ChannelId.CAPACITY_KWH);
		IntegerWriteChannel voltageChannel = this.channel(Battery.ChannelId.VOLTAGE);
		IntegerWriteChannel minCellVoltageChannel = this.channel(Battery.ChannelId.MINIMAL_CELL_VOLTAGE);
		IntegerWriteChannel maxPowerChannel = this.channel(Battery.ChannelId.MAXIMAL_POWER);

		try {
			disChargeMinVoltageChannel.setNextWriteValue(disChargeMinVoltage);
			chargeMaxVoltageChannel.setNextWriteValue(chargeMaxVoltage);
			disChargeMaxCurrentChannel.setNextWriteValue(disChargeMaxCurrent);
			chargeMaxCurrentChannel.setNextWriteValue(chargeMaxCurrent);
			socChannel.setNextValue(soc);
			sohChannel.setNextValue(soh);
			tempChannel.setNextValue(temperature);
			capacityChannel.setNextWriteValue(capacityKWh);
		
			disChargeMinVoltageChannel.setNextValue(disChargeMinVoltage);
			chargeMaxVoltageChannel.setNextValue(chargeMaxVoltage);
			disChargeMaxCurrentChannel.setNextValue(disChargeMaxCurrent);
			chargeMaxCurrentChannel.setNextValue(chargeMaxCurrent);
			
			voltageChannel.setNextValue(voltage);
			minCellVoltageChannel.setNextValue(minCellVoltage_mV);
			
			maxPowerChannel.setNextValue(maximalPower_W);

		} catch (OpenemsException e) {
			log.error("Error occurred while writing channel values! " + e.getMessage());
		}
	}

}
