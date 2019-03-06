package io.openems.common.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig.Factory.Property;
import io.openems.common.utils.JsonUtils;

/**
 * Holds the configuration of an Edge.
 */
public class EdgeConfig {

	private final static Logger log = LoggerFactory.getLogger(EdgeConfig.class);

	public static class Component {

		private final String factoryId;
		private final TreeMap<String, JsonElement> properties;

		public Component(String factoryId, TreeMap<String, JsonElement> properties) {
			this.factoryId = factoryId;
			this.properties = properties;
		}

		public String getFactoryId() {
			return factoryId;
		}

		public Map<String, JsonElement> getProperties() {
			return properties;
		}

		/**
		 * Returns the Component configuration as a JSON Object.
		 * 
		 * <pre>
		 * {
		 *   factoryId: string,
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
					.addProperty("factoryId", this.getFactoryId()) //
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
			TreeMap<String, JsonElement> properties = new TreeMap<>();
			for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(json, "properties").entrySet()) {
				properties.put(entry.getKey(), entry.getValue());
			}
			Optional<String> factoryIdOpt = JsonUtils.getAsOptionalString(json, "factoryId");
			if (!factoryIdOpt.isPresent()) {
				factoryIdOpt = Optional.of("Update to latest OpenEMS Edge!");
				log.warn(factoryIdOpt.get());
			}
			return new Component(//
					// TODO: Remove, once FEMS are updated to latest OpenEMS Edge
					factoryIdOpt.get(), //
					properties);
		}
	}

	public static class Factory {

		public static Factory create(ObjectClassDefinition ocd, String[] natureIds) {
			String name = ocd.getName();
			String description = ocd.getDescription();
			List<Property> properties = new ArrayList<>();
			properties.addAll(Factory.toProperties(ocd, true));
			properties.addAll(Factory.toProperties(ocd, false));
			return new Factory(name, description, properties.toArray(new Property[properties.size()]), natureIds);
		}

		private static List<Property> toProperties(ObjectClassDefinition ocd, boolean isRequired) {
			int filter;
			if (isRequired) {
				filter = ObjectClassDefinition.REQUIRED;
			} else {
				filter = ObjectClassDefinition.OPTIONAL;
			}
			List<Property> properties = new ArrayList<>();
			AttributeDefinition[] ads = ocd.getAttributeDefinitions(filter);
			for (AttributeDefinition ad : ads) {
				if (ad.getID().endsWith(".target")) {
					// ignore
				} else {
					switch (ad.getID()) {
					case "webconsole.configurationFactory.nameHint":
						// ignore ID
						break;
					default:
						properties.add(Property.from(ad, isRequired));
					}
				}
			}
			return properties;
		}

		public static class Property {

			private final String id;
			private final String name;
			private final String description;
			private final boolean isRequired;
			private final JsonElement defaultValue;
			private final JsonObject schema;

			public Property(String id, String name, String description, boolean isRequired, JsonElement defaultValue,
					JsonObject schema) {
				this.id = id;
				this.name = name;
				this.description = description;
				this.isRequired = isRequired;
				this.defaultValue = defaultValue;
				this.schema = schema;
			}

			public static Property from(AttributeDefinition ad, boolean isRequired) {
				String id = ad.getID();
				String name = ad.getName();

				String description = ad.getDescription();
				if (description == null) {
					description = "";
				}

				JsonElement defaultValue = JsonUtils.getAsJsonElement(ad.getDefaultValue());
				if ((ad.getCardinality() == 0 || ad.getCardinality() == 1) && defaultValue.isJsonArray()
						&& ((JsonArray) defaultValue).size() == 1) {
					defaultValue = ((JsonArray) defaultValue).get(0);
				}

				JsonObject schema;
				int cardinality = Math.abs(ad.getCardinality());
				if (cardinality > 1) {
					schema = JsonUtils.buildJsonObject() //
							.addProperty("type", "repeat") //
							.add("fieldArray", getSchema(ad)) //
							.build();
				} else {
					schema = getSchema(ad);
				}
				return new Property(id, name, description, isRequired, defaultValue, schema);
			}

			private static JsonObject getSchema(AttributeDefinition ad) {
				JsonObject schema = new JsonObject();
				if (ad.getOptionLabels() != null && ad.getOptionValues() != null) {
					// use given options for schema
					schema.addProperty("type", "select");
					JsonArray titleMap = new JsonArray();
					for (int i = 0; i < ad.getOptionLabels().length; i++) {
						titleMap.add(JsonUtils.buildJsonObject() //
								.addProperty("value", ad.getOptionValues()[i]) //
								.addProperty("name", ad.getOptionLabels()[i]) //
								.build());
					}
					schema.add("titleMap", titleMap);

				} else {
					// generate schema from AttributeDefinition Type
					switch (ad.getType()) {
					case AttributeDefinition.STRING:
					case AttributeDefinition.CHARACTER:
						schema = JsonUtils.buildJsonObject() //
								.addProperty("type", "input") //
								.add("templateOptions", JsonUtils.buildJsonObject() //
										.addProperty("type", "text") //
										.build()) //
								.build();
						break;
					case AttributeDefinition.LONG:
					case AttributeDefinition.INTEGER:
					case AttributeDefinition.SHORT:
					case AttributeDefinition.DOUBLE:
					case AttributeDefinition.FLOAT:
					case AttributeDefinition.BYTE:
						schema = JsonUtils.buildJsonObject() //
								.addProperty("type", "input") //
								.add("templateOptions", JsonUtils.buildJsonObject() //
										.addProperty("type", "number") //
										.build()) //
								.build();
						break;
					case AttributeDefinition.PASSWORD:
						schema = JsonUtils.buildJsonObject() //
								.addProperty("type", "input") //
								.add("templateOptions", JsonUtils.buildJsonObject() //
										.addProperty("type", "password") //
										.build()) //
								.build();
						break;
					case AttributeDefinition.BOOLEAN:
						schema = JsonUtils.buildJsonObject() //
								.addProperty("type", "toggle") //
								.build();
						break;
					}
				}
				return schema;
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
				boolean isRequired = JsonUtils.getAsBoolean(json, "isRequired");
				JsonElement defaultValue = JsonUtils.getOptionalSubElement(json, "defaultValue")
						.orElse(JsonNull.INSTANCE);
				JsonObject schema = JsonUtils.getAsJsonObject(json, "schema");
				return new Property(id, name, description, isRequired, defaultValue, schema);
			}

			/**
			 * Returns the Factory Property as a JSON Object.
			 * 
			 * <pre>
			 * {
			 *   id: string,
			 *   name: string,
			 *   description: string,
			 *   isOptional: boolean,
			 *   defaultValue: any,
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
						.addProperty("isRequired", this.isRequired) //
						.add("defaultValue", this.defaultValue) //
						.add("schema", this.schema) //
						.build();
			}

		}

		private final String name;
		private String description;
		private Property[] properties;
		private final String[] natureIds;

		public Factory(String name, String description, Property[] properties, String[] natureIds) {
			this.name = name;
			this.description = description;
			this.properties = properties;
			this.natureIds = natureIds;
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

		public String[] getNatureIds() {
			return natureIds;
		}

		/**
		 * Returns the Factory configuration as a JSON Object.
		 * 
		 * <pre>
		 * {
		 *   natureIds: string[]
		 * }
		 * </pre>
		 * 
		 * @return configuration as a JSON Object
		 */
		public JsonObject toJson() {
			JsonArray natureIds = new JsonArray();
			for (String naturId : this.getNatureIds()) {
				natureIds.add(naturId);
			}
			JsonArray properties = new JsonArray();
			for (Property property : this.getProperties()) {
				properties.add(property.toJson());
			}
			return JsonUtils.buildJsonObject() //
					.addProperty("name", this.name) //
					.addProperty("description", this.description) //
					.add("natureIds", natureIds) //
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
			// TODO Update to latest OpenEMS Edge! Remove "Optional"
			String name = JsonUtils.getAsOptionalString(json, "name").orElse("Undefined");
			String description = JsonUtils.getAsOptionalString(json, "description").orElse("");
			Optional<JsonArray> natureIdsOpt = JsonUtils.getAsOptionalJsonArray(json, "natureIds");
			if (!natureIdsOpt.isPresent()) {
				natureIdsOpt = JsonUtils.getAsOptionalJsonArray(json, "natures");
			}
			String[] natureIds = JsonUtils.getAsStringArray(natureIdsOpt.get());
			Optional<JsonArray> jPropertiesOpt = JsonUtils.getAsOptionalJsonArray(json, "properties");
			Property[] properties;
			if (jPropertiesOpt.isPresent()) {
				JsonArray jProperties = jPropertiesOpt.get();
				properties = new Property[jProperties.size()];
				for (int i = 0; i < jProperties.size(); i++) {
					JsonElement jProperty = jProperties.get(i);
					properties[i] = Property.fromJson(jProperty);
				}
			} else {
				properties = new Property[0];
			}
			return new Factory(name, description, properties, natureIds);
		}
	}

