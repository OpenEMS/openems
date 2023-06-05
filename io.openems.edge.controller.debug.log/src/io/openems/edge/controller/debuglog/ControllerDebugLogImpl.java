package io.openems.edge.controller.debuglog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.TreeMultimap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Debug.Log", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerDebugLogImpl extends AbstractOpenemsComponent
		implements ControllerDebugLog, Controller, OpenemsComponent {

	private static final Pattern COMPONENT_ID_PATTERN = Pattern.compile("([^0-9]+)([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(ControllerDebugLogImpl.class);
	private final TreeMultimap<String, String> additionalChannels = TreeMultimap.create();
	private final Set<String> ignoreComponents = new HashSet<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Controller.Debug.Log)))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	private Config config;

	public ControllerDebugLogImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerDebugLog.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private synchronized void applyConfig(Config config) throws OpenemsNamedException {
		this.config = config;

		// Parse Additional Channels
		this.additionalChannels.clear();
		for (String channel : config.additionalChannels()) {
			if (channel.isEmpty()) {
				continue;
			}
			var c = ChannelAddress.fromString(channel);
			this.additionalChannels.put(c.getComponentId(), c.getChannelId());
		}

		// Parse Ignore Components
		this.ignoreComponents.clear();
		for (String componentId : config.ignoreComponents()) {
			if (componentId.isEmpty()) {
				continue;
			}
			this.ignoreComponents.add(componentId);
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.logInfo(this.log, this.getLogMessage());
	}

	protected String getLogMessage() {
		final List<String> result = new ArrayList<>();
		/*
		 * Asks each component for its debugLog()-ChannelIds. Builds an aggregated log
		 * message of those channelIds and their current values.
		 */
		this.components.stream() //
				.sorted((c1, c2) -> {
					var c1Matcher = COMPONENT_ID_PATTERN.matcher(c1.id());
					var c2Matcher = COMPONENT_ID_PATTERN.matcher(c2.id());
					if (c1Matcher.find() && c2Matcher.find()) {
						var c1Name = c1Matcher.group(1);
						var c1Number = Integer.parseInt(c1Matcher.group(2));
						var c2Name = c2Matcher.group(1);
						var c2Number = Integer.parseInt(c2Matcher.group(2));
						if (c1Name.equals(c2Name)) {
							// Sort by Component-ID numbers
							return Integer.compare(c1Number, c2Number);
						}
						// Sort by full Component-ID
						return c1.id().compareTo(c2.id());
					}
					// Sort by full Component-ID
					return c1.id().compareTo(c2.id());
				}) //
				.forEachOrdered(component -> {
					final List<String> logs = new ArrayList<>();

					// Should the default logs of this Component be ignored?
					if (this.ignoreComponents.stream() //
							.noneMatch(pattern -> StringUtils.matchWildcard(component.id(), pattern) >= 0)) {
						// Component Debug-Log
						var debugLog = component.debugLog();
						if (debugLog != null) {
							logs.add(debugLog);
						}

						// State
						var state = component.getStateChannel().listStates();
						if (!state.isEmpty()) {
							logs.add("State:" + state);
						}
					}

					// Additional Channels
					SortedSet<String> additionalChannels = this.additionalChannels.get(component.id());
					for (String channelId : additionalChannels) {
						Channel<?> channel = component.channel(channelId);
						logs.add(channelId + ":" + channel.value().asString());
					}

					// Any logs? Add them to the output
					if (!logs.isEmpty()) {
						var b = new StringBuilder();
						b.append(component.id()).append("[");
						if (this.config.showAlias() && !Objects.equal(component.id(), component.alias())) {
							b.append(component.alias()).append("|");
						}
						b.append(String.join("|", logs));
						b.append("]");
						result.add(b.toString());
					}
				});
		if (this.config.condensedOutput()) {
			// separate components by space; one line in total
			return String.join(" ", result);
		}
		// separate components by newline
		return String.join("\n", result);
	}
}
