package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.StateChannel;
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
		implements ComponentManager, OpenemsComponent, JsonApi, ConfigurationListener {

	private final OsgiValidateWorker osgiValidateWorker;
	private final OutOfMemoryHeapDumpWorker outOfMemoryHeapDumpWorker;
	private final DefaultConfigurationWorker defaultConfigurationWorker;
	private final EdgeConfigFactory edgeConfigFactory;

	private BundleContext bundleContext;

	@Reference
	private MetaTypeService metaTypeService;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Core.ComponentManager)))")
	private volatile List<OpenemsComponent> enabledComponents = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(!(service.factoryPid=Core.ComponentManager))")
	private volatile List<OpenemsComponent> allComponents = new CopyOnWriteArrayList<>();

	public ComponentManagerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ComponentManager.ChannelId.values() //
		);
		this.osgiValidateWorker = new OsgiValidateWorker(this);
		this.outOfMemoryHeapDumpWorker = new OutOfMemoryHeapDumpWorker(this);
		this.defaultConfigurationWorker = new DefaultConfigurationWorker(this);
		this.edgeConfigFactory = new EdgeConfigFactory();
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext) throws OpenemsException {
		super.activate(componentContext, OpenemsConstants.COMPONENT_MANAGER_ID, "Component-Manager", true);

		this.bundleContext = bundleContext;

		// Start OSGi Validate Worker
		this.osgiValidateWorker.activate(this.id());

		// Start the Out-Of-Memory Worker
		this.outOfMemoryHeapDumpWorker.activate(this.id());

		// Start the Default-Configuration Worker
		this.defaultConfigurationWorker.activate(this.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		// Stop OSGi Validate Worker
		this.osgiValidateWorker.deactivate();

		// Stop the Out-Of-Memory Worker
		this.outOfMemoryHeapDumpWorker.deactivate();

		// Stop the Default-Configuration Worker
		this.defaultConfigurationWorker.deactivate();
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return Collections.unmodifiableList(this.enabledComponents);
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return Collections.unmodifiableList(this.allComponents);
	}

	protected StateChannel configNotActivatedChannel() {
		return this.channel(ComponentManager.ChannelId.CONFIG_NOT_ACTIVATED);
	}

	protected StateChannel defaultConfigurationFailed() {
		return this.channel(ComponentManager.ChannelId.DEFAULT_CONFIGURATION_FAILED);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
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
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.GUEST);

		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));

		case CreateComponentConfigRequest.METHOD:
			return this.handleCreateComponentConfigRequest(user, CreateComponentConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(user, UpdateComponentConfigRequest.from(request));

		case DeleteComponentConfigRequest.METHOD:
			return this.handleDeleteComponentConfigRequest(user, DeleteComponentConfigRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param user    the User
	 * @param request the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		EdgeConfig config = this.getEdgeConfig(null);
		GetEdgeConfigResponse response = new GetEdgeConfigResponse(request.getId(), config);
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 * 
	 * @param user    the User
	 * @param request the CreateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(User user,
			CreateComponentConfigRequest request) throws OpenemsNamedException {
		// Get Component-ID from Request
		String componentId = null;
		for (Property property : request.getProperties()) {
			if (property.getName().equals("id")) {
				componentId = JsonUtils.getAsString(property.getValue());
			}
		}
		if (componentId == null) {
			throw new OpenemsException("Component-ID is missing in " + request.toString());
		}

		// Check that there is currently no Component with the same ID.
		Configuration[] configs;
		try {
			configs = this.cm.listConfigurations("(id=" + componentId + ")");
		} catch (IOException | InvalidSyntaxException e) {
			throw OpenemsError.GENERIC.exception("Unable to list configurations for ID [" + componentId + "]. "
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		if (configs != null && configs.length > 0) {
			throw new OpenemsException("A Component with id [" + componentId + "] is already existing!");
		}

		Configuration config;
		try {
			config = this.cm.createFactoryConfiguration(request.getFactoryPid(), null);
		} catch (IOException e) {
			e.printStackTrace();
			throw OpenemsError.GENERIC.exception("Unable create Configuration for Factory-ID ["
					+ request.getFactoryPid() + "]. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		// Create map with configuration attributes
		Dictionary<String, Object> properties = new Hashtable<>();
		for (Property property : request.getProperties()) {
			properties.put(property.getName(), JsonUtils.getAsBestType(property.getValue()));
		}

		// Update Configuration
		try {
			this.applyConfiguration(user, config, properties);
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
			throw OpenemsError.EDGE_UNABLE_TO_CREATE_CONFIG.exception(request.getFactoryPid(), e.getMessage());
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param user    the User
	 * @param request the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest request) throws OpenemsNamedException {
		Configuration config = this.getExistingConfigForId(request.getComponentId());

		// Create map with changed configuration attributes
		Dictionary<String, Object> properties = config.getProperties();
		for (Property property : request.getProperties()) {
			// do not allow certain properties to be updated, like pid and service.pid
			if (!EdgeConfig.ignorePropertyKey(property.getName())) {
				Object value = JsonUtils.getAsBestType(property.getValue());
				if (value instanceof Object[] && ((Object[]) value).length == 0) {
					value = new String[0];
				}
				properties.put(property.getName(), value);
			}
		}

		// Update Configuration
		try {
			this.applyConfiguration(user, config, properties);
		} catch (IOException e) {
			e.printStackTrace();
			throw OpenemsError.EDGE_UNABLE_TO_APPLY_CONFIG.exception(request.getComponentId(), e.getMessage());
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a DeleteComponentConfigRequest.
	 * 
	 * @param user    the User
	 * @param request the DeleteComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(User user,
			DeleteComponentConfigRequest request) throws OpenemsNamedException {
		Configuration config = this.getExistingConfigForId(request.getComponentId());

		try {
			config.delete();
		} catch (IOException e) {
			e.printStackTrace();
			throw OpenemsError.EDGE_UNABLE_TO_DELETE_CONFIG.exception(request.getComponentId(), e.getMessage());
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Updates the Configuration from the given Properties and adds some meta
	 * information.
	 * 
	 * @param user       the User
	 * @param config     the Configuration object
	 * @param properties the properties
	 * @throws IOException on error
	 */
	private void applyConfiguration(User user, Configuration config, Dictionary<String, Object> properties)
			throws IOException {
		String lastChangeBy;
		if (user != null) {
			lastChangeBy = user.getId() + ": " + user.getName();
		} else {
			lastChangeBy = "UNDEFINED";
		}
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, lastChangeBy);
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
				LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
		config.update(properties);
	}

	/**
	 * Gets the ConfigAdmin Configuration for the OpenEMS Component with the given
	 * Component-ID.
	 * 
	 * @param componentId the Component-ID
	 * @return the Configuration
	 * @throws OpenemsNamedException on error
	 */
	protected Configuration getExistingConfigForId(String componentId) throws OpenemsNamedException {
		Configuration[] configs;
		try {
			configs = this.cm.listConfigurations("(id=" + componentId + ")");
		} catch (IOException | InvalidSyntaxException e) {
			e.printStackTrace();
			throw OpenemsError.GENERIC.exception("Unable to list configurations for ID [" + componentId + "]. "
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		// Make sure we only have one config
		if (configs == null || configs.length == 0) {
			throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
		} else if (configs.length > 1) {
			throw OpenemsError.EDGE_MULTIPLE_COMPONENTS_WITH_ID.exception(componentId);
		}
		return configs[0];
	}

	@Override
	public synchronized EdgeConfig getEdgeConfig(ConfigurationEvent event) {
		return this.edgeConfigFactory.getEdgeConfig(this.bundleContext, this.metaTypeService, this.cm,
				this.allComponents, event);
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		// trigger immediate validation on configuration event
		this.osgiValidateWorker.triggerNextRun();

		// Update EdgeConfig and send Event
	}
}
