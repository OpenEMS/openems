package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsConstants;
import io.openems.common.event.EventBuilder;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailOpenemsType;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;
import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.StateChannelDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * This Worker constantly checks if {@link EdgeConfig} was updated, e.g. because
 * configuration properties changed or Channels changed. If an update was
 * recognized, an event is announced.
 */
public class EdgeConfigWorker extends ComponentManagerWorker {

	private static final int CYCLE_TIME = 300_000; // in ms

	private static final Logger LOG = LoggerFactory.getLogger(EdgeConfigWorker.class);

	private final Logger log = LoggerFactory.getLogger(EdgeConfigWorker.class);
	private final Queue<ConfigurationEvent> events = new ArrayDeque<>();

	private EdgeConfig.ActualEdgeConfig.Builder cache = null;

	public EdgeConfigWorker(ComponentManagerImpl parent) {
		super(parent);
	}

	@Override
	protected synchronized void forever() {
		this.getEdgeConfig();
	}

	/**
	 * Gets the EdgeConfig object; updates the cache if necessary and publishes a
	 * CONFIG_UPDATE event on update.
	 *
	 * @return the {@link EdgeConfig}
	 */
	public synchronized EdgeConfig getEdgeConfig() {
		var wasConfigUpdated = false;

		if (this.cache != null) {
			// Use Cache

			// Apply ConfigurationEvents from queue
			ConfigurationEvent event;
			while ((event = this.events.poll()) != null) {
				wasConfigUpdated |= this.updateCacheFromEvent(event);
			}
			// Update Cache Channels
			this.updateChannels(this.cache);

		} else {

			// No cache
			this.cache = this.buildNewEdgeConfig();
			wasConfigUpdated = true;
		}

		var result = this.cache.buildEdgeConfig();

		if (wasConfigUpdated) {
			EventBuilder.from(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CONFIG_UPDATE) //
					.addArg(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY, result) //
					.send();
		}

		return result;
	}

	@Override
	protected int getCycleTime() {
		return CYCLE_TIME;
	}

	@Override
	public synchronized void configurationEvent(ConfigurationEvent event) {
		this.events.offer(event);
		this.triggerNextRun();
	}

