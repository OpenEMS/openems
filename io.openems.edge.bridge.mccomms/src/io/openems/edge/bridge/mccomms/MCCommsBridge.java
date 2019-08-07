package io.openems.edge.bridge.mccomms;

import io.openems.edge.common.channel.ChannelId;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.concurrent.LinkedBlockingQueue;


@Designate( ocd=Config.class, factory=true)
@Component(
		name="io.openems.edge.bridge.mccomms",
		immediate=true,
		configurationPolicy=ConfigurationPolicy.REQUIRE)
public class MCCommsBridge extends AbstractOpenemsComponent implements OpenemsComponent{
	
	protected MCCommsBridge(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}
	
	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
