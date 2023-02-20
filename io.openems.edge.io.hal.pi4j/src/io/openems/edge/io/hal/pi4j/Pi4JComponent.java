package io.openems.edge.io.hal.pi4j;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO Rasbperry Pi Hardware Abstraction", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Pi4JComponent extends AbstractOpenemsComponent implements Pi4JInterface, OpenemsComponent {

	private Config config;
	private Context pi4j;
	private ComponentManager componentManager;
	
	public Pi4JComponent() {
		super(OpenemsComponent.ChannelId.values(),
				Pi4JInterface.ChannelId.values());
	}
	
	protected Pi4JComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Activate
	void activate(ComponentContext context, Config config, @Reference ComponentManager componentManager) throws OpenemsException {
		this.componentManager = componentManager;
		if(this.componentCount() > 0) {
			throw new OpenemsException("Component Pi4J can be loaded only once. Illegal attempt loading it twice.");
		}
		System.out.println(this.componentCount());
		System.out.print(config.alias());
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.pi4j = Pi4J.newAutoContext();
		this.pi4j.platforms().describe().print(System.out);
		this.pi4j = Pi4J.newContextBuilder()
				.noAutoDetectPlatforms()
				.addPlatform(new OpenEmsRaspberryPiPlatform())
				.autoDetectProviders()
				.build();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		System.out.println(this.componentCount());
		
	}
	
	private int componentCount() {
		var pi4JComponents = this.componentManager.getEdgeConfig()
				.getComponentsByFactory("Pi4JComponent.AllAlphabetically");
		return pi4JComponents.size();
	}

}
