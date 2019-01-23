package io.openems.common.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Holds the configuration of an Edge.
 */
public class EdgeConfig {

	public static class Component {

		private final String factoryPid;
		private final Map<String, JsonElement> properties;

		public Component(String factoryPid, Map<String, JsonElement> properties) {
			this.factoryPid = factoryPid;
			this.properties = properties;
		}

		public String getFactoryPid() {
			return factoryPid;
		}

		public Map<String, JsonElement> getProperties() {
			return properties;
		}

		/**
		 * Returns the Component configuration as a JSON Object.
		 * 
		 * <pre>
		 * {
		 *   factoryPid: string,
		 *	 properties: {
		 *     [key: string]: value
		 *   }
		 * }
		 * </pre>
		 * 
		 * @return configuration as a JSON Object
		 */
		public JsonObject toJson() {
			JsonObject properties = new JsonObject();
			for (Entry<String, JsonElement> property : this.getProperties().entrySet()) {
				properties.add(property.getKey(), property.getValue());
			}
			return JsonUtils.buildJsonObject() //
					.addProperty("factoryPid", this.getFactoryPid()) //
					.add("properties", properties) //
					.build();
		}

		/**
		 * Creates a Component from JSON.
		 * 
		 * @param json the JSON
		 * @return the Component
		 * @throws OpenemsNamedException on error
		 */
		public static Component fromJson(JsonElement json) throws OpenemsNamedException {
			Map<String, JsonElement> properties = new HashMap<>();
			for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(json, "properties").entrySet()) {
				properties.put(entry.getKey(), entry.getValue());
			}
			return new Component(//
					JsonUtils.getAsString(json, "factoryPid"), //
					properties);
		}
	}

	public static class Factory {

		private final String[] natures;

		public Factory(String[] natures) {
			this.natures = natures;
		}

		public String[] getNatures() {
			return natures;
		}

		/**
		 * Returns the Factory configuration as a JSON Object.
		 * 
		 * <pre>
		 * {
		 *   natures: string[]
		 * }
		 * </pre>
		 * 
		 * @return configuration as a JSON Object
		 */
		public JsonObject toJson() {
			JsonArray natures = new JsonArray();
			for (String nature : this.getNatures()) {
				natures.add(nature);
			}
			return JsonUtils.buildJsonObject() //
					.add("natures", natures) //
					.build();
		}

		/**
		 * Creates a Factory from JSON.
		 * 
		 * @param json the JSON
		 * @return the Factory
		 * @throws OpenemsNamedException on error
		 */
		public static Factory fromJson(JsonElement json) throws OpenemsNamedException {
			String[] natures = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(json, "natures"));
			return new Factory(natures);
		}

	}

	private final TreeMap<String, Component> components = new TreeMap<>();
	private final TreeMap<String, Factory> factories = new TreeMap<>();

	public EdgeConfig() {
	}

	public void addComponent(String id, Component component) {
		this.components.put(id, component);
	}

	public void addFactory(String pid, Factory factory) {
		this.factories.put(pid, factory);
	}

	public TreeMap<String, Component> getComponents() {
		return this.components;
	}

	public TreeMap<String, Factory> getFactories() {
		return factories;
	}

	/**
	 * Returns the configuration as a JSON Object.
	 * 
	 * <pre>
	 * {
	 *   components: { {@link EdgeConfig.Component#toJson()} }, 
	 *   factories: {
	 *     [pid: string]: {
	 *       natures: string[]
	 *     }
	 *   }
	 * }
	 * </pre>
	 * 
	 * @return configuration as a JSON Object
	 */
	public JsonObject toJson() {
		JsonObject components = new JsonObject();
		for (Entry<String, Component> entry : this.getComponents().entrySet()) {
			components.add(entry.getKey(), entry.getValue().toJson());
		}

		JsonObject factories = new JsonObject();
		for (Entry<String, Factory> entry : this.getFactories().entrySet()) {
			factories.add(entry.getKey(), entry.getValue().toJson());
		}

		return JsonUtils.buildJsonObject() //
				.add("components", components) //
				.add("factories", factories) //
				.build();
	}

	/**
	 * Creates an EdgeConfig from a JSON Object.
	 * 
	 * @param json the configuration in JSON format
	 * @return the EdgeConfig
	 * @throws OpenemsNamedException on error
	 */
	public static EdgeConfig fromJson(JsonObject json) throws OpenemsNamedException {
		EdgeConfig result = new EdgeConfig();
		if (json.has("things") && json.has("meta")) {
			return EdgeConfig.fromOldJsonFormat(json);
		}

		for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(json, "components").entrySet()) {
			result.addComponent(entry.getKey(), Component.fromJson(entry.getValue()));
		}

		for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(json, "factories").entrySet()) {
			result.addFactory(entry.getKey(), Factory.fromJson(entry.getValue()));
		}

		return result;
	}

	@Deprecated
	private static EdgeConfig fromOldJsonFormat(JsonObject json) throws OpenemsNamedException {
		EdgeConfig result = new EdgeConfig();

		JsonObject things = JsonUtils.getAsJsonObject(json, "things");
		for (Entry<String, JsonElement> entry : things.entrySet()) {
			JsonObject config = JsonUtils.getAsJsonObject(entry.getValue());
			String id = JsonUtils.getAsString(config, "id");
			String clazz = JsonUtils.getAsString(config, "class");
			result.addComponent(id, new EdgeConfig.Component(clazz, new HashMap<>() /* no properties */));
		}

		JsonObject metas = JsonUtils.getAsJsonObject(json, "meta");
		for (Entry<String, JsonElement> entry : metas.entrySet()) {
			JsonObject meta = JsonUtils.getAsJsonObject(entry.getValue());
			String pid = JsonUtils.getAsString(meta, "class");
			String[] implement = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(meta, "implements"));
			result.addFactory(pid, new EdgeConfig.Factory(implement));
		}

		return result;
	}

}
