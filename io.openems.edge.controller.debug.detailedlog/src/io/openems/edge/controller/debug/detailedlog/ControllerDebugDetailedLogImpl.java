package io.openems.edge.controller.debug.detailedlog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

/**
 * This controller prints all channels and their values on the console.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Debug.DetailedLog", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
public class ControllerDebugDetailedLogImpl extends AbstractOpenemsComponent
		implements ControllerDebugDetailedLog, Controller, OpenemsComponent {

	private static final int WIDTH_FIRST = 30;

	private final Logger log = LoggerFactory.getLogger(ControllerDebugDetailedLogImpl.class);
	private final Set<String> finishedFirstRun = new HashSet<>();
	private final Map<ChannelAddress, String> lastPrinted = new HashMap<>();

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public ControllerDebugDetailedLogImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerDebugDetailedLog.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		for (String componentId : this.config.component_ids()) {
			var component = this.componentManager.getComponent(componentId);
			var printedHeader = false;

			if (!this.finishedFirstRun.contains(component.id())) {
				/*
				 * Print on first run
				 */
				this.logInfo(this.log, "=======================================");
				this.log("ID", component.id());
				this.log("Service-PID", component.servicePid());
				this.log("Implementation", reducePackageName(component.getClass()));
				getInheritanceViaReflection(component.getClass(), null).asMap().forEach((inheritance, names) -> {
					var first = true;
					for (String name : names) {
						if (first) {
							this.log(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, inheritance.name()), name);
						} else {
							this.log("", name);
						}
						first = false;
					}
				});
				this.finishedFirstRun.add(component.id());
				printedHeader = true;
			}

			final Map<ChannelAddress, String> shouldPrint = new HashMap<>();
			component.channels().stream() //
					.sorted((c1, c2) -> c1.channelId().name().compareTo(c2.channelId().name())) //
					.forEach(channel -> {
						var unit = channel.channelDoc().getUnit().symbol;
						/*
						 * create descriptive text
						 */
						var channelText = "";
						switch (channel.channelDoc().getAccessMode()) {
						case READ_ONLY:
						case READ_WRITE:
							var description = "";
							if (channel instanceof EnumReadChannel) {
								try {
									description += channel.value().asOptionString();
								} catch (IllegalArgumentException e) {
									description += "UNKNOWN OPTION VALUE [" + channel.value().asString() + "]";
									description += "ERROR: " + e.getMessage();
								}
							}
							if (channel instanceof StateChannel
									&& ((StateChannel) channel).value().orElse(false) == true) {
								if (!description.isEmpty()) {
									description += "; ";
								}
								description += ((StateChannel) channel).channelDoc().getText();
							}
							if (channel instanceof StateCollectorChannel
									&& ((StateCollectorChannel) channel).value().orElse(0) != 0) {
								if (!description.isEmpty()) {
									description += "; ";
								}
								description += ((StateCollectorChannel) channel).listStates();
							}
							channelText = String.format("%15s %-3s %s", //
									channel.value().asStringWithoutUnit(), //
									unit, //
									description.isEmpty() ? "" : "(" + description + ")");
							break;

						case WRITE_ONLY:
							channelText += "WRITE_ONLY";
						}
						// Build complete line
						var line = String.format("%-" + WIDTH_FIRST + "s : %s", channel.channelId().id(), channelText);
						// Print the line only if is not equal to the last printed line
						if (!this.lastPrinted.containsKey(channel.address())
								|| !this.lastPrinted.get(channel.address()).equals(line)) {
							shouldPrint.put(channel.address(), line);
						}
						// Add line to last printed lines
						this.lastPrinted.put(channel.address(), line);
					});

			if (!shouldPrint.isEmpty()) {
				if (!printedHeader) {
					/*
					 * Print header (this is not the first run)
					 */
					this.logInfo(this.log, "=======================================");
					this.log("ID", component.id());
				}

				this.logInfo(this.log, "---------------------------------------");
				shouldPrint.values().stream().sorted().forEach(line -> {
					this.logInfo(this.log, line);
				});
				this.logInfo(this.log, "---------------------------------------");
			}
		}
	}

	private enum Inheritance {
		EXTEND, IMPLEMENT;
	}

	private static Multimap<Inheritance, String> getInheritanceViaReflection(Class<?> clazz,
			Multimap<Inheritance, String> map) {
		if (map == null) {
			map = HashMultimap.create();
		}
		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && !superClazz.equals(Object.class)) {
			map.put(Inheritance.EXTEND, reducePackageName(superClazz));
			getInheritanceViaReflection(superClazz, map);
		}
		for (Class<?> iface : clazz.getInterfaces()) {
			map.put(Inheritance.IMPLEMENT, reducePackageName(iface));
			getInheritanceViaReflection(iface, map);
		}
		return map;
	}

	private void log(String topic, String message) {
		this.logInfo(this.log, String.format("%-" + WIDTH_FIRST + "s : %s", topic, message));
	}

	private static String reducePackageName(Class<?> clazz) {
		return reducePackageName(clazz.getName());
	}

	private static String reducePackageName(String name) {
		if (name.startsWith("io.openems.edge.")) {
			return name.substring(16);
		}
		return name;
	}
}
