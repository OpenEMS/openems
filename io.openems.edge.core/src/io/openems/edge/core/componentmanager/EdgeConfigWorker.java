package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.event.Event;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailOpenemsType;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;
import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.StateChannelDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * This Worker constantly checks if {@link EdgeConfig} was updated, e.g. because
 * configuration properties changed or Channels changed. If an update was
 * recognized, an event is announced.
 */
public class EdgeConfigWorker extends AbstractWorker {

	private final static int CYCLE_TIME = 30_000; // in ms
//	TODO private final static int CYCLE_TIME = 300_000; // in ms

	private final Logger log = LoggerFactory.getLogger(EdgeConfigWorker.class);

	private final ComponentManagerImpl parent;
	private EdgeConfig cache = null;
	private final Queue<ConfigurationEvent> events = new ArrayDeque<ConfigurationEvent>();

	public EdgeConfigWorker(ComponentManagerImpl parent) {
		this.parent = parent;
	}

	@Override
	protected synchronized void forever() {
		this.getEdgeConfig();
	}

	/**
	 * Gets the EdgeConfig object; updates the cache if necessary and publishes a
	 * CONFIG_UPDATE event on update.
	 */
	public synchronized EdgeConfig getEdgeConfig() {
		boolean wasConfigUpdated = false;

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

		if (wasConfigUpdated) {
			Map<String, Object> attachment = new HashMap<>();
			attachment.put(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY, this.cache);
			this.parent.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CONFIG_UPDATE, attachment));
		}

		return this.cache;
	}

	@Override
	protected int getCycleTime() {
		return CYCLE_TIME;
	}

	public synchronized void handleEvent(ConfigurationEvent event) {
		this.events.offer(event);
		this.triggerNextRun();
	}

	/**
	 * Update the local EdgeConfig cache from event.
	 * 
	 * @param config the {@link EdgeConfig}
	 * @param event  the {@link ConfigurationEvent}
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean updateCacheFromEvent(ConfigurationEvent event) {
		if (event.getType() == ConfigurationEvent.CM_UPDATED) {
			// Update/Create: apply only changes
			String pid = event.getPid();
			return this.readConfigurations(this.cache, "(service.pid=" + pid + ")");
		} else {
			// Something else - e.g. delete - create full EdgeConfig
			this.cache = this.buildNewEdgeConfig();
			return true;
		}
	}

	/**
	 * Build a new EdgeConfig without using Cache.
	 * 
	 * @return the {@link EdgeConfig}
	 */
	private EdgeConfig buildNewEdgeConfig() {
		EdgeConfig result = new EdgeConfig();
		this.readFactories(result);
		this.readConfigurations(result, null /* no filter: read all */);
		this.readComponents(result);
		return result;
	}

	/**
	 * Update EdgeConfig Channels.
	 * 
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean updateChannels(EdgeConfig config) {
		boolean wasConfigUpdated = false;
		for (OpenemsComponent component : this.parent.getAllComponents()) {
			EdgeConfig.Component comp = config.getComponents().get(component.id());
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
		TreeMap<String, EdgeConfig.Component.Channel> result = new TreeMap<>();
		if (component != null) {
			for (Channel<?> channel : component.channels()) {
				io.openems.edge.common.channel.ChannelId channelId = channel.channelId();
				Doc doc = channelId.doc();
				ChannelDetail detail = null;
				switch (doc.getChannelCategory()) {
				case ENUM: {
					Map<String, JsonElement> values = new HashMap<>();
					EnumDoc d = (EnumDoc) doc;
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
					StateChannelDoc d = (StateChannelDoc) doc;
					Level level = d.getLevel();
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
	 * @param result the {@link EdgeConfig}
	 * @param filter the filter string for
	 *               {@link ConfigurationAdmin#listConfigurations(String)}, null for
	 *               no filter
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean readConfigurations(EdgeConfig result, String filter) {
		Configuration[] configs = null;
		try {
			configs = this.parent.cm.listConfigurations(filter);
		} catch (IOException | InvalidSyntaxException e) {
			return false;
		}

		Set<String> missingComponentIds = new HashSet<>(result.getComponents().keySet());
		if (configs != null) {
			for (Configuration config : configs) {
				Dictionary<String, Object> properties = config.getProperties();

				// Read Component-ID
				String componentId = null;
				Object componentIdObj = properties.get("id");
				if (componentIdObj != null && (componentIdObj instanceof String)) {
					componentId = (String) componentIdObj;
				} else {
					// Singleton?
					for (OpenemsComponent component : this.parent.getAllComponents()) {
						if (config.getPid().equals(component.serviceFactoryPid())) {
							componentId = component.id();
							break;
						}
					}
				}
				if (componentId == null) {
					continue;
				}

				// Remove from missingComponentIds
				missingComponentIds.remove(componentId);

				// Read Alias
				String componentAlias = componentId;
				{
					Object componentAliasObj = properties.get("alias");
					if (componentAliasObj != null && componentAliasObj instanceof String
							&& !((String) componentAliasObj).trim().isEmpty()) {
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
				EdgeConfig.Factory factory = result.getFactories().get(factoryPid);

				// Read all Properties
				TreeMap<String, JsonElement> propertyMap = convertProperties(properties, factory);

				// Read all Channels
				TreeMap<String, EdgeConfig.Component.Channel> channels = this.getChannels(componentId);

				// Create EdgeConfig.Component and add it to Result
				result.addComponent(componentId, new EdgeConfig.Component(config.getPid(), componentId, componentAlias,
						factoryPid, propertyMap, channels));
			}
		}

		/*
		 * Remove Components that are not anymore configured
		 */
		if (filter == null) {
			for (String missingComponentId : missingComponentIds) {
				result.removeComponent(missingComponentId);
			}
		}
		return true;
	}

	/**
	 * Read active, properly initialized Components.
	 * 
	 * @param result the {@link EdgeConfig}
	 * @return true if this operation changed the {@link EdgeConfig}
	 */
	private boolean readComponents(EdgeConfig result) {
		boolean wasConfigUpdated = false;
		for (OpenemsComponent component : this.parent.getAllComponents()) {
			this.readComponent(result, component);
			wasConfigUpdated = true;
		}
		return wasConfigUpdated;
	}

	/**
	 * Read this Component.
	 * 
	 * @param result    the {@link EdgeConfig}
	 * @param component the Component
	 */
	private void readComponent(EdgeConfig result, OpenemsComponent component) {
		String factoryPid = component.serviceFactoryPid();
		String componentId = component.id();

		// get configuration properties
		TreeMap<String, JsonElement> properties = convertProperties( //
				component.getComponentContext().getProperties(), //
				result.getFactories().get(factoryPid));

		// get Channels
		TreeMap<String, io.openems.common.types.EdgeConfig.Component.Channel> channels = this.getChannels(component);

		Optional<Component> resultComponent = result.getComponent(componentId);
		if (resultComponent.isPresent()) {
			// Update existing properties
			Map<String, JsonElement> resultProperties = resultComponent.get().getProperties();
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
			Map<String, io.openems.common.types.EdgeConfig.Component.Channel> resultChannels = resultComponent.get()
					.getChannels();
			for (Entry<String, io.openems.common.types.EdgeConfig.Component.Channel> channel : channels.entrySet()) {
				if (!resultChannels.containsKey(channel.getKey())) {
					resultChannels.put(channel.getKey(), channel.getValue());
				}
			}

		} else {
			// Create new EdgeConfig.Component and add it to Result
			result.addComponent(componentId, new EdgeConfig.Component(component.servicePid(), componentId,
					component.alias(), factoryPid, properties, channels));
		}
	}

	/**
	 * Read Factories.
	 * 
	 * @param result the {@link EdgeConfig}
	 */
	private void readFactories(EdgeConfig result) {
		final Bundle[] bundles = this.parent.bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			final MetaTypeInformation mti = this.parent.metaTypeService.getMetaTypeInformation(bundle);

			// read Bundle Manifest
			URL manifestUrl = bundle.getResource("META-INF/MANIFEST.MF");
			Manifest manifest;
			try {
				manifest = new Manifest(manifestUrl.openStream());
			} catch (IOException e) {
				// unable to read manifest
				continue;
			}

			// get Factory-PIDs in this Bundle
			String[] factoryPids = mti.getFactoryPids();
			for (String factoryPid : factoryPids) {
				switch (factoryPid) {
				case "osgi.executor.provider":
					// ignore these Factory-PIDs
					break;
				default:
					// Get ObjectClassDefinition (i.e. the main annotation on the Config class)
					ObjectClassDefinition objectClassDefinition = mti.getObjectClassDefinition(factoryPid, null);
					// Get Natures implemented by this Factory-PID
					String[] natures = getNatures(bundle, manifest, factoryPid);
					// Add Factory to config
					result.addFactory(factoryPid,
							EdgeConfig.Factory.create(factoryPid, objectClassDefinition, natures));
				}
			}

			// get Singleton PIDs in this Bundle
			for (String pid : mti.getPids()) {
				switch (pid) {
				default:
					// Get ObjectClassDefinition (i.e. the main annotation on the Config class)
					ObjectClassDefinition objectClassDefinition = mti.getObjectClassDefinition(pid, null);
					// Get Natures implemented by this Factory-PID
					String[] natures = getNatures(bundle, manifest, pid);
					// Add Factory to config
					result.addFactory(pid, EdgeConfig.Factory.create(pid, objectClassDefinition, natures));
				}
			}
		}
	}

	/**
	 * Reads Natures from an XML:
	 * 
	 * <pre>
	 * <scr:component>
	 *   <service>
	 *     <provide interface="...">
	 *   </service>
	 * </scr:component>
	 * </pre>
	 * 
	 * @return
	 */
	private String[] getNatures(Bundle bundle, Manifest manifest, String factoryPid) {
		try {
			// get "Service-Component"-Entry of Manifest
			String serviceComponentsString = manifest.getMainAttributes()
					.getValue(ComponentConstants.SERVICE_COMPONENT);
			if (serviceComponentsString == null) {
				return new String[0];
			}
			String[] serviceComponents = serviceComponentsString.split(",");

			// read Service-Component XML files from OSGI-INF folder
			for (String serviceComponent : serviceComponents) {
				if (!serviceComponent.contains(factoryPid)) {
					// search for correct XML file
					continue;
				}

				URL componentUrl = bundle.getResource(serviceComponent);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(componentUrl.openStream());
				doc.getDocumentElement().normalize();

				NodeList serviceNodes = doc.getElementsByTagName("service");
				for (int i = 0; i < serviceNodes.getLength(); i++) {
					Node serviceNode = serviceNodes.item(i);
					if (serviceNode.getNodeType() == Node.ELEMENT_NODE) {
						NodeList provideNodes = serviceNode.getChildNodes();

						// Read "interface" attributes and return them
						Set<String> result = new HashSet<>();
						for (int j = 0; j < provideNodes.getLength(); j++) {
							Node provideNode = provideNodes.item(j);
							NamedNodeMap attributes = provideNode.getAttributes();
							if (attributes != null) {
								Node interfaceNode = attributes.getNamedItem("interface");
								String nature = interfaceNode.getNodeValue();
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
	 * @param value      the property key
	 * @return the value as JsonElement
	 */
	private static JsonElement getPropertyAsJsonElement(Dictionary<String, Object> properties, String key) {
		Object valueObj = properties.get(key);
		if (valueObj != null && valueObj instanceof String) {
			String value = (String) valueObj;
			// find boolean
			if (value.equalsIgnoreCase("true")) {
				return new JsonPrimitive(true);
			} else if (value.equalsIgnoreCase("false")) {
				return new JsonPrimitive(false);
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
		TreeMap<String, JsonElement> result = new TreeMap<>();
		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (!EdgeConfig.ignorePropertyKey(key)) {

				JsonElement value = getPropertyAsJsonElement(properties, key);
				if (factory != null) {
					Optional<EdgeConfig.Factory.Property> propertyOpt = factory.getProperty(key);
					if (propertyOpt.isPresent()) {
						EdgeConfig.Factory.Property property = propertyOpt.get();
						// hide Password fields
						if (property.isPassword()) {
							value = new JsonPrimitive("xxx");
						}
					}
				}

				result.put(key, value);
			}
		}
		return result;
	}
}
