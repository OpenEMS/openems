package io.openems.edge.io.hal.pi4j;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.modberry.Cm4Hardware;
import io.openems.edge.io.hal.modberry.ModBerryX500CM4;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.LinuxFS.HAL", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
//		scope = ServiceScope.SINGLETON //
)
public class RasbperryPiComponent extends AbstractOpenemsComponent implements RaspberryPiInterface, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(RasbperryPiComponent.class);
	
	private Config config;

//	private Led led;

	public RasbperryPiComponent() {
		super(OpenemsComponent.ChannelId.values(),
				RaspberryPiInterface.ChannelId.values());
	}
	
	protected RasbperryPiComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
//		log.error("Loading pi4j component");
//		var hardware = new ModBerryX500CM4();
//		System.out.println("Hardware loaded");
//		this.led = hardware.getLed(Cm4Hardware.Led.LED_1);
	}

	@Deactivate
	protected void deactivate() {
		this.logError(this.log, "Shutting down Pi4J context.");
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
//		Optional.ofNullable(this.led).ifPresent(Led::toggle);
	}
}
