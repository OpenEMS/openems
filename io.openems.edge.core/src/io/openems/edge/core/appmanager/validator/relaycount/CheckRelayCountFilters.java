package io.openems.edge.core.appmanager.validator.relaycount;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class CheckRelayCountFilters {

	/**
	 * Creates a {@link InjectableComponentConfig} for a
	 * {@link CheckRelayCountFilter} which filters home relay contacts.
	 * 
	 * @param onlyHighVoltageRelays determines which relay channels are disabled
	 * @param deviceHardware        the {@link OpenemsAppInstance} of the device
	 *                              hardware
	 * @return the {@link InjectableComponentConfig}
	 */
	public static InjectableComponentConfig feneconHome(//
			boolean onlyHighVoltageRelays, //
			OpenemsAppInstance deviceHardware //
	) {
		var properties = createProperties(onlyHighVoltageRelays, deviceHardware);
		return new InjectableComponentConfig(HomeFilter.COMPONENT_NAME, properties);
	}

	/**
	 * Creates a {@link InjectableComponentConfig} for a
	 * {@link CheckRelayCountFilter} which filters techbase CM4S Gen 3 relay
	 * contacts.
	 *
	 * @param onlyHighVoltageRelays determines which relay channels are disabled
	 * @param deviceHardware        the {@link OpenemsAppInstance} of the device
	 *                              hardware
	 * @return the {@link InjectableComponentConfig}
	 */
	public static InjectableComponentConfig techbaseCm4sGen3(//
			boolean onlyHighVoltageRelays, //
			OpenemsAppInstance deviceHardware //
	) {
		var properties = createProperties(onlyHighVoltageRelays, deviceHardware);
		return new InjectableComponentConfig(TechbaseCm4sGen3Filter.COMPONENT_NAME, properties);
	}

	/**
	 * Creates a {@link InjectableComponentConfig} for a
	 * {@link CheckRelayCountFilter} which filters gpio relays.
	 *
	 * @return the {@link InjectableComponentConfig}
	 */
	public static InjectableComponentConfig gpio() {
		return new InjectableComponentConfig(GpioFilter.COMPONENT_NAME, emptyMap());
	}

	/**
	 * Creates a {@link InjectableComponentConfig} for a
	 * {@link CheckRelayCountFilter} which filters shelly.
	 *
	 * @return the {@link InjectableComponentConfig}
	 */
	public static InjectableComponentConfig shelly() {
		return new InjectableComponentConfig(ShellyFilter.COMPONENT_NAME, emptyMap());
	}

	private static Map<String, Object> createProperties(boolean onlyHighVoltageRelays,
			OpenemsAppInstance deviceHardware) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("onlyHighVoltageRelays", onlyHighVoltageRelays);
		properties.put("deviceHardware", deviceHardware);
		return properties;
	}

}