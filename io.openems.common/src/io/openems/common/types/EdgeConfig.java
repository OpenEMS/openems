package io.openems.common.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig.Component.JsonFormat;
import io.openems.common.utils.JsonUtils;

/**
 * Holds the configuration of an Edge.
 */
public class EdgeConfig {

	private static Logger LOG = LoggerFactory.getLogger(EdgeConfig.class);

	/**
	 * Represents an instance of an OpenEMS Component.
	 */
	public static class Component {

		public static final String NO_SERVICE_PID = "NO_SERVICE_PID";

		/**
		 * Represents a Channel of an OpenEMS Component.
		 */
		public static class Channel {

			public static interface ChannelDetail {
				/**
				 * Gets the {@link ChannelCategory} of the Channel.
				 *
				 * @return the {@link ChannelCategory}
				 */
				public ChannelCategory getCategory();

				/**
				 * Gets the {@link PersistencePriority} of the Channel.
				 *
				 * @return the {@link PersistencePriority}
				 */
				public PersistencePriority getPersistencePriority();

				/**
				 * Gets the {@link ChannelDetail} as {@link JsonObject}.
				 *
				 * @return the {@link JsonObject}
				 */
				public JsonObject toJson();
			}

			/**
			 * Channel-Details for OpenemsType-Channel.
			 */
			public static class ChannelDetailOpenemsType implements ChannelDetail {

				private final PersistencePriority persistencePriority;

				public ChannelDetailOpenemsType(PersistencePriority persistencePriority) {
					this.persistencePriority = persistencePriority;
				}

				@Override
				public ChannelCategory getCategory() {
					return ChannelCategory.OPENEMS_TYPE;
				}

				@Override
				public JsonObject toJson() {
					return new JsonObject();
				}

				@Override
				public PersistencePriority getPersistencePriority() {
					return this.persistencePriority;
				}
			}

			/**
			 * Channel-Details for EnumChannel.
			 */
			public static class ChannelDetailEnum implements ChannelDetail {

				private final Map<String, JsonElement> options;
				private final PersistencePriority persistencePriority;

				public ChannelDetailEnum(Map<String, JsonElement> options, PersistencePriority persistencePriority) {
					this.options = options;
					this.persistencePriority = persistencePriority;
				}

				@Override
				public ChannelCategory getCategory() {
					return ChannelCategory.ENUM;
				}

				/**
				 * Gets a Map of all the options of an EnumChannel.
				 *
				 * @return the Map of options
				 */
				public Map<String, JsonElement> getOptions() {
					return this.options;
				}

				@Override
				public JsonObject toJson() {
					var options = new JsonObject();
					for (Entry<String, JsonElement> entry : this.options.entrySet()) {
						options.add(entry.getKey(), entry.getValue());
					}
					return JsonUtils.buildJsonObject() //
							.add("options", options) //
							.build();
				}

				@Override
				public PersistencePriority getPersistencePriority() {
					return this.persistencePriority;
				}
			}

			/**
			 * Channel-Details for StateChannel.
			 */
			public static class ChannelDetailState implements ChannelDetail {

				private final Level level;
				private final PersistencePriority persistencePriority;

				public ChannelDetailState(Level level, PersistencePriority persistencePriority) {
					this.level = level;
					this.persistencePriority = persistencePriority;
				}

				/**
				 * Gets the {@link Level} of the StateChannel.
				 *
				 * @return the {@link Level}
				 */
				public Level getLevel() {
					return this.level;
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

				@Override
				public PersistencePriority getPersistencePriority() {
					return this.persistencePriority;
				}
			}

