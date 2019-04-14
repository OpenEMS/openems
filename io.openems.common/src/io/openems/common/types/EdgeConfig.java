package io.openems.common.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Holds the configuration of an Edge.
 */
public class EdgeConfig {

	/**
	 * Represents an instance of an OpenEMS Component.
	 */
	public static class Component {

		/**
		 * Represents a Channel of an OpenEMS Component.
		 */
		public static class Channel {

			public static interface ChannelDetail {
				ChannelCategory getCategory();

				JsonObject toJson();
			}

			/**
			 * Channel-Details for OpenemsType-Channel.
			 */
			public static class ChannelDetailOpenemsType implements ChannelDetail {

				public ChannelDetailOpenemsType() {
				}

				@Override
				public ChannelCategory getCategory() {
					return ChannelCategory.OPENEMS_TYPE;
				}

				@Override
				public JsonObject toJson() {
					return new JsonObject();
				}
			}

			/**
			 * Channel-Details for EnumChannel.
			 */
			public static class ChannelDetailEnum implements ChannelDetail {

				private final Map<String, JsonElement> options;

				public ChannelDetailEnum(Map<String, JsonElement> options) {
					this.options = options;
				}

				@Override
				public ChannelCategory getCategory() {
					return ChannelCategory.ENUM;
				}

				public Map<String, JsonElement> getOptions() {
					return options;
				}

				@Override
				public JsonObject toJson() {
					JsonObject options = new JsonObject();
					for (Entry<String, JsonElement> entry : this.options.entrySet()) {
						options.add(entry.getKey(), entry.getValue());
					}
					return JsonUtils.buildJsonObject() //
							.add("options", options) //
							.build();
				}
			}

			/**
			 * Channel-Details for StateChannel.
			 */
			public static class ChannelDetailState implements ChannelDetail {

				private final Level level;

				public ChannelDetailState(Level level) {
					this.level = level;
				}

				@Override
				public ChannelCategory getCategory() {
					return ChannelCategory.STATE;
				}

				@Override
				public JsonObject toJson() {
					return JsonUtils.buildJsonObject() //
							.addProperty("level", this.level.name()) //
							.build();
				}
			}

			/**
			 * Creates a Channel from JSON.
			 * 
			 * @param json the JSON
			 * @return the Channel
			 * @throws OpenemsNamedException on error
			 */
			public static Channel fromJson(JsonElement json) throws OpenemsNamedException {
				OpenemsType type = JsonUtils.getAsEnum(OpenemsType.class, json, "type");
				AccessMode accessMode = JsonUtils.getAsEnum(AccessMode.class, json, "accessMode");
				Unit unit = JsonUtils.getAsEnum(Unit.class, json, "unit");
				ChannelCategory categoryKey = JsonUtils.getAsEnum(ChannelCategory.class, json, "categoryKey");
				JsonObject categoryDetails = JsonUtils.getAsJsonObject(json, "categoryDetails");
				ChannelDetail detail = null;
				switch (categoryKey) {
				case OPENEMS_TYPE: {
					detail = new ChannelDetailOpenemsType();
					break;
				}

				case ENUM: {
					Map<String, JsonElement> values = new HashMap<>();
					for (Entry<String, JsonElement> entry : categoryDetails.entrySet()) {
						values.put(entry.getKey(), entry.getValue());
					}
					detail = new ChannelDetailEnum(values);
					break;
				}

				case STATE: {
					Level level = JsonUtils.getAsEnum(Level.class, categoryDetails, "level");
					detail = new ChannelDetailState(level);
					break;
				}

				default:
					throw new OpenemsException("Unknown Category-Key [" + categoryKey + "]");
				}
				return new Channel(type, accessMode, unit, detail);
			}

			private final OpenemsType type;
			private final AccessMode accessMode;
			private final Unit unit;
			private final ChannelDetail detail;

			public Channel(OpenemsType type, AccessMode accessMode, Unit unit, ChannelDetail detail) {
				this.type = type;
				this.accessMode = accessMode;
				this.unit = unit;
				this.detail = detail;
			}

			public OpenemsType getType() {
				return type;
			}

			public AccessMode getAccessMode() {
				return accessMode;
			}

			public Unit getUnit() {
				return unit;
			}

			public ChannelDetail getDetail() {
				return detail;
			}

			/**
			 * Gets the JSON representation of this Channel.
			 * 
			 * @return a JsonObject
			 */
			public JsonObject toJson() {
				return JsonUtils.buildJsonObject(this.detail.toJson()) //
						.addProperty("type", this.type.name()) //
						.addProperty("accessMode", this.accessMode.getAbbreviation()) //
						.addProperty("unit", this.unit.getSymbol()) //
						.addProperty("category", this.detail.getCategory().name()) //
						.build();
			}
		}