	/**
	 * Update the local EdgeConfig cache from event.
	 *
	 * @param event the {@link ConfigurationEvent}
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean updateCacheFromEvent(ConfigurationEvent event) {
		if (event.getType() == ConfigurationEvent.CM_UPDATED) {
			// Update/Create: apply only changes
			var pid = event.getPid();
			return this.readConfigurations(this.cache, "(service.pid=" + pid + ")");
		}
		// Something else - e.g. delete - create full EdgeConfig
		this.cache = this.buildNewEdgeConfig();
		return true;
	}

	/**
	 * Build a new EdgeConfig without using Cache.
	 *
	 * @return the {@link EdgeConfig}
	 */
	private EdgeConfig.ActualEdgeConfig.Builder buildNewEdgeConfig() {
		var builder = EdgeConfig.ActualEdgeConfig.create();
		try {
			this.readFactories(builder);
			this.readConfigurations(builder, null /* no filter: read all */);
			this.readComponents(builder);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return builder;
	}

	/**
	 * Update EdgeConfig Channels.
	 *
	 * @param builder the {@link EdgeConfig} builder
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean updateChannels(EdgeConfig.ActualEdgeConfig.Builder builder) {
		var wasConfigUpdated = false;
		for (OpenemsComponent component : this.parent.getAllComponents()) {
			var comp = builder.getComponents().get(component.id());
			if (comp == null) {
				this.log.warn("Component [" + component.id() + "] was missing!");
				continue;
			}
			if (comp.getChannels().size() != component.channels().size()) {
				comp.setChannels(this.getChannels(component));
				wasConfigUpdated = true;
			}
		}
		return wasConfigUpdated;
	}

	/**
	 * Get the Channels for a Component.
	 *
	 * @param componentId the Component-ID
	 * @return a map of Channels; or empty map if the Component is not active
	 */
	private TreeMap<String, EdgeConfig.Component.Channel> getChannels(String componentId) {
		for (OpenemsComponent component : this.parent.getAllComponents()) {
			if (componentId.equals(component.id())) {
				return this.getChannels(component);
			}
		}
		return new TreeMap<>();
	}

	/**
	 * Get the Channels for a Component.
	 *
	 * @param component the {@link OpenemsComponent}
	 * @return a map of Channels; or empty map if the Component is not active
	 */
	private TreeMap<String, EdgeConfig.Component.Channel> getChannels(OpenemsComponent component) {
		var result = new TreeMap<String, EdgeConfig.Component.Channel>();
		if (component != null) {
			for (Channel<?> channel : component.channels()) {
				var channelId = channel.channelId();
				var doc = channelId.doc();
				ChannelDetail detail = null;
				switch (doc.getChannelCategory()) {
				case ENUM: {
					Map<String, JsonElement> values = new HashMap<>();
					var d = (EnumDoc) doc;
					for (OptionsEnum option : d.getOptions()) {
						values.put(option.getName(), new JsonPrimitive(option.getValue()));
					}
					detail = new EdgeConfig.Component.Channel.ChannelDetailEnum(values);
					break;
				}
				case OPENEMS_TYPE:
					detail = new ChannelDetailOpenemsType();
					break;
				case STATE:
					var d = (StateChannelDoc) doc;
					var level = d.getLevel();
					detail = new ChannelDetailState(level);
					break;
				}
				result.put(channelId.id(), new EdgeConfig.Component.Channel(//
						channelId.id(), //
						doc.getType(), //
						doc.getAccessMode(), //
						doc.getText(), //
						doc.getUnit(), //
						detail //
				));
			}
		}
		return result;
	}

	/**
	 * Read all existing configurations, even those that are not properly
	 * initialized.
	 *
	 * @param builder the {@link EdgeConfig} builder
	 * @param filter  the filter string for
	 *                {@link ConfigurationAdmin#listConfigurations(String)}, null
	 *                for no filter
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean readConfigurations(EdgeConfig.ActualEdgeConfig.Builder builder, String filter) {
		Configuration[] configs = null;
		try {
			configs = this.parent.cm.listConfigurations(filter);
		} catch (IOException | InvalidSyntaxException e) {
			return false;
		}

		Set<String> missingComponentIds = new HashSet<>(builder.getComponents().keySet());
		if (configs != null) {
			for (Configuration config : configs) {
				var properties = config.getProperties();
				if (properties == null) {
					this.log.warn(config.getPid() + ": Properties is 'null'");
					continue;
				}
				// Read Component-ID
				String componentId = null;
				var componentIdObj = properties.get("id");
				if (componentIdObj instanceof String) {
					// Read 'id' property
					componentId = (String) componentIdObj;

				} else {
					// Singleton
					for (OpenemsComponent component : this.parent.getAllComponents()) {
						if (config.getPid().equals(component.serviceFactoryPid())) {
							componentId = component.id();
							break;
						}
					}
				}

				if (componentId == null) {
					// Use default value for 'id' property
					var factoryPid = config.getFactoryPid();
					if (factoryPid == null) {
						continue;
					}
					var factory = builder.getFactories().get(factoryPid);
					if (factory != null) {
						var defaultValue = JsonUtils.getAsOptionalString(factory.getPropertyDefaultValue("id"));
						if (defaultValue.isPresent()) {
							componentId = defaultValue.get();
						}
					}
				}

				if (componentId == null) {
					continue;
				}

				// Remove from missingComponentIds
				missingComponentIds.remove(componentId);

				// Read Alias
				var componentAlias = componentId;
				{
					var componentAliasObj = properties.get("alias");
					if (componentAliasObj instanceof String && !((String) componentAliasObj).trim().isEmpty()) {
						componentAlias = (String) componentAliasObj;
					}
				}

				String factoryPid;
				if (config.getFactoryPid() != null) {
					// Get Factory
					factoryPid = config.getFactoryPid();
				} else {
					// Singleton Component
					factoryPid = config.getPid();
				}

				// Read Factory
				var factory = builder.getFactories().get(factoryPid);

				// Read all Properties
				var propertyMap = convertProperties(properties, factory);

				// Read all Channels
				var channels = this.getChannels(componentId);

				// Create EdgeConfig.Component and add it to Result
				builder.addComponent(componentId, new EdgeConfig.Component(config.getPid(), componentId, componentAlias,
						factoryPid, propertyMap, channels));
			}
		}

		/*
		 * Remove Components that are not anymore configured
		 */
		if (filter == null) {
			for (String missingComponentId : missingComponentIds) {
				builder.removeComponent(missingComponentId);
			}
		}
		return true;
	}