	private final TreeMap<String, Component> components = new TreeMap<>();
	private final TreeMap<String, Factory> factories = new TreeMap<>();

	public EdgeConfig() {
	}

	public void addComponent(String id, Component component) {
		this.components.put(id, component);
	}

	public void addFactory(String id, Factory factory) {
		this.factories.put(id, factory);
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
	 * @param factoryId the given Factory.
	 * @return a List of Component-IDs.
	 */
	public List<String> getComponentIdsByFactory(String factoryId) {
		List<String> result = new ArrayList<>();
		for (Entry<String, Component> componentEntry : this.components.entrySet()) {
			if (factoryId.equals(componentEntry.getValue().factoryId)) {
				result.add(componentEntry.getKey());
			}
		}
		return result;
	}

	/**
	 * Get Component instances by the given Factory.
	 * 
	 * @param factoryId the given Factory PID.
	 * @return a List of Components.
	 */
	public List<Component> getComponentsByFactory(String factoryId) {
		List<Component> result = new ArrayList<>();
		for (Entry<String, Component> componentEntry : this.components.entrySet()) {
			if (factoryId.equals(componentEntry.getValue().factoryId)) {
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
			String factoryId = componentEntry.getValue().factoryId;
			Factory factory = this.factories.get(factoryId);
			if (factory == null) {
				continue;
			}
			for (String thisNature : factory.natureIds) {
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
	 *     [: string]: {
	 *       natureIds: string[]
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
	 *   [id: string]: {
	 *     natureIds: string[]
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
			TreeMap<String, JsonElement> properties = new TreeMap<>();
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
			String id = JsonUtils.getAsString(meta, "class");
			String[] implement = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(meta, "implements"));
			Property[] properties = new Property[0];
			result.addFactory(id, new EdgeConfig.Factory(id, "", properties, implement));
		}

		return result;
	}

}