			/**
			 * Creates a Channel from JSON.
			 *
			 * @param channelId the Channel-ID
			 * @param json      the JSON
			 * @return the Channel
			 * @throws OpenemsNamedException on error
			 */
			public static Channel fromJson(String channelId, JsonElement json) throws OpenemsNamedException {
				final var type = JsonUtils.getAsEnum(OpenemsType.class, json, "type");
				var accessModeAbbrOpt = JsonUtils.getAsOptionalString(json, "accessMode");
				var persistencePriority = JsonUtils
						.getAsOptionalEnum(PersistencePriority.class, json, "persistencePriority")
						.orElse(PersistencePriority.DEFAULT_PERSISTENCE_PRIORITY);
				var accessMode = AccessMode.READ_ONLY;
				if (accessModeAbbrOpt.isPresent()) {
					var accessModeAbbr = accessModeAbbrOpt.get();
					for (AccessMode thisAccessMode : AccessMode.values()) {
						if (accessModeAbbr.equals(thisAccessMode.getAbbreviation())) {
							accessMode = thisAccessMode;
							break;
						}
					}
				}
				var text = JsonUtils.getAsOptionalString(json, "text").orElse("");
				var unitOpt = JsonUtils.getAsOptionalString(json, "unit");
				final Unit unit;
				if (unitOpt.isPresent()) {
					unit = Unit.fromSymbolOrElse(unitOpt.get(), Unit.NONE);
				} else {
					unit = Unit.NONE;
				}
				var category = JsonUtils.getAsOptionalEnum(ChannelCategory.class, json, "category")
						.orElse(ChannelCategory.OPENEMS_TYPE);
				ChannelDetail detail = null;
				switch (category) {
				case OPENEMS_TYPE: {
					detail = new ChannelDetailOpenemsType(persistencePriority);
					break;
				}

				case ENUM: {
					Map<String, JsonElement> values = new HashMap<>();
					var optionsOpt = JsonUtils.getAsOptionalJsonObject(json, "options");
					if (optionsOpt.isPresent()) {
						for (Entry<String, JsonElement> entry : optionsOpt.get().entrySet()) {
							values.put(entry.getKey(), entry.getValue());
						}
					}
					detail = new ChannelDetailEnum(values, persistencePriority);
					break;
				}

				case STATE: {
					var level = JsonUtils.getAsEnum(Level.class, json, "level");
					detail = new ChannelDetailState(level, persistencePriority);
					break;
				}

				default:
					throw new OpenemsException("Unknown Category-Key [" + category + "]");
				}
				return new Channel(channelId, type, accessMode, text, unit, detail);
			}

			private final String id;
			private final OpenemsType type;
			private final AccessMode accessMode;
			private final String text;
			private final Unit unit;
			private final ChannelDetail detail;

			public Channel(String id, OpenemsType type, AccessMode accessMode, String text, Unit unit,
					ChannelDetail detail) {
				this.id = id;
				this.type = type;
				this.accessMode = accessMode;
				this.text = text;
				this.unit = unit;
				this.detail = detail;
			}

			/**
			 * Gets the Channel-ID.
			 *
			 * @return the Channel-ID.
			 */
			public String getId() {
				return this.id;
			}

			/**
			 * Gets the {@link OpenemsType} of the Channel.
			 *
			 * @return the {@link OpenemsType}
			 */
			public OpenemsType getType() {
				return this.type;
			}

			/**
			 * Gets the {@link AccessMode} of the Channel.
			 *
			 * @return the {@link AccessMode}
			 */
			public AccessMode getAccessMode() {
				return this.accessMode;
			}

			/**
			 * Gets the descriptive text of the Channel.
			 *
			 * @return the descriptive text
			 */
			public String getText() {
				return this.text;
			}

			/**
			 * Gets the {@link Unit} of the Channel.
			 *
			 * @return the {@link Unit}
			 */
			public Unit getUnit() {
				return this.unit;
			}

			/**
			 * Gets the specific {@link ChannelDetail} object of the Channel.
			 *
			 * @return the {@link ChannelDetail}
			 */
			public ChannelDetail getDetail() {
				return this.detail;
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
						.addProperty("text", this.text) //
						.addProperty("unit", this.unit.symbol) //
						.addProperty("category", this.detail.getCategory().name()) //
						.addProperty("persistencePriority", this.detail.getPersistencePriority()) //
						.build();
			}
		}

		private final String servicePid;
		private final String id;
		private final String alias;
		private final String factoryId;
		private final SortedMap<String, JsonElement> properties;
		private final SortedMap<String, Channel> channels;

		public Component(String servicePid, String id, String alias, String factoryId,
				SortedMap<String, JsonElement> properties, SortedMap<String, Channel> channels) {
			this.servicePid = servicePid;
			this.id = id;
			this.alias = alias;
			this.factoryId = factoryId;
			this.properties = properties;
			this.channels = channels;
		}

		/**
		 * Constructor with NO_SERVICE_PID.
		 *
		 * @param id         the Component-ID
		 * @param alias      the Alias
		 * @param factoryId  the Factory-ID
		 * @param properties the configuration properties
		 * @param channels   the channels
		 */
		public Component(String id, String alias, String factoryId, SortedMap<String, JsonElement> properties,
				SortedMap<String, Channel> channels) {
			this(NO_SERVICE_PID, id, alias, factoryId, properties, channels);
		}

