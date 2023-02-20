package io.openems.edge.io.hal.pi4j;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.hal.pi4j.modberry.Cm4Hardware;
import io.openems.edge.io.hal.pi4j.modberry.ModBerryX500CM4;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.RaspberryPi.HAL", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		scope = ServiceScope.SINGLETON //
)
public class Pi4JComponent extends AbstractOpenemsComponent implements Pi4JInterface, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Pi4JComponent.class);
	
	private Config config;
	private Context pi4j;
	
	public Pi4JComponent() {
		super(OpenemsComponent.ChannelId.values(),
				Pi4JInterface.ChannelId.values());
	}
	
	protected Pi4JComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}
	
	public ModBerryX500CM4 getInstance() {
		return new ModBerryX500CM4(pi4j);
	}


	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.pi4j = Pi4J.newAutoContext();
		this.pi4j.platforms().describe().print(System.out);
		this.pi4j = Pi4J.newContextBuilder()
				.noAutoDetectPlatforms()
				.addPlatform(new OpenEmsRaspberryPiPlatform())
				.autoDetectProviders()
				.build();
		
		var hardware = getInstance();
		hardware.getLed(Cm4Hardware.Led.LED_1);
	}

	@Deactivate
	protected void deactivate() {
		this.pi4j.shutdown();
		this.logError(log, "Shutting down Pi4J context.");
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// No op
	}
}
