package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
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
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailOpenemsType;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;
import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.StateChannelDoc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Static helper class to create an {@link EdgeConfig}.
 */
public class EdgeConfigFactory {

	private final static Logger log = LoggerFactory.getLogger(EdgeConfigFactory.class);

	private EdgeConfig cache = null;

	/**
	 * Build an {@link EdgeConfig}.
	 * 
	 * @param bundleContext   the {@link BundleContext}
	 * @param metaTypeService the {@link MetaTypeService}
	 * @param cm              the {@link ConfigurationAdmin}
	 * @param allComponents   a list of all active {@link OpenemsComponent}s
	 * @param event           the event, or null to not use the Cache and create new
	 *                        EdgeConfig
	 * @return an {@link EdgeConfig} instance
	 */
	public synchronized EdgeConfig getEdgeConfig(BundleContext bundleContext, MetaTypeService metaTypeService,
			ConfigurationAdmin cm, List<OpenemsComponent> allComponents, ConfigurationEvent event) {
		EdgeConfig result;

		if (event != null && this.cache != null) {
			// Use Cache
			this.updateCacheFromEvent(bundleContext, metaTypeService, cm, allComponents, event);
			result = this.cache;

		} else {
			result = buildNewEdgeConfig(bundleContext, metaTypeService, cm, allComponents, event);
			this.cache = result;
		}

		return result;
	}

	/**
	 * Update the local EdgeConfig cache from event.
	 * 
	 * @param bundleContext   the {@link BundleContext}
	 * @param metaTypeService the {@link MetaTypeService}
	 * @param cm              the {@link ConfigurationAdmin}
	 * @param allComponents   a list of all active {@link OpenemsComponent}s
	 * @param event           the event, or null to not use the Cache and create new
	 *                        EdgeConfig
	 */
	private synchronized void updateCacheFromEvent(BundleContext bundleContext, MetaTypeService metaTypeService,
			ConfigurationAdmin cm, List<OpenemsComponent> allComponents, ConfigurationEvent event) {
		String pid = event.getPid();
		for (OpenemsComponent component : allComponents) {
			if (pid.equals(component.servicePid())) {
				// Found active Component
				readComponent(this.cache, component);
				return;
			}
		}
		// No active Component; use Configuration object
		boolean wasReadConfigSuccessful = readConfigurations(this.cache, cm, "(service.pid=" + pid + ")");
		if (wasReadConfigSuccessful) {
			return;
		}

		// No success -> build from scratch without cache
		this.cache = buildNewEdgeConfig(bundleContext, metaTypeService, cm, allComponents, event);
	}

	/**
	 * Build a new EdgeConfig without using Cache.
	 * 
	 * @param bundleContext   the {@link BundleContext}
	 * @param metaTypeService the {@link MetaTypeService}
	 * @param cm              the {@link ConfigurationAdmin}
	 * @param allComponents   a list of all active {@link OpenemsComponent}s
	 * @param event           the event, or null to not use the Cache and create new
	 *                        EdgeConfig
	 * @return the {@link EdgeConfig}
	 */
	private static EdgeConfig buildNewEdgeConfig(BundleContext bundleContext, MetaTypeService metaTypeService,
			ConfigurationAdmin cm, List<OpenemsComponent> allComponents, ConfigurationEvent event) {
		EdgeConfig result = new EdgeConfig();
		readFactories(result, bundleContext, metaTypeService);
		readComponents(result, allComponents);
		readConfigurations(result, cm, null /* no filter: read all */);
		return result;
	}

	/**
	 * Read active, properly initialized Components.
	 * 
	 * @param result        the {@link EdgeConfig}
	 * @param allComponents a list of all Components
	 */
	private static void readComponents(EdgeConfig result, List<OpenemsComponent> allComponents) {
		for (OpenemsComponent component : allComponents) {
			readComponent(result, component);
		}
	}

	/**
	 * Read this Component.
	 * 
	 * @param result    the {@link EdgeConfig}
	 * @param component the Component
	 */
	private static void readComponent(EdgeConfig result, OpenemsComponent component) {
		String componentId = component.id();
		String factoryPid = component.serviceFactoryPid();

		// get configuration properties
		TreeMap<String, JsonElement> properties = convertProperties( //
				component.getComponentContext().getProperties(), //
				result.getFactories().get(factoryPid));

		// get Channels
		TreeMap<String, EdgeConfig.Component.Channel> channels = new TreeMap<>();
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
			channels.put(channelId.id(), new EdgeConfig.Component.Channel(//
					channelId.id(), //
					doc.getType(), //
					doc.getAccessMode(), //
					doc.getText(), //
					doc.getUnit(), //
					detail //
			));
		}
		// Create EdgeConfig.Component and add it to Result
		result.addComponent(componentId, new EdgeConfig.Component(component.servicePid(), componentId,
				component.alias(), factoryPid, properties, channels));
	}

	/**
	 * Read all remaining, existing configurations, even those that are not properly
	 * initialized.
	 * 
	 * @param result the {@link EdgeConfig}
	 * @param cm     the {@link ConfigurationAdmin}
	 * @param filter the filter string for
	 *               {@link ConfigurationAdmin#listConfigurations(String)}, null for
	 *               no filter
	 * @return true on success, false if no configuration has been added
	 */
	private static boolean readConfigurations(EdgeConfig result, ConfigurationAdmin cm, String filter) {
		Configuration[] configs = null;
		try {
			configs = cm.listConfigurations(filter);
		} catch (IOException | InvalidSyntaxException e) {
			return false;
		}

		if (configs == null || configs.length == 0) {
			return false;
		}

		for (Configuration config : configs) {
			Dictionary<String, Object> properties = config.getProperties();
			TreeMap<String, JsonElement> propertyMap = new TreeMap<>();

			// Read Component-ID
			Object componentIdObj = properties.get("id");
			if (componentIdObj == null || !(componentIdObj instanceof String)) {
				continue;
			}
			String componentId = (String) componentIdObj;

			// Has this Component already been added?
			if (result.getComponents().containsKey(componentId)) {
				continue;
			}

			// Read Alias
			String componentAlias = componentId;
			{
				Object componentAliasObj = properties.get("alias");
				if (componentAliasObj != null && componentAliasObj instanceof String
						&& !((String) componentAliasObj).trim().isEmpty()) {
					componentAlias = (String) componentAliasObj;
				}
			}

			// Get Factory; only if this is not a singleton Component
			String factoryPid = null;
			EdgeConfig.Factory factory = null;
			{
				Object factoryPidObj = properties.get("service.factoryPid");
				if (factoryPidObj != null && factoryPidObj instanceof String) {
					factoryPid = (String) factoryPidObj;
					factory = result.getFactories().get(factoryPid);
				}
			}

			// Read all properties
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
					propertyMap.put(key, value);
				}
			}

			// Create EdgeConfig.Component and add it to Result
			result.addComponent(componentId, new EdgeConfig.Component(config.getPid(), componentId, componentAlias,
					factoryPid, propertyMap, new TreeMap<>() /* no channels available */));
		}
		return true;
	}

	/**
	 * Read Factories.
	 * 
	 * @param result          the {@link EdgeConfig}
	 * @param bundleContext   the {@link BundleContext}
	 * @param metaTypeService the {@link MetaTypeService}
	 */
	private static void readFactories(EdgeConfig result, BundleContext bundleContext, MetaTypeService metaTypeService) {
		final Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			final MetaTypeInformation mti = metaTypeService.getMetaTypeInformation(bundle);

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
	private static String[] getNatures(Bundle bundle, Manifest manifest, String factoryPid) {
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
			log.warn("Unable to get Natures. " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
