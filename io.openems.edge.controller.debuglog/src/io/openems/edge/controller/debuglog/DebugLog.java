package io.openems.edge.controller.debuglog;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.DebugLog", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class DebugLog extends AbstractOpenemsComponent implements Controller {

	private final Logger log = LoggerFactory.getLogger(DebugLog.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		StringBuilder b = new StringBuilder();
		/*
		 * Asks each component for its debugLog()-ChannelIds. Prints an aggregated log
		 * of those channelIds and their current values.
		 */
		this.components.forEach(component -> {
			b.append(component.id() + " [");
			Set<io.openems.edge.common.channel.doc.ChannelId> channelIds = component.debugLog();
			int i = 0;
			for (io.openems.edge.common.channel.doc.ChannelId channelId : channelIds) {
				Channel<?> channel = component.channel(channelId);
				b.append(channel.channelId().id() + ":" + channel.getActiveValue());
				// add final comma to all but the last
				if (++i < channelIds.size()) {
					b.append(",");
				}
			}
			b.append("] ");
		});
		log.info(b.toString());
	}
}