	/**
	 * Read active, properly initialized Components.
	 *
	 * @param builder the {@link EdgeConfig} builder
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean readComponents(EdgeConfig.ActualEdgeConfig.Builder builder) {
		var wasConfigUpdated = false;
		for (OpenemsComponent component : this.parent.getAllComponents()) {
			this.readComponent(builder, component);
			wasConfigUpdated = true;
		}
		return wasConfigUpdated;
	}

	/**
	 * Read this Component.
	 *
	 * @param builder   the {@link EdgeConfig} builder
	 * @param component the Component
	 */
	private void readComponent(EdgeConfig.ActualEdgeConfig.Builder builder, OpenemsComponent component) {
		var factoryPid = component.serviceFactoryPid();
		var componentId = component.id();

		// get configuration properties
		var properties = convertProperties(//
				component.getComponentContext().getProperties(), //
				builder.getFactories().get(factoryPid));

		// get Channels
		var channels = this.getChannels(component);

		var resultComponent = builder.getComponent(componentId);
		if (resultComponent.isPresent()) {
			// Update existing properties
			var resultProperties = resultComponent.get().getProperties();
			for (Entry<String, JsonElement> property : properties.entrySet()) {
				switch (property.getKey()) {
				case "org.ops4j.pax.logging.appender.name":
					// ignore
					continue;
				}
				if (!resultProperties.containsKey(property.getKey())) {
					resultProperties.put(property.getKey(), property.getValue());
				}
			}

			// Update existing Channels
			var resultChannels = resultComponent.get().getChannels();
			for (Entry<String, io.openems.common.types.EdgeConfig.Component.Channel> channel : channels.entrySet()) {
				if (!resultChannels.containsKey(channel.getKey())) {
					resultChannels.put(channel.getKey(), channel.getValue());
				}
			}

		} else {
			// Create new EdgeConfig.Component and add it to Result
			builder.addComponent(componentId, new EdgeConfig.Component(component.servicePid(), componentId,
					component.alias(), factoryPid, properties, channels));
		}
	}

