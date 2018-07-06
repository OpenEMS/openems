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

	private int capacity_kWh;
	
	public BatteryDummy() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}
	
	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.capacity_kWh = config.capacity_kWh();
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
		this.getChargeMaxCurrent();
		this.getChargeMaxVoltage();
		this.getDischargeMaxCurrent();
		this.getDischargeMinVoltage();
		this.getGridMode();
		this.getMaxCapacity().setNextValue(capacity_kWh);;
		this.getSoc().setNextValue(50);;
		this.getState();
	}

}