		/**
		 * Constructor with NO_SERVICE_PID, properties as JsonObject and no channels.
		 *
		 * @param id         the Component-ID
		 * @param alias      the Alias
		 * @param factoryId  the Factory-ID
		 * @param properties the configuration properties
		 */
		public Component(String id, String alias, String factoryId, JsonObject properties) {
			this(NO_SERVICE_PID, id, alias, factoryId,
					properties.entrySet().stream()
							.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (o1, o2) -> o1, TreeMap::new)),
					new TreeMap<>());
		}

		/**
		 * Gets the PID of the {@link Component}.
		 *
		 * @return the PID
		 */
		public String getPid() {
			return this.servicePid;
		}

		/**
		 * Gets the ID of the {@link Component}, e.g. 'ctrlDebugLog0'.
		 *
		 * @return the Component-ID
		 */
		public String getId() {
			return this.id;
		}

		/**
		 * Gets the Alias of the {@link Component}.
		 *
		 * @return the Alias
		 */
		public String getAlias() {
			return this.alias;
		}

		/**
		 * Gets the Factory-ID of the {@link Component}, e.g. 'Controller.Debug.Log'.
		 *
		 * @return the Factory-ID
		 */
		public String getFactoryId() {
			return this.factoryId;
		}

		/**
		 * Gets the Properties of the {@link Component}.
		 *
		 * @return the Properties
		 */
		public Map<String, JsonElement> getProperties() {
			return this.properties;
		}

		/**
		 * Gets the Property with the given ID.
		 *
		 * @param propertyId the Property-ID
		 * @return the Property as {@link Optional}
		 */
		public Optional<JsonElement> getProperty(String propertyId) {
			return Optional.ofNullable(this.properties.get(propertyId));
		}

		/**
		 * Gets the Property with the given ID or throws an Exception if it does not
		 * exist.
		 *
		 * @param propertyId the Property-ID
		 * @return the Property
		 */
		public JsonElement getPropertyOrError(String propertyId) throws InvalidValueException {
			var property = this.properties.get(propertyId);
			if (property != null) {
				return property;
			}
			throw new InvalidValueException("Property with ID [" + propertyId + "] does not exist.");
		}

		/**
		 * Gets a map of {@link Channel}s of the {@link Component}.
		 *
		 * @return the map of {@link Channel}s by their Channel-IDs.
		 */
		public Map<String, Channel> getChannels() {
			return this.channels;
		}

		public void setChannels(Map<String, Channel> channels) {
			this.channels.clear();
			this.channels.putAll(channels);
		}

