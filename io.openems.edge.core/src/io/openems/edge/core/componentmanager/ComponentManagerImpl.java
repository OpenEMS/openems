package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

@Component( //
		name = "Core.ComponentManager", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.COMPONENT_MANAGER_ID, //
				"enabled=true" //
		})
public class ComponentManagerImpl extends AbstractOpenemsComponent
		implements ComponentManager, OpenemsComponent, JsonApi {

	private final Logger log = LoggerFactory.getLogger(ComponentManagerImpl.class);

	private final OsgiValidateWorker osgiValidateWorker;

	private BundleContext bundleContext;

	@Reference
	private MetaTypeService metaTypeService;

	@Reference
	protected ConfigurationAdmin cm;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		CONFIG_NOT_ACTIVATED(new Doc() //
				.text("A configured OpenEMS Component was not activated") //
				.type(OpenemsType.BOOLEAN) //
				.level(Level.WARNING));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Core.ComponentManager)))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public ComponentManagerImpl() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));

		this.osgiValidateWorker = new OsgiValidateWorker(this);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext, Map<String, Object> properties)
			throws OpenemsException {
		super.activate(componentContext, properties, OpenemsConstants.COMPONENT_MANAGER_ID, true);

		this.bundleContext = bundleContext;

		// Start OSGi Validate Worker
		this.osgiValidateWorker.activate(this.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		// Stop OSGi Validate Worker
		this.osgiValidateWorker.deactivate();
	}

	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponent(String componentId) {
		List<OpenemsComponent> components = this.components;
		for (OpenemsComponent component : components) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw new IllegalArgumentException("Component [" + componentId + "] is not available.");
	}

	@Override
	public <T extends Channel<?>> T getChannel(ChannelAddress channelAddress) throws IllegalArgumentException {
		OpenemsComponent component = this.getComponent(channelAddress.getComponentId());
		return component.channel(channelAddress.getChannelId());
	}

	protected StateChannel configNotActivatedChannel() {
		return this.channel(ChannelId.CONFIG_NOT_ACTIVATED);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public JsonrpcResponseSuccess handleJsonrpcRequest(JsonrpcRequest request) throws OpenemsNamedException {
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(GetEdgeConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(UpdateComponentConfigRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param request the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private JsonrpcResponseSuccess handleGetEdgeConfigRequest(GetEdgeConfigRequest request)
			throws OpenemsNamedException {
		EdgeConfig config = this.getEdgeConfig();
		GetEdgeConfigResponse response = new GetEdgeConfigResponse(request.getId(), config);
		return response;
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param request the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private JsonrpcResponseSuccess handleUpdateComponentConfigRequest(UpdateComponentConfigRequest request)
			throws OpenemsNamedException {
		Configuration config = this.getConfigForId(request.getComponentId());

		// Create map with changed configuration attributes
		Hashtable<String, Object> attributes = new Hashtable<>();
		for (UpdateComponentConfigRequest.Update update : request.getUpdate()) {
			// TODO do not allow certain properties, like pid and service.pid
			attributes.put(update.getProperty(), JsonUtils.getAsBestType(update.getValue()));
		}

		// Update Configuration
		try {
			config.update(attributes);
		} catch (IOException e) {
			e.printStackTrace();
			throw OpenemsError.EDGE_UNABLE_TO_APPLY_CONFIG.exception(request.getComponentId(), e.getMessage());
		}

		return new GenericJsonrpcResponseSuccess(request.getId());
	}

	/**
	 * Gets the ConfigAdmin Configuration for the OpenEMS Component with the given
	 * Component-ID.
	 * 
	 * @param componentId the Component-ID
	 * @return the Configuration
	 * @throws OpenemsNamedException on error
	 */
	private Configuration getConfigForId(String componentId) throws OpenemsNamedException {
		Configuration[] configs;
		try {
			configs = this.cm.listConfigurations("(id=" + componentId + ")");
		} catch (IOException | InvalidSyntaxException e) {
			e.printStackTrace();
			throw OpenemsError.GENERIC.exception("Unable to list configurations for ID [" + componentId + "]. "
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		// Make sure we only have one config
		if (configs.length == 0) {
			throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
		} else if (configs.length > 1) {
			throw OpenemsError.EDGE_MULTIPLE_COMPONENTS_WITH_ID.exception(componentId);
		}
		return configs[0];
	}

	@Override
	public EdgeConfig getEdgeConfig() {
		EdgeConfig config = new EdgeConfig();
		final List<OpenemsComponent> components = this.components;
		for (OpenemsComponent component : components) {
			config.addComponent(component.id(), new EdgeConfig.Component(component.factoryPid()));
		}

		final Bundle[] bundles = this.bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			final MetaTypeInformation mti = this.metaTypeService.getMetaTypeInformation(bundle);

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
					// Get Natures implemented by this Factory-PID
					String[] natures = this.getNatures(bundle, manifest, factoryPid);
					// Add Factory to config
					config.addFactory(factoryPid, new EdgeConfig.Factory(natures));
				}
			}
		}
		return config;
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
			this.logWarn(this.log, "Unable to get Natures. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		return new String[0];
	}
}
