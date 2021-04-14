package io.openems.edge.controller.api.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.service.cm.Configuration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.edge.common.component.OpenemsComponent;

public class Utils {

	/**
	 * Gets Meta information about active components.
	 * 
	 * @param components the Components
	 * @return a JsonObject in the form
	 * 
	 *         <pre>
	 * {
	 *   "Controller.Symmetric.Balancing": {
	 *     "implements":[ "Controller" ]
	 *   },
	 *   "Simulator.EssSymmetric.Reacting": {
	 *     "implements":[ "Ess", "SymmetricEss" ]
	 *   }
	 * }
	 *         </pre>
	 */
	protected static JsonObject getComponentsMeta(List<OpenemsComponent> components) {
		JsonObject j = new JsonObject();
		components.stream() //
				// sort by Component ID
				.sorted((c1, c2) -> c1.id().compareTo(c2.id())) //
				.forEach(component -> {
					JsonObject jComponent = new JsonObject();

					JsonArray jImplements = new JsonArray();
					Arrays.stream(component.getClass().getInterfaces()) //
							// filter interesting Interfaces
							.filter(iface -> !(iface.equals(OpenemsComponent.class))
									&& OpenemsComponent.class.isAssignableFrom(iface))
							// sort by SimpleName
							.sorted((i1, i2) -> i1.getSimpleName().compareTo(i2.getSimpleName())) //
							.forEach(iface -> {
								jImplements.add(iface.getSimpleName());
							});
					jComponent.add("implements", jImplements);

					j.add(component.getComponentContext().getProperties().get("component.name").toString(), jComponent);
				});
		return j;
	}

	/**
	 * Converts the configuration of components to Json.
	 * 
	 * @param configs the Configurations
	 * @return a JsonObject in the form
	 * 
	 *         <pre>
	 * {
	 *   "ess0": {
	 *     "enabled": "true",
	 *     "modbus.id": "modbus0",
	 *     "service.factoryPid": "Ess.Fenecon.Commercial40",
	 *     "service.pid": "Ess.Fenecon.Commercial40.bcd5e8da-33c8-4258-ade5-480b5c0bbd2e"
	 * }
	 *         </pre>
	 */
	protected static JsonObject getComponents(Configuration[] configs) {
		JsonObject j = new JsonObject();
		for (Configuration config : configs) {
			Dictionary<String, Object> properties = config.getProperties();
			if (properties == null) {
				continue;
			}
			String id = (String) properties.get("id");
			if (id == null) {
				continue;
			}
			JsonObject jComponent = new JsonObject();
			Collections.list(properties.keys()).stream() //
					// filter interesting config properties
					.filter(key -> !key.equals("id") && !key.endsWith(".target")) //
					// sort by key (property name)
					.sorted() //
					.forEach(key -> {
						Object obj = properties.get(key);
						if (obj instanceof String[]) {
							jComponent.addProperty(key, String.join(",", (String[]) obj));
						} else {
							jComponent.add(key, toJson(obj));
						}
					});

			j.add(id, jComponent);
		}
		return j;
	}

	/**
	 * Converts an object to a JsonPrimitive.
	 * 
	 * @param value the object
	 * @return the JsonPrimitive
	 */
	private static JsonPrimitive toJson(Object value) {
		if (value instanceof Number) {
			return new JsonPrimitive((Number) value);
		} else if (value instanceof Boolean) {
			return new JsonPrimitive((Boolean) value);
		} else {
			return new JsonPrimitive(value.toString());
		}
	}
}