		private final String factoryId;
		private final TreeMap<String, JsonElement> properties;
		private final TreeMap<String, Channel> channels;

		public Component(String factoryId, TreeMap<String, JsonElement> properties, TreeMap<String, Channel> channels) {
			this.factoryId = factoryId;
			this.properties = properties;
			this.channels = channels;
		}

		public String getFactoryId() {
			return factoryId;
		}

		public Map<String, JsonElement> getProperties() {
			return properties;
		}

		public Map<String, Channel> getChannels() {
			return channels;
		}

		/**
		 * Returns the Component configuration as a JSON Object.
		 * 
		 * <pre>
		 * {
		 *   factoryId: string,
		 *	 properties: {
		 *     [key: string]: value
		 *   },
		 *   channels: {
		 *     [channelId: string]: {}
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
			JsonObject channels = new JsonObject();
			for (Entry<String, Channel> channel : this.getChannels().entrySet()) {
				channels.add(channel.getKey(), channel.getValue().toJson());
			}
			return JsonUtils.buildJsonObject() //
					.addProperty("factoryId", this.getFactoryId()) //
					.add("properties", properties) //
					.add("channels", channels) //
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
			TreeMap<String, Channel> channels = new TreeMap<>();
			for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(json, "channels").entrySet()) {
				channels.put(entry.getKey(), Channel.fromJson(entry.getValue()));
			}
			return new Component(//
					JsonUtils.getAsString(json, "factoryId"), //
					properties, //
					channels);
		}
	}

	/**
	 * Represents an OpenEMS Component Factory.
	 */
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

		/**
		 * Represents a configuration option of an OpenEMS Component Factory.
		 */
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
					JsonArray options = new JsonArray();
					for (int i = 0; i < ad.getOptionLabels().length; i++) {
						String label = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
								ad.getOptionLabels()[i].replaceAll("_", " _"));
						options.add(JsonUtils.buildJsonObject() //
								.addProperty("value", ad.getOptionValues()[i]) //
								.addProperty("label", label) //
								.build());
					}
					return JsonUtils.buildJsonObject() //
							.addProperty("type", "select") //
							.add("templateOptions", JsonUtils.buildJsonObject() //
									.add("options", options) //
									.build()) //
							.build();

				} else {
					// generate schema from AttributeDefinition Type
					switch (ad.getType()) {
					case AttributeDefinition.STRING:
					case AttributeDefinition.CHARACTER:
						return JsonUtils.buildJsonObject() //
								.addProperty("type", "input") //
								.add("templateOptions", JsonUtils.buildJsonObject() //
										.addProperty("type", "text") //
										.build()) //
								.build();

					case AttributeDefinition.LONG:
					case AttributeDefinition.INTEGER:
					case AttributeDefinition.SHORT:
					case AttributeDefinition.DOUBLE:
					case AttributeDefinition.FLOAT:
					case AttributeDefinition.BYTE:
						return JsonUtils.buildJsonObject() //
								.addProperty("type", "input") //
								.add("templateOptions", JsonUtils.buildJsonObject() //
										.addProperty("type", "number") //
										.build()) //
								.build();

					case AttributeDefinition.PASSWORD:
						return JsonUtils.buildJsonObject() //
								.addProperty("type", "input") //
								.add("templateOptions", JsonUtils.buildJsonObject() //
										.addProperty("type", "password") //
										.build()) //
								.build();

					case AttributeDefinition.BOOLEAN:
						return JsonUtils.buildJsonObject() //
								.addProperty("type", "toggle") //
								.build();
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
			String name = JsonUtils.getAsString(json, "name");
			String description = JsonUtils.getAsString(json, "description");
			String[] natureIds = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(json, "natureIds"));
			JsonArray jProperties = JsonUtils.getAsJsonArray(json, "properties");
			Property[] properties = new Property[jProperties.size()];
			for (int i = 0; i < jProperties.size(); i++) {
				JsonElement jProperty = jProperties.get(i);
				properties[i] = Property.fromJson(jProperty);
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
			TreeMap<String, Component.Channel> channels = new TreeMap<>();
			result.addComponent(id, new EdgeConfig.Component(clazz, properties, channels));
		}

		JsonObject metas = JsonUtils.getAsJsonObject(json, "meta");
		for (Entry<String, JsonElement> entry : metas.entrySet()) {
			JsonObject meta = JsonUtils.getAsJsonObject(entry.getValue());
			String id = JsonUtils.getAsString(meta, "class");
			String[] implement = JsonUtils.getAsStringArray(JsonUtils.getAsJsonArray(meta, "implements"));
			Factory.Property[] properties = new Factory.Property[0];
			result.addFactory(id, new EdgeConfig.Factory(id, "", properties, implement));
		}

		return result;
	}

}
