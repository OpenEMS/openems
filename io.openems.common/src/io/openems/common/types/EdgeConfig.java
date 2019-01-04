package io.openems.common.types;

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

		public Component(String factoryPid) {
			this.factoryPid = factoryPid;
		}

		public String getFactoryPid() {
			return factoryPid;
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
	 *   components: {
	 *     [id: string]: {
	 *       factoryPid: string
	 *     }
	 *   },
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
			Component component = entry.getValue();
			components.add(entry.getKey(), //
					JsonUtils.buildJsonObject() //
							.addProperty("factoryPid", component.getFactoryPid()) //
							.build());
		}

		JsonObject factories = new JsonObject();
		for (Entry<String, Factory> entry : this.getFactories().entrySet()) {
			Factory factory = entry.getValue();
			JsonArray natures = new JsonArray();
			for (String nature : factory.getNatures()) {
				natures.add(nature);
			}
			factories.add(entry.getKey(), //
					JsonUtils.buildJsonObject() //
							.add("natures", natures) //
							.build());
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
			JsonObject component = JsonUtils.getAsJsonObject(entry.getValue());
			result.addComponent(//
					entry.getKey(), //
					new Component(//
							JsonUtils.getAsString(component, "factoryPid") //
					));
		}

		for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(json, "factories").entrySet()) {
			JsonObject factory = JsonUtils.getAsJsonObject(entry.getValue());
			String[] natures = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(factory, "natures"));
			result.addFactory(//
					entry.getKey(), //
					new Factory(//
							natures //
					));
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
			result.addComponent(id, new EdgeConfig.Component(clazz));
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