		/**
		 * Gets the Channels of the given {@link ChannelCategory}.
		 *
		 * @param channelCategory the {@link ChannelCategory}
		 * @return a map of {@link Channel}s
		 */
		public Map<String, Channel> getChannelsOfCategory(ChannelCategory channelCategory) {
			return this.channels.entrySet().stream()
					.filter(entry -> entry.getValue().getDetail().getCategory() == channelCategory) //
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		/**
		 * Gets the StateChannels.
		 *
		 * @return the StateChannels
		 */
		public Map<String, Channel> getStateChannels() {
			return this.getChannelsOfCategory(ChannelCategory.STATE);
		}

		/**
		 * Is the given Channel-ID a StateChannel?.
		 *
		 * @param channelId the Channel-ID
		 * @return true if it is a StateChannel
		 */
		public boolean isStateChannel(String channelId) {
			return this.channels.entrySet().stream() //
					.anyMatch(entry ->
					/* find Channel-ID */
					entry.getKey().equals(channelId)
							/* is of type StateChannel */
							&& entry.getValue().getDetail().getCategory() == ChannelCategory.STATE);
		}

		/**
		 * Get the StateChannel with the given Channel-ID.
		 *
		 * @param channelId the Channel-ID
		 * @return the Channel; or empty if the Channel does not exist or is not a
		 *         StateChannel.
		 */
		public Optional<Component.Channel> getStateChannel(String channelId) {
			return this.channels.entrySet().stream() //
					.filter(entry ->
					/* find Channel-ID */
					entry.getKey().equals(channelId)
							/* is of type StateChannel */
							&& entry.getValue().getDetail().getCategory() == ChannelCategory.STATE) //
					.map(Entry::getValue) //
					.findFirst();
		}

		/**
		 * Returns the Component configuration as a JSON Object.
		 *
		 * <pre>
		 * {
		 *   alias: string,
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
		 * @param jsonFormat the {@link JsonFormat}
		 * @return configuration as a JSON Object
		 */
		public JsonObject toJson(JsonFormat jsonFormat) {
			var properties = new JsonObject();
			for (Entry<String, JsonElement> property : this.getProperties().entrySet()) {
				properties.add(property.getKey(), property.getValue());
			}
			var result = JsonUtils.buildJsonObject() //
					.addProperty("alias", this.getAlias()) //
					.addProperty("factoryId", this.getFactoryId()) //
					.add("properties", properties); //
			switch (jsonFormat) {
			case WITHOUT_CHANNELS:
				break;

			case COMPLETE:
				var channels = new JsonObject();
				for (Entry<String, Channel> channel : this.getChannels().entrySet()) {
					channels.add(channel.getKey(), channel.getValue().toJson());
				}
				result.add("channels", channels); //
				break;
			}
			return result.build();
		}

		public enum JsonFormat {
			COMPLETE, WITHOUT_CHANNELS;
		}

		/**
		 * Creates a Component from JSON.
		 *
		 * @param componentId the Component-ID
		 * @param json        the JSON
		 * @return the Component
		 * @throws OpenemsNamedException on error
		 */
		public static Component fromJson(String componentId, JsonElement json) throws OpenemsNamedException {
			var alias = JsonUtils.getAsOptionalString(json, "alias").orElse(componentId);
			var factoryId = JsonUtils.getAsOptionalString(json, "factoryId").orElse("NO_FACTORY_ID");
			var properties = new TreeMap<String, JsonElement>();
			var jPropertiesOpt = JsonUtils.getAsOptionalJsonObject(json, "properties");
			if (jPropertiesOpt.isPresent()) {
				for (Entry<String, JsonElement> entry : jPropertiesOpt.get().entrySet()) {
					if (!ignorePropertyKey(entry.getKey())
							&& !ignoreComponentPropertyKey(componentId, entry.getKey())) {
						properties.put(entry.getKey(), entry.getValue());
					}
				}
			}
			var channels = new TreeMap<String, Channel>();
			var jChannelsOpt = JsonUtils.getAsOptionalJsonObject(json, "channels");
			if (jChannelsOpt.isPresent()) {
				for (Entry<String, JsonElement> entry : jChannelsOpt.get().entrySet()) {
					channels.put(entry.getKey(), Channel.fromJson(entry.getKey(), entry.getValue()));
				}
			}
			return new Component(//
					componentId, //
					alias, //
					factoryId, //
					properties, //
					channels);
		}
	}

	/**
	 * Represents an OpenEMS Component Factory.
	 */
	public static class Factory {

		/**
		 * Creates a {@link Factory} from an {@link ObjectClassDefinition}.
		 *
		 * @param factoryId the Factory-ID
		 * @param ocd       the {@link ObjectClassDefinition}
		 * @param natureIds the Nature-IDs
		 * @return a {@link Factory}
		 */
		public static Factory create(String factoryId, ObjectClassDefinition ocd, String[] natureIds) {
			var name = ocd.getName();
			var description = ocd.getDescription();
			List<Property> properties = new ArrayList<>(Factory.toProperties(ocd));
			return new Factory(factoryId, name, description, properties.toArray(new Property[properties.size()]),
					natureIds);
		}

