package io.openems.edge.controller.debuglog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

/**
 * This controller prints information about all available components on the
 * console.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Debug.Log", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DebugLog extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(DebugLog.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Controller.Debug.Log)))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public DebugLog() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		StringBuilder b = new StringBuilder();
		/*
		 * Asks each component for its debugLog()-ChannelIds. Prints an aggregated log
		 * of those channelIds and their current values.
		 */
		this.components.stream() //
				.filter(c -> c.isEnabled() && c.id() != null) // enabled components only
				.sorted((c1, c2) -> c1.id().compareTo(c2.id())) // sorted by Component-ID
				.forEachOrdered(component -> {
					String debugLog = component.debugLog();
					String state = component.getState().listStates();

					if (debugLog != null || !state.isEmpty()) {
						b.append(component.id() + "[");
						if (debugLog != null && !state.isEmpty()) {
							b.append(debugLog + "|State:" + state);
						} else if (debugLog != null) {
							b.append(debugLog);
						} else if (!state.isEmpty()) {
							b.append(state);
						}
						b.append("] ");
					}
				});
		this.logInfo(this.log, b.toString());
	}
}
