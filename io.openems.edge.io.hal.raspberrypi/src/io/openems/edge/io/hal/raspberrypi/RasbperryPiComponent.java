package io.openems.edge.io.hal.raspberrypi;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.io.hal.linuxfs.HardwareFactory;
import io.openems.edge.io.hal.modberry.RaspberryPiPlattform;
import io.openems.edge.io.hal.modberry.ModBerryX500CM4;
import io.openems.edge.controller.api.Controller;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.hal.raspberrypi", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		scope = ServiceScope.SINGLETON
)
public class RasbperryPiComponent extends AbstractOpenemsComponent implements RaspberryPiInterface, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(RasbperryPiComponent.class);
	
	private Config config;
	private HardwareFactory gpioFactory;
	private RaspberryPiPlattform hardwarePlattform;
	
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
		this.log.debug("Loading HAL for Raspberry Pi...");
		this.gpioFactory = new HardwareFactory(config.gpioPath());
		this.hardwarePlattform = this.createHardwarePlattform(config.hardwarePlattform());

	}


	@Deactivate
	protected void deactivate() {
		this.logError(this.log, "Shutting down Pi4J context.");
		super.deactivate();
	}
	
	private RaspberryPiPlattform createHardwarePlattform(HardwarePlattformEnum selectedHardware) {
		if (selectedHardware.equals(HardwarePlattformEnum.MODBERRY_X500_CM4)) {
			return new ModBerryX500CM4(this.gpioFactory);
		} else {
			throw new IllegalArgumentException("Hardware plattform not configured properly. Value " + selectedHardware.asCamelCase());
		}
	}

	@Override
	public <T extends RaspberryPiPlattform> T getHardwareAs(Class<T> clazz) {
		return clazz.cast(this.hardwarePlattform);
	}
	
}