		/**
		 * Parses a {@link ObjectClassDefinition} to a list of {@link Property}s.
		 *
		 * @param ocd the {@link ObjectClassDefinition}
		 * @return a list of {@link Property}s
		 */
		public static List<Property> toProperties(ObjectClassDefinition ocd) {
			List<Property> properties = new ArrayList<>();
			var ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			if (ads != null) {
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
							break;
						}
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
			private final OpenemsType type;
			private final boolean isRequired;
			private final boolean isPassword;
			private final JsonElement defaultValue;
			private final JsonObject schema;

			public Property(String id, String name, String description, OpenemsType type, boolean isRequired,
					boolean isPassword, JsonElement defaultValue, JsonObject schema) {
				this.id = id;
				this.name = name;
				this.description = description;
				this.type = type;
				this.isRequired = isRequired;
				this.isPassword = isPassword;
				this.defaultValue = defaultValue;
				this.schema = schema;
			}

			/**
			 * Creates a {@link Property} from an {@link AttributeDefinition}.
			 *
			 * @param ad the {@link AttributeDefinition}
			 * @return the {@link Property}
			 */
			public static Property from(AttributeDefinition ad) {
				var description = ad.getDescription();
				if (description == null) {
					description = "";
				}

				final OpenemsType type;
				switch (ad.getType()) {
				case AttributeDefinition.LONG:
					type = OpenemsType.LONG;
					break;
				case AttributeDefinition.INTEGER:
					type = OpenemsType.INTEGER;
					break;
				case AttributeDefinition.SHORT:
				case AttributeDefinition.BYTE:
					type = OpenemsType.SHORT;
					break;
				case AttributeDefinition.DOUBLE:
					type = OpenemsType.DOUBLE;
					break;
				case AttributeDefinition.FLOAT:
					type = OpenemsType.FLOAT;
					break;
				case AttributeDefinition.BOOLEAN:
					type = OpenemsType.BOOLEAN;
					break;
				case AttributeDefinition.STRING:
				case AttributeDefinition.CHARACTER:
				case AttributeDefinition.PASSWORD:
					type = OpenemsType.STRING;
					break;
				default:
					EdgeConfig.LOG.warn("AttributeDefinition type [" + ad.getType() + "] is unknown!");
					type = OpenemsType.STRING;
				}

				var defaultValues = ad.getDefaultValue();
				JsonElement defaultValue;
				if (defaultValues == null) {
					defaultValue = JsonNull.INSTANCE;

				} else if (ad.getCardinality() == 0) {
					// Simple Type
					if (defaultValues.length == 1) {
						defaultValue = JsonUtils.getAsJsonElement(defaultValues[0]);
					} else {
						defaultValue = new JsonPrimitive("");
					}

				} else {
					// Array Type
					var defaultValueArray = new JsonArray();
					for (String value : defaultValues) {
						defaultValueArray.add(JsonUtils.getAsJsonElement(value));
					}
					defaultValue = defaultValueArray;
				}

				JsonObject schema;
				var cardinality = Math.abs(ad.getCardinality());
				if (cardinality > 1) {
					schema = JsonUtils.buildJsonObject() //
							.addProperty("type", "repeat") //
							.add("fieldArray", Property.getSchema(ad)) //
							.build();
				} else {
					schema = Property.getSchema(ad);
				}

				boolean isPassword;
				if (ad.getType() == AttributeDefinition.PASSWORD) {
					isPassword = true;
				} else {
					isPassword = false;
				}

				var id = ad.getID();
				var name = ad.getName();
				final boolean isRequired;
				switch (id) {
				case "alias":
					// Set alias as not-required. If no alias is given it falls back to id.
					isRequired = false;
					break;
				default:
					isRequired = ad.getCardinality() == 0;
				}
				return new Property(id, name, description, type, isRequired, isPassword, defaultValue, schema);
			}

			private static JsonObject getSchema(AttributeDefinition ad) {
				var schema = new JsonObject();
				if (//
				ad.getOptionLabels() != null && ad.getOptionLabels().length > 0 //
						&& ad.getOptionValues() != null && ad.getOptionValues().length > 0) {
					// use given options for schema
					var options = new JsonArray();
					for (var i = 0; i < ad.getOptionLabels().length; i++) {
						var label = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
								ad.getOptionLabels()[i].replace("_", " _"));
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

				}
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
				var id = JsonUtils.getAsString(json, "id");
				var name = JsonUtils.getAsString(json, "name");
				var description = JsonUtils.getAsString(json, "description");
				var type = JsonUtils.getAsOptionalEnum(OpenemsType.class, json, "type").orElse(OpenemsType.STRING);
				var isRequired = JsonUtils.getAsBoolean(json, "isRequired");
				boolean isPassword = JsonUtils.getAsOptionalBoolean(json, "isPassword").orElse(false);
				var defaultValue = JsonUtils.getOptionalSubElement(json, "defaultValue").orElse(JsonNull.INSTANCE);
				var schema = JsonUtils.getAsJsonObject(json, "schema");
				return new Property(id, name, description, type, isRequired, isPassword, defaultValue, schema);
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
			 *   isPassword: boolean,
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
						.addProperty("type", this.type.toString().toLowerCase()) //
						.addProperty("isRequired", this.isRequired) //
						.addProperty("isPassword", this.isPassword) //
						.add("defaultValue", this.defaultValue) //
						.add("schema", this.schema) //
						.build();
			}

			/**
			 * Gets the ID of the {@link Property}.
			 *
			 * @return the Property-ID
			 */
			public String getId() {
				return this.id;
			}

			/**
			 * Gets the Name of the {@link Property}.
			 *
			 * @return the Name
			 */
			public String getName() {
				return this.name;
			}

			/**
			 * Gets the Description of the {@link Property}.
			 *
			 * @return the Description
			 */
			public String getDescription() {
				return this.description;
			}

			/**
			 * Gets the {@link OpenemsType} of the {@link Property}.
			 *
			 * @return the {@link OpenemsType}
			 */
			public OpenemsType getType() {
				return this.type;
			}

			/**
			 * Does this Property represent a password?.
			 *
			 * @return true if it is a password
			 */
			public boolean isPassword() {
				return this.isPassword;
			}

			/**
			 * Is this Property required?.
			 *
			 * @return true if it is required
			 */
			public boolean isRequired() {
				return this.isRequired;
			}

			/**
			 * Gets the default value of the Property.
			 *
			 * @return the default value
			 */
			public JsonElement getDefaultValue() {
				return this.defaultValue;
			}
		}

