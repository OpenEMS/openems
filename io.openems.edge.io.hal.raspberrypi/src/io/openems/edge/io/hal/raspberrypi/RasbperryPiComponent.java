package io.openems.edge.io.hal.raspberrypi;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.event.Event;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.linuxfs.HardwareFactory;
import io.openems.edge.io.hal.modberry.Cm4Hardware;
import io.openems.edge.io.hal.modberry.ModBerryX500CM4;
import io.openems.edge.controller.api.Controller;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.hal.raspberrypi", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		scope = ServiceScope.SINGLETON
)
public class RasbperryPiComponent extends AbstractOpenemsComponent implements Controller, RaspberryPiInterface, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(RasbperryPiComponent.class);
	
	private Config config;

	private Led led;
	private byte status = 0;

	public RasbperryPiComponent() {
		super(OpenemsComponent.ChannelId.values(),
				RaspberryPiInterface.ChannelId.values(),
				Controller.ChannelId.values());
	}
	
	protected RasbperryPiComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		log.debug("Loading HAL for Raspberry Pi...");
		var gpioFactory = new HardwareFactory(config.gpioPath());
		var hardware = new ModBerryX500CM4(gpioFactory);
		System.out.println("Hardware loaded");
		this.led = hardware.getLed(Cm4Hardware.Led.LED_1);
		var timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				led.toggle();
			}
		}, 0, 2000);
	}

	@Deactivate
	protected void deactivate() {
		this.logError(this.log, "Shutting down Pi4J context.");
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		System.out.println("\nCalled run of Pi4J component");
		if(status % 5 == 0) {
			Optional.ofNullable(this.led).ifPresent(Led::toggle);
			status = (byte) (status++ % 5);
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		System.out.println("Handle event");
	}
}
