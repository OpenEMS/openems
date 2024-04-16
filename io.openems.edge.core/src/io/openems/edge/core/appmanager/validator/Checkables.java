package io.openems.edge.core.appmanager.validator;

import java.util.Collections;
import java.util.TreeMap;

import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.relaycount.InjectableComponentConfig;

public final class Checkables {

	/**
	 * Creates a {@link CheckableConfig} which checks if the installed system is a
	 * Home.
	 * 
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig checkHome() {
		return empty(CheckHome.COMPONENT_NAME);
	}

	/**
	 * Creates a {@link CheckableConfig} which checks if the relay with the given
	 * name has at least the given amount of ports available.
	 * 
	 * @param io      the name of the relay or null if any relay
	 * @param count   the number of available ports
	 * @param filters additional relay filter
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig checkRelayCount(String io, int count, InjectableComponentConfig... filters) {
		return new ValidatorConfig.CheckableConfig(CheckRelayCount.COMPONENT_NAME,
				new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
						.onlyIf(io != null, t -> t.put("io", io)) //
						.put("count", count) //
						.onlyIf(filters.length != 0, t -> t.put("filter", filters)) //
						.build());
	}

	/**
	 * Creates a {@link CheckableConfig} which checks if any installed relay has at
	 * least the given amount of ports available.
	 * 
	 * @param count   the number of available ports
	 * @param filters additional relay filter
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig checkRelayCount(int count, InjectableComponentConfig... filters) {
		return checkRelayCount(null, count, filters);
	}

	/**
	 * Creates a {@link CheckableConfig} which checks if a app is installed which
	 * matches any of the given appIds.
	 * 
	 * @param appIds the apps which should not be installed
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig checkAppsNotInstalled(String... appIds) {
		return new ValidatorConfig.CheckableConfig(CheckAppsNotInstalled.COMPONENT_NAME, //
				new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
						.put("appIds", appIds) //
						.build());
	}

	private static final CheckableConfig empty(String checkableName) {
		return new CheckableConfig(checkableName, Collections.emptyMap());
	}

	private Checkables() {
	}

}