		private final String id;
		private final String name;
		private final String description;
		private final Property[] properties;
		private final String[] natureIds;

		public Factory(String id, String name, String description, Property[] properties, String[] natureIds) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.properties = properties;
			this.natureIds = natureIds;
		}

		/**
		 * Gets the ID of the {@link Factory}.
		 *
		 * @return the ID
		 */
		public String getId() {
			return this.id;
		}

		/**
		 * Gets the Name of the {@link Factory}.
		 *
		 * @return the name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Gets the Description of the {@link Factory}.
		 *
		 * @return the description
		 */
		public String getDescription() {
			return this.description;
		}

		/**
		 * Gets the {@link Property}s of the {@link Factory}.
		 *
		 * @return an array of {@link Property}s
		 */
		public Property[] getProperties() {
			return this.properties;
		}

		/**
		 * Gets the Property with the given key.
		 *
		 * @param key the property key
		 * @return the Property
		 */
		public Optional<Property> getProperty(String key) {
			for (EdgeConfig.Factory.Property property : this.properties) {
				if (property.getId().equals(key)) {
					return Optional.of(property);
				}
			}
			return Optional.empty();
		}

		/**
		 * Gets the default value of a property.
		 *
		 * @param propertyId the Property ID
		 * @return the default value as {@link JsonElement}
		 */
		public JsonElement getPropertyDefaultValue(String propertyId) {
			var property = this.getProperty(propertyId);
			if (!property.isPresent()) {
				return JsonNull.INSTANCE;
			}
			return property.get().getDefaultValue();
		}

		/**
		 * Gets the Nature-IDs of the {@link Factory}.
		 *
		 * @return the Nature-IDs
		 */
		public String[] getNatureIds() {
			return this.natureIds;
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
			var natureIds = new JsonArray();
			for (String naturId : this.getNatureIds()) {
				natureIds.add(naturId);
			}
			var properties = new JsonArray();
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
		 * @param factoryId the Factory-ID
		 * @param json      the JSON
		 * @return the Factory
		 * @throws OpenemsNamedException on error
		 */
		public static Factory fromJson(String factoryId, JsonElement json) throws OpenemsNamedException {
			// TODO Update to latest OpenEMS Edge! Remove "Optional"
			var name = JsonUtils.getAsOptionalString(json, "name").orElse("Undefined");
			var description = JsonUtils.getAsOptionalString(json, "description").orElse("");
			var natureIdsOpt = JsonUtils.getAsOptionalJsonArray(json, "natureIds");
			if (!natureIdsOpt.isPresent()) {
				natureIdsOpt = JsonUtils.getAsOptionalJsonArray(json, "natures");
			}
			var natureIds = JsonUtils.getAsStringArray(natureIdsOpt.get());
			var jPropertiesOpt = JsonUtils.getAsOptionalJsonArray(json, "properties");
			Property[] properties;
			if (jPropertiesOpt.isPresent()) {
				var jProperties = jPropertiesOpt.get();
				properties = new Property[jProperties.size()];
				for (var i = 0; i < jProperties.size(); i++) {
					var jProperty = jProperties.get(i);
					properties[i] = Property.fromJson(jProperty);
				}
			} else {
				properties = new Property[0];
			}
			return new Factory(factoryId, name, description, properties, natureIds);
		}
	}

	public static class ActualEdgeConfig {

		public static class Builder {
			private final TreeMap<String, Component> components = new TreeMap<>();
			private final TreeMap<String, Factory> factories = new TreeMap<>();

			/**
			 * Adds a {@link Component}.
			 *
			 * @param id        the Component-ID
			 * @param component the {@link Component}
			 * @return myself
			 */
			public Builder addComponent(String id, Component component) {
				this.components.put(id, component);
				return this;
			}

