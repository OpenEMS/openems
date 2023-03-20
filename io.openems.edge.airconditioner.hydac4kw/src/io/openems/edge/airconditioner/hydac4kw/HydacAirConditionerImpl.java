package io.openems.edge.airconditioner.hydac4kw;

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

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.hal.api.DigitalIn;
import io.openems.edge.io.hal.api.DigitalOut;
import io.openems.edge.io.hal.modberry.ModBerryX500CM4;
import io.openems.edge.io.hal.modberry.ModberryX500CM4Hardware;
import io.openems.edge.io.hal.raspberrypi.RaspberryPiInterface;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.airconditioner.hydac4kw", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class HydacAirConditionerImpl extends AbstractOpenemsComponent implements HydacAirConditioner, OpenemsComponent, EventHandler {
	
	public HydacAirConditionerImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HydacAirConditioner.ChannelId.values());
	}
	
	private Config config;
	
	@Reference
	RaspberryPiInterface raspberryPiProvider;
	
	private ModBerryX500CM4 hardware;
	private DigitalOut onGpio;
	private DigitalIn error1Gpio;
	private DigitalIn error2Gpio;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.hardware = raspberryPiProvider.getHardwareAs(ModBerryX500CM4.class);
		this.onGpio = this.hardware.getDigitalOut(ModberryX500CM4Hardware.DigitalOut.DOUT_2);
		this.error1Gpio = this.hardware.getDigitalIn(ModberryX500CM4Hardware.OptoDigitalIn.DIN_1);
		this.error2Gpio = this.hardware.getDigitalIn(ModberryX500CM4Hardware.OptoDigitalIn.DIN_2);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
			this.reportError();
			this.controlDevice();
		}
	}
	
	private void reportError() {
//		this.channel(HydacAirConditioner.ChannelId.START_COOLDOWN).setNextValue(true);
		this.channel(HydacAirConditioner.ChannelId.ERROR_1).setNextValue(error1Gpio.isOn());
		this.channel(HydacAirConditioner.ChannelId.ERROR_2).setNextValue(error2Gpio.isOn());
	}
	
	private void controlDevice() {
		boolean isDefined =  this.channel(HydacAirConditioner.ChannelId.ON).value().isDefined();
		this.onGpio.setValue(this.isEnabled() && isDefined && (boolean) this.channel(HydacAirConditioner.ChannelId.ON).value().get());
	}

	@Override
	public String debugLog() {
		return String.format("[Air conditioner Hydac 4kw] Operating State: %s, Error1: %s, Error2: %s", this.onGpio.isOff(), this.error1Gpio.isOn(), this.error1Gpio.isOn());
	}
}
