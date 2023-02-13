package io.openems.edge.io.hal.pi4j;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.io.openems.edge.io.hal.pi4j", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Pi4JComponent extends AbstractOpenemsComponent implements Pi4JInterface, OpenemsComponent {

	private Config config = null;
	private Context pi4j;
	
	public Pi4JComponent() {
		super(OpenemsComponent.ChannelId.values(),
				Pi4JInterface.ChannelId.values());
	}
	
	protected Pi4JComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.pi4j = Pi4J.newAutoContext();
		this.pi4j.platforms().describe().print(System.out);
		this.pi4j = Pi4J.newContextBuilder()
				.noAutoDetectPlatforms()
				.addPlatform(new OpenEmsRaspberryPiPlatform())
				.autoDetectProviders()
				.build();
//		System.out.println(pi4j.platforms().getAll());
//		context. .platforms().describe().print(System.out);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// TODO Auto-generated method stub
		
	}

}