			/**
			 * Removes a {@link Component}.
			 *
			 * @param id the Component-ID
			 */
			public void removeComponent(String id) {
				this.components.remove(id);
			}

			/**
			 * Gets a {@link Component} by its Component-ID.
			 *
			 * @param componentId the Component-ID
			 * @return the {@link Component} as {@link Optional}
			 */
			public Optional<Component> getComponent(String componentId) {
				return Optional.ofNullable(this.components.get(componentId));
			}

			/**
			 * Gets the {@link Component}s in the builder.
			 * 
			 * @return components
			 */
			public TreeMap<String, Component> getComponents() {
				return this.components;
			}

			/**
			 * Add a Factory.
			 *
			 * @param id      the Factory-ID
			 * @param factory the {@link Factory}
			 * @return myself
			 */
			public Builder addFactory(String id, Factory factory) {
				this.factories.put(id, factory);
				return this;
			}

			/**
			 * Gets the {@link Factory}s in the builder.
			 * 
			 * @return components
			 */
			public TreeMap<String, Factory> getFactories() {
				return this.factories;
			}

			/**
			 * Builds the {@link ActualEdgeConfig}.
			 * 
			 * @return {@link ActualEdgeConfig}
			 */
			public ActualEdgeConfig build() {
				return new ActualEdgeConfig(ImmutableSortedMap.copyOf(this.getComponents()),
						ImmutableSortedMap.copyOf(this.getFactories()));
			}

			/**
			 * Builds the {@link EdgeConfig}.
			 * 
			 * @return {@link EdgeConfig}
			 */
			public EdgeConfig buildEdgeConfig() {
				return new EdgeConfig(this.build());
			}
		}

		/**
		 * Creates an empty {@link ActualEdgeConfig}.
		 * 
		 * @return {@link ActualEdgeConfig}
		 */
		public static ActualEdgeConfig empty() {
			return ActualEdgeConfig.create().build();
		}

		/**
		 * Create a {@link ActualEdgeConfig.Builder} builder.
		 * 
		 * @return a {@link Builder}
		 */
		public static ActualEdgeConfig.Builder create() {
			return new Builder();
		}

		private final ImmutableSortedMap<String, Component> components;
		private final ImmutableSortedMap<String, Factory> factories;

		private ActualEdgeConfig(ImmutableSortedMap<String, Component> components,
				ImmutableSortedMap<String, Factory> factories) {
			this.components = components;
			this.factories = factories;
		}
	}

	/**
	 * Creates an empty {@link EdgeConfig}.
	 * 
	 * @return {@link EdgeConfig}
	 */
	public static EdgeConfig empty() {
		return ActualEdgeConfig.create().buildEdgeConfig();
	}

	/**
	 * Creates an {@link EdgeConfig} from a {@link JsonObject}.
	 * 
	 * @param json the {@link JsonObject}
	 * @return {@link EdgeConfig}
	 */
	public static EdgeConfig fromJson(JsonObject json) {
		return new EdgeConfig(json);
	}

	// Either _actual or _json is always set; possibly both.
	/**
	 * Do not use directly. Use {@link #getActual()} instead.
	 */
	private volatile ActualEdgeConfig _actual = null;
	/**
	 * Do not use directly. Use {@link #toJson()} instead.
	 */
	private volatile JsonObject _json = null;

	/**
	 * Build from {@link ActualEdgeConfig} using a {@link Builder}.
	 * 
	 * @param actual the {@link ActualEdgeConfig}
	 */
	private EdgeConfig(ActualEdgeConfig actual) {
		this._actual = actual;
	}

	private EdgeConfig(JsonObject json) {
		this._json = json;
	}