	/**
	 * Read Factories.
	 *
	 * @param builder the {@link EdgeConfig} builder
	 */
	private void readFactories(EdgeConfig.ActualEdgeConfig.Builder builder) {
		var bundleContext = this.parent.bundleContext;
		if (bundleContext == null) {
			// Can be null in JUnit tests
			return;
		}
		final var bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			final var mti = this.parent.metaTypeService.getMetaTypeInformation(bundle);
			if (mti == null) {
				continue;
			}

			// read Bundle Manifest
			var manifestUrl = bundle.getResource("META-INF/MANIFEST.MF");
			Manifest manifest;
			try {
				manifest = new Manifest(manifestUrl.openStream());
			} catch (IOException e) {
				// unable to read manifest
				continue;
			}

			// get Factory-PIDs in this Bundle
			var factoryPids = mti.getFactoryPids();
			for (String factoryPid : factoryPids) {
				switch (factoryPid) {
				case "osgi.executor.provider":
					// ignore these Factory-PIDs
					break;
				default:
					// Get ObjectClassDefinition (i.e. the main annotation on the Config class)
					var objectClassDefinition = mti.getObjectClassDefinition(factoryPid, null);
					// Get Natures implemented by this Factory-PID
					var natures = this.getNatures(bundle, manifest, factoryPid);
					// Add Factory to config
					builder.addFactory(factoryPid,
							EdgeConfig.Factory.create(factoryPid, objectClassDefinition, natures));
				}
			}

			// get Singleton PIDs in this Bundle
			for (String pid : mti.getPids()) {
				switch (pid) {
				default:
					// Get ObjectClassDefinition (i.e. the main annotation on the Config class)
					var objectClassDefinition = mti.getObjectClassDefinition(pid, null);
					// Get Natures implemented by this Factory-PID
					var natures = this.getNatures(bundle, manifest, pid);
					// Add Factory to config
					builder.addFactory(pid, EdgeConfig.Factory.create(pid, objectClassDefinition, natures));
				}
			}
		}
	}

	/**
	 * Reads Natures from an XML.
	 *
	 * <pre>
	 * &lt;scr:component&gt;
	 *   &lt;service&gt;
	 *     &lt;provide interface="..."&gt;
	 *   &lt;/service&gt;
	 * &lt;/scr:component&gt;
	 * </pre>
	 *
	 * @param bundle     the {@link Bundle}
	 * @param manifest   the {@link Manifest}
	 * @param factoryPid the Factory-PID
	 * @return Natures as array of Strings
	 */
	private String[] getNatures(Bundle bundle, Manifest manifest, String factoryPid) {
		try {
			// get "Service-Component"-Entry of Manifest
			var serviceComponentsString = manifest.getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
			if (serviceComponentsString == null) {
				return new String[0];
			}
			var serviceComponents = serviceComponentsString.split(",");

			// read Service-Component XML files from OSGI-INF folder
			for (String serviceComponent : serviceComponents) {
				if (!serviceComponent.contains(factoryPid)) {
					// search for correct XML file
					continue;
				}

				var componentUrl = bundle.getResource(serviceComponent);
				var dbFactory = DocumentBuilderFactory.newInstance();
				var dBuilder = dbFactory.newDocumentBuilder();
				var doc = dBuilder.parse(componentUrl.openStream());
				doc.getDocumentElement().normalize();

				var serviceNodes = doc.getElementsByTagName("service");
				for (var i = 0; i < serviceNodes.getLength(); i++) {
					var serviceNode = serviceNodes.item(i);
					if (serviceNode.getNodeType() == Node.ELEMENT_NODE) {
						var provideNodes = serviceNode.getChildNodes();

						// Read "interface" attributes and return them
						Set<String> result = new HashSet<>();
						for (var j = 0; j < provideNodes.getLength(); j++) {
							var provideNode = provideNodes.item(j);
							var attributes = provideNode.getAttributes();
							if (attributes != null) {
								var interfaceNode = attributes.getNamedItem("interface");
								var nature = interfaceNode.getNodeValue();
								switch (nature) {
								case "org.osgi.service.event.EventHandler":
								case "org.ops4j.pax.logging.spi.PaxAppender":
									// ignore these natures;
									break;
								default:
									result.add(nature);
								}
							}
						}
						return result.toArray(new String[result.size()]);
					}
				}
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			this.log.warn("Unable to get Natures. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		return new String[0];
	}

	/**
	 * Gets a component Property as JsonElement. Uses some more techniques to find
	 * the proper type than {@link JsonUtils#getAsJsonElement(Object)}.
	 *
	 * @param properties the properties
	 * @param key        the property key
	 * @return the value as JsonElement
	 */
	private static JsonElement getPropertyAsJsonElement(Dictionary<String, Object> properties, String key) {
		var valueObj = properties.get(key);
		if (valueObj instanceof String) {
			var value = ((String) valueObj).trim();
			// find boolean
			if (value.equalsIgnoreCase("true")) {
				return new JsonPrimitive(true);
			}
			if (value.equalsIgnoreCase("false")) {
				return new JsonPrimitive(false);
			}
			// find JSON
			if (value.startsWith("{") && value.endsWith("}") /* JsonObject */
					|| value.startsWith("[") && value.endsWith("]") /* JsonObject */
			) {
				try {
					return JsonUtils.parse(value);
				} catch (OpenemsNamedException e) {
					LOG.warn(e.getMessage());
				}
			}
		}
		// fallback to JsonUtils
		return JsonUtils.getAsJsonElement(valueObj);
	}

	/**
	 * Convert properties to a String/JsonElement Map.
	 *
	 * @param properties the component properties
	 * @param factory    the {@link EdgeConfig.Factory}
	 * @return converted properties
	 */
	private static TreeMap<String, JsonElement> convertProperties(Dictionary<String, Object> properties,
			EdgeConfig.Factory factory) {
		var result = new TreeMap<String, JsonElement>();

		/*
		 * Read Factory Properties
		 */
		if (factory != null) {
			for (EdgeConfig.Factory.Property property : factory.getProperties()) {
				var key = property.getId();

				if (EdgeConfig.ignorePropertyKey(key)) {
					// Ignore this Property
					continue;
				}

				JsonElement value = null;
				if (property.isPassword()) {
					// hide Password fields
					value = new JsonPrimitive("xxx");

				} else {
					// get configured value
					value = getPropertyAsJsonElement(properties, key);

					if (value == null || value.isJsonNull()) {
						// get default value
						value = factory.getPropertyDefaultValue(key);
					}
				}

				if (value == null) {
					// fallback to JsonNull
					value = JsonNull.INSTANCE;
				}

				result.put(key, value);
			}
		}

		/*
		 * Add remaining existing properties
		 */
		var keys = properties.keys();
		while (keys.hasMoreElements()) {
			var key = keys.nextElement();
			if (result.containsKey(key)) {
				// already added
				continue;
			}

			if (EdgeConfig.ignorePropertyKey(key)) {
				// has to be ignored
				continue;
			}

			if (key.startsWith("_") || key.equals(OpenemsConstants.PROPERTY_FACTORY_PID)
					|| key.equals(OpenemsConstants.PROPERTY_PID)) {
				// starting with "_" or known property
				result.put(key, getPropertyAsJsonElement(properties, key));
			}
		}
		return result;
	}
}
