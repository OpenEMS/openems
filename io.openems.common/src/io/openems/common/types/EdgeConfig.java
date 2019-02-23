package io.openems.common.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig.Factory.Property;
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

		public static Factory create(ObjectClassDefinition ocd, String[] natures) {
			String name = ocd.getName();
			String description = ocd.getDescription();
			AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			List<Property> properties = new ArrayList<>();
			for (AttributeDefinition ad : ads) {
				if (ad.getID().endsWith(".target")) {
					// ignore
				} else {
					switch (ad.getID()) {
					case "webconsole.configurationFactory.nameHint":
						// ignore ID
						break;
					default:
						properties.add(Property.from(ad));
					}
				}
			}
			return new Factory(name, description, properties.toArray(new Property[properties.size()]), natures);
		}

		public static class Property {

			private final String id;
			private final String name;
			private final String description;
			private final JsonObject schema;

			public Property(String id, String name, String description, JsonObject schema) {
				this.id = id;
				this.name = name;
				this.description = description;
				this.schema = schema;
			}

			public static Property from(AttributeDefinition ad) {
				String id = ad.getID();
				String name = ad.getName();
				String description = ad.getDescription();
				if (description == null) {
					description = "";
				}
				JsonObject schema = new JsonObject();
				switch (ad.getType()) {
				case AttributeDefinition.STRING:
					schema = JsonUtils.buildJsonObject() //
							.addProperty("type", "string") //
							.build();
					break;
				case AttributeDefinition.LONG:
				case AttributeDefinition.INTEGER:
				case AttributeDefinition.SHORT:
				case AttributeDefinition.DOUBLE:
				case AttributeDefinition.FLOAT:
				case AttributeDefinition.BYTE:
				case AttributeDefinition.PASSWORD:
				case AttributeDefinition.CHARACTER:
					schema = JsonUtils.buildJsonObject() //
							.addProperty("type", "number") //
							.build();
					break;
				case AttributeDefinition.BOOLEAN:
					schema = JsonUtils.buildJsonObject() //
							.addProperty("type", "boolean") //
							.build();
					break;
				}
				return new Property(id, name, description, schema);
			}

			/**
			 * Creates a Property from JSON.
			 * 
			 * @param json the JSON
			 * @return the Property
			 * @throws OpenemsNamedException on error
			 */
			public static Property fromJson(JsonElement json) throws OpenemsNamedException {
				String id = JsonUtils.getAsString(json, "id");
				String name = JsonUtils.getAsString(json, "name");
				String description = JsonUtils.getAsString(json, "description");
				JsonObject schema = JsonUtils.getAsJsonObject(json, "schema");
				return new Property(id, name, description, schema);
			}

			/**
			 * Returns the Factory Property as a JSON Object.
			 * 
			 * <pre>
			 * {
			 *   id: string,
			 *   name: string,
			 *   description: string,
			 *   schema: {
			 *     type: string
			 *   }
			 * }
			 * </pre>
			 * 
			 * @return property as a JSON Object
			 */
			public JsonObject toJson() {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", this.id) //
						.addProperty("name", this.name) //
						.addProperty("description", this.description) //
						.add("schema", this.schema) //
						.build();
			}

		}

		private final String name;
		private String description;
		private Property[] properties;
		private final String[] natures;

		public Factory(String name, String description, Property[] properties, String[] natures) {
			this.name = name;
			this.description = description;
			this.properties = properties;
			this.natures = natures;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public Property[] getProperties() {
			return properties;
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
			JsonArray properties = new JsonArray();
			for (Property property : this.getProperties()) {
				properties.add(property.toJson());
			}
			return JsonUtils.buildJsonObject() //
					.addProperty("name", this.name) //
					.addProperty("description", this.description) //
					.add("natures", natures) //
					.add("properties", properties) //
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
			String name = JsonUtils.getAsString(json, "name");
			String description = JsonUtils.getAsString(json, "description");
			String[] natures = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(json, "natures"));
			JsonArray jProperties = JsonUtils.getAsJsonArray(json, "properties");
			Property[] properties = new Property[jProperties.size()];
			for (int i = 0; i < jProperties.size(); i++) {
				JsonElement jProperty = jProperties.get(i);
				properties[i] = Property.fromJson(jProperty);
			}
			return new Factory(name, description, properties, natures);
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
	 * Get Component-IDs of Component instances by the given Factory.
	 * 
	 * @param factoryPid the given Factory.
	 * @return a List of Component-IDs.
	 */
	public List<String> getComponentIdsByFactory(String factoryPid) {
		List<String> result = new ArrayList<>();
		for (Entry<String, Component> componentEntry : this.components.entrySet()) {
			if (factoryPid.equals(componentEntry.getValue().factoryPid)) {
				result.add(componentEntry.getKey());
			}
		}
		return result;
	}

	/**
	 * Get Component instances by the given Factory.
	 * 
	 * @param factoryPid the given Factory.
	 * @return a List of Components.
	 */
	public List<Component> getComponentsByFactory(String factoryPid) {
		List<Component> result = new ArrayList<>();
		for (Entry<String, Component> componentEntry : this.components.entrySet()) {
			if (factoryPid.equals(componentEntry.getValue().factoryPid)) {
				result.add(componentEntry.getValue());
			}
		}
		return result;
	}

	/**
	 * Get Component-IDs of Components that implement the given Nature.
	 * 
	 * @param nature the given Nature.
	 * @return a List of Component-IDs.
	 */
	public List<String> getComponentsImplementingNature(String nature) {
		List<String> result = new ArrayList<>();
		for (Entry<String, Component> componentEntry : this.components.entrySet()) {
			String factoryPid = componentEntry.getValue().factoryPid;
			Factory factory = this.factories.get(factoryPid);
			if (factory == null) {
				continue;
			}
			for (String thisNature : factory.natures) {
				if (nature.equals(thisNature)) {
					result.add(componentEntry.getKey());
					break;
				}
			}
		}
		return result;
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
		return JsonUtils.buildJsonObject() //
				.add("components", this.componentsToJson()) //
				.add("factories", this.factoriesToJson()) //
				.build();
	}

	/**
	 * Returns the configuration Components as a JSON Object.
	 * 
	 * <pre>
	 * {
	 *   {@link EdgeConfig.Component#toJson()} 
	 * }
	 * </pre>
	 * 
	 * @return Components as a JSON Object
	 */
	public JsonObject componentsToJson() {
		JsonObject components = new JsonObject();
		for (Entry<String, Component> entry : this.getComponents().entrySet()) {
			components.add(entry.getKey(), entry.getValue().toJson());
		}
		return components;
	}

	/**
	 * Returns the configuration Factories as a JSON Object.
	 * 
	 * <pre>
	 * {
	 *   [pid: string]: {
	 *     natures: string[]
	 *   }
	 * }
	 * </pre>
	 * 
	 * @return Factories as a JSON Object
	 */
	public JsonObject factoriesToJson() {
		JsonObject factories = new JsonObject();
		for (Entry<String, Factory> entry : this.getFactories().entrySet()) {
			factories.add(entry.getKey(), entry.getValue().toJson());
		}
		return factories;
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
			Map<String, JsonElement> properties = new HashMap<>();
			for (Entry<String, JsonElement> property : config.entrySet()) {
				switch (property.getKey()) {
				case "id":
				case "alias":
				case "class":
					// ignore
					break;
				default:
					if (property.getValue().isJsonPrimitive()) {
						// ignore everything but JSON-Primitives
						properties.put(property.getKey(), property.getValue());
					}
				}
			}
			result.addComponent(id, new EdgeConfig.Component(clazz, properties));
		}

		JsonObject metas = JsonUtils.getAsJsonObject(json, "meta");
		for (Entry<String, JsonElement> entry : metas.entrySet()) {
			JsonObject meta = JsonUtils.getAsJsonObject(entry.getValue());
			String pid = JsonUtils.getAsString(meta, "class");
			String[] implement = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(meta, "implements"));
			Property[] properties = new Property[0];
			result.addFactory(pid, new EdgeConfig.Factory(pid, "", properties, implement));
		}

		return result;
	}

}