	/**
	 * Gets the {@link ActualEdgeConfig}. Either by parsing it from {@link #json} or
	 * by returning from cache.
	 * 
	 * @return {@link ActualEdgeConfig}; empty on JSON parse error
	 */
	private synchronized ActualEdgeConfig getActual() {
		if (this._actual != null) {
			return this._actual; // exists in cache
		}

		var builder = ActualEdgeConfig.create();
		try {
			for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(this._json, "components").entrySet()) {
				builder.addComponent(entry.getKey(), Component.fromJson(entry.getKey(), entry.getValue()));
			}
			for (Entry<String, JsonElement> entry : JsonUtils.getAsJsonObject(this._json, "factories").entrySet()) {
				builder.addFactory(entry.getKey(), Factory.fromJson(entry.getKey(), entry.getValue()));
			}
			this._actual = builder.build();
			return this._actual;

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			return ActualEdgeConfig.empty();
		}
	}

	/**
	 * Gets a {@link Component} by its Component-ID.
	 *
	 * @param componentId the Component-ID
	 * @return the {@link Component} as {@link Optional}
	 */
	public Optional<Component> getComponent(String componentId) {
		return Optional.ofNullable(this.getActual().components.get(componentId));
	}

	/**
	 * Gets the {@link Component} or throws an Exception if it does not exist.
	 *
	 * @param componentId the Component-ID
	 * @return the {@link Component}
	 * @throws InvalidValueException on error
	 */
	public Component getComponentOrError(String componentId) throws InvalidValueException {
		var component = this.getActual().components.get(componentId);
		if (component != null) {
			return component;
		}
		throw new InvalidValueException("Component with ID [" + componentId + "] does not exist.");
	}

	/**
	 * Gets the {@link Component}s.
	 *
	 * @return the {@link Component}s
	 */
	public ImmutableSortedMap<String, Component> getComponents() {
		return this.getActual().components;
	}

	/**
	 * Gets the {@link Factory}s.
	 *
	 * @return the {@link Factory}s
	 */
	public ImmutableSortedMap<String, Factory> getFactories() {
		return this.getActual().factories;
	}

	/**
	 * Get Component-IDs of Component instances by the given Factory.
	 *
	 * @param factoryId the given Factory.
	 * @return a List of Component-IDs.
	 */
	public List<String> getComponentIdsByFactory(String factoryId) {
		List<String> result = new ArrayList<>();
		for (Entry<String, Component> componentEntry : this.getComponents().entrySet()) {
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
		for (Entry<String, Component> componentEntry : this.getComponents().entrySet()) {
			if (factoryId.equals(componentEntry.getValue().factoryId)) {
				result.add(componentEntry.getValue());
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
	public synchronized JsonObject toJson() {
		if (this._json == null) {
			this._json = JsonUtils.buildJsonObject() //
					.add("components", this.componentsToJson(JsonFormat.COMPLETE)) //
					.add("factories", this.factoriesToJson()) //
					.build();
		}
		return this._json;
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
	 * @param jsonFormat the {@link JsonFormat}
	 * @return Components as a JSON Object
	 */
	public JsonObject componentsToJson(JsonFormat jsonFormat) {
		var b = JsonUtils.buildJsonObject();
		for (Entry<String, Component> entry : this.getComponents().entrySet()) {
			b.add(entry.getKey(), entry.getValue().toJson(jsonFormat));
		}
		return b.build();
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
		var b = JsonUtils.buildJsonObject();
		for (Entry<String, Factory> entry : this.getFactories().entrySet()) {
			b.add(entry.getKey(), entry.getValue().toJson());
		}
		return b.build();
	}

	/**
	 * Get the StateChannel with the given Channel-Address.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the Channel; or empty if the Channel does not exist or is not a
	 *         StateChannel.
	 */
	public Optional<Component.Channel> getStateChannel(ChannelAddress channelAddress) {
		var component = this.getComponent(channelAddress.getComponentId());
		if (component.isEmpty()) {
			return Optional.empty();
		}
		return component.get().getStateChannel(channelAddress.getChannelId());
	}

	/**
	 * Internal Method to decide whether a configuration property should be ignored.
	 *
	 * @param key the property key
	 * @return true if it should get ignored
	 */
	public static boolean ignorePropertyKey(String key) {
		return switch (key) {
		case OpenemsConstants.PROPERTY_COMPONENT_ID, OpenemsConstants.PROPERTY_OSGI_COMPONENT_ID,
				OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME, OpenemsConstants.PROPERTY_FACTORY_PID,
				OpenemsConstants.PROPERTY_PID, "webconsole.configurationFactory.nameHint", "event.topics" ->
			true;

		default -> false;
		};
	}

	/**
	 * Internal Method to decide whether a configuration property should be ignored.
	 *
	 * @param componentId the Component-ID
	 * @param key         the property key
	 * @return true if it should get ignored
	 */
	public static boolean ignoreComponentPropertyKey(String componentId, String key) {
		return switch (componentId) {
		// Filter for _sum component
		case "_sum" -> switch (key) {
		case "productionMaxActivePower", "consumptionMaxActivePower", "gridMinActivePower", "gridMaxActivePower" ->
			true;

		default -> false;
		};

		default -> false;
		};
	}
}
