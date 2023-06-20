package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.google.gson.JsonNull;

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
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsxRequest;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsxResponse;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = ComponentManager.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class ComponentManagerImpl extends AbstractOpenemsComponent
		implements ComponentManager, OpenemsComponent, JsonApi, ConfigurationListener {

	private final List<ComponentManagerWorker> workers = new ArrayList<>();
	private final EdgeConfigWorker edgeConfigWorker;

	protected BundleContext bundleContext;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private volatile ClockProvider clockProvider = null;

	@Reference
	protected MetaTypeService metaTypeService;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected EventAdmin eventAdmin;

	@Reference
	protected ServiceComponentRuntime serviceComponentRuntime;

	public ComponentManagerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ComponentManager.ChannelId.values() //
		);
		this.workers.add(new OsgiValidateWorker(this));
		this.workers.add(new OutOfMemoryHeapDumpWorker(this));
		this.workers.add(new DefaultConfigurationWorker(this));
		this.workers.add(this.edgeConfigWorker = new EdgeConfigWorker(this));
	}

	@Activate
	private void activate(ComponentContext componentContext, BundleContext bundleContext) throws OpenemsException {
		super.activate(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.bundleContext = bundleContext;

		for (ComponentManagerWorker worker : this.workers) {
			worker.activate(this.id());
		}

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext componentContext, BundleContext bundleContext) {
		super.modified(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.bundleContext = bundleContext;

		for (ComponentManagerWorker worker : this.workers) {
			worker.modified(this.id());
		}

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();

		for (ComponentManagerWorker worker : this.workers) {
			worker.deactivate();
		}
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return this.getComponentsViaService("(&(enabled=true)(!(service.factoryPid=Core.ComponentManager)))");
	}

	@Override
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz) {
		return this.getComponentsViaService(clazz, "(enabled=true)");
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return this.getComponentsViaService("(!(service.factoryPid=" + ComponentManager.SINGLETON_SERVICE_PID + "))");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		var component = this.getComponentViaService(componentId, true);
		if (component != null) {
			return (T) component;
		}
		throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getPossiblyDisabledComponent(String componentId)
			throws OpenemsNamedException {
		var component = this.getComponentViaService(componentId);
		if (component != null) {
			return (T) component;
		}
		throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
	}

	/**
	 * Gets the components via OSGi service reference.
	 *
	 * @param filter the filter for the components
	 * @return the components matching the filter
	 */
	private List<OpenemsComponent> getComponentsViaService(String filter) {
		return this.getComponentsViaService(OpenemsComponent.class, filter);
	}

	/**
	 * Gets the components via OSGi service reference.
	 * 
	 * @param <T>    the class type
	 * @param clazz  The class under whose name the service was registered. Must not
	 *               be {@code null}.
	 * @param filter the filter for the components
	 * @return the components matching the filter
	 */
	private <T> List<T> getComponentsViaService(Class<T> clazz, String filter) {
		if (this.bundleContext == null) {
			// Can be null in JUnit tests
			return Collections.emptyList();
		}

		try {
			var serviceReferences = this.bundleContext.getServiceReferences(clazz, filter);

			var allComponents = new ArrayList<T>(serviceReferences.size());
			for (var reference : serviceReferences) {
				var component = this.bundleContext.getService(reference);
				if (component == null) {
					continue;
				}
				allComponents.add(component);
				this.bundleContext.ungetService(reference);
			}
			return allComponents;

		} catch (InvalidSyntaxException e) {
			// filter invalid
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	/**
	 * Gets the component via OSGi service reference. Be careful, that the Component
	 * might not be 'enabled'.
	 *
	 * @param <T>         the type of the component
	 * @param componentId the id of the component
	 * @return the component or null if not found
	 */
	private <T extends OpenemsComponent> T getComponentViaService(String componentId) {
		return this.getComponentViaService(componentId, false);
	}

	/**
	 * Gets the component via OSGi service reference.
	 *
	 * @param <T>            the type of the component
	 * @param componentId    the id of the component
	 * @param hasToBeEnabled if the component has to be enabled
	 * @return the component or null if not found
	 */
	@SuppressWarnings("unchecked")
	private <T extends OpenemsComponent> T getComponentViaService(String componentId, boolean hasToBeEnabled) {
		var filter = "(id=" + componentId + ")";
		if (hasToBeEnabled) {
			filter = "(&(enabled=true)" + filter + ")";
		}
		var components = this.getComponentsViaService(filter);
		if (components.isEmpty()) {
			return null;
		}
		return (T) components.get(0);
	}

	@Override
	public String debugLog() {
		final List<String> logs = new ArrayList<>();
		for (ComponentManagerWorker worker : this.workers) {
			var message = worker.debugLog();
			if (message != null) {
				logs.add(message);
			}
		}
		if (logs.isEmpty()) {
			return null;
		}
		return String.join("|", logs);
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

		case ChannelExportXlsxRequest.METHOD:
			return this.handleChannelExportXlsxRequest(user, ChannelExportXlsxRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a {@link GetEdgeConfigRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetEdgeConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		var config = this.getEdgeConfig();
		var response = new GetEdgeConfigResponse(request.getId(), config);
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a {@link CreateComponentConfigRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link CreateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(User user,
			CreateComponentConfigRequest request) throws OpenemsNamedException {
		// Get Component-ID from Request
		String componentId = null;
		for (Property property : request.getProperties()) {
			if (property.getName().equals("id")) {
				componentId = JsonUtils.getAsString(property.getValue());
			}
		}

		Configuration config;
		if (componentId != null) {
			// Normal OpenEMS Component with ID.
			// Check that there is currently no Component with the same ID.
			List<Configuration> configs;
			try {
				configs = this.listConfigurations(componentId);
			} catch (IOException | InvalidSyntaxException e) {
				throw OpenemsError.GENERIC.exception("Unable to list configurations for ID [" + componentId + "]. "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			if (!configs.isEmpty()) {
				throw new OpenemsException("A Component with id [" + componentId + "] is already existing!");
			}
			try {
				config = this.cm.createFactoryConfiguration(request.getFactoryPid(), null);
			} catch (IOException e) {
				e.printStackTrace();
				throw OpenemsError.GENERIC.exception("Unable create Configuration for Factory-ID ["
						+ request.getFactoryPid() + "]. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}

		} else {
			// Singleton?
			try {
				config = this.cm.getConfiguration(request.getFactoryPid(), null);
			} catch (IOException e) {
				e.printStackTrace();
				throw OpenemsError.GENERIC.exception("Unable to get Configurations for Factory-PID ["
						+ request.getFactoryPid() + "]. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			if (config.getProperties() != null) {
				throw new OpenemsException(
						"A Singleton Component for PID [" + request.getFactoryPid() + "] is already existing!");
			}
		}

		// Create map with configuration attributes
		Dictionary<String, Object> properties = new Hashtable<>();
		for (Property property : request.getProperties()) {
			var value = JsonUtils.getAsBestType(property.getValue());
			if (value instanceof Object[] && ((Object[]) value).length == 0) {
				value = new String[0];
			}
			properties.put(property.getName(), value);
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
	 * Handles a {@link UpdateComponentConfigRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link UpdateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest request) throws OpenemsNamedException {
		var config = this.getExistingConfigForId(request.getComponentId());

		// Create map with changed configuration attributes
		var properties = config.getProperties();
		if (properties == null) {
			throw OpenemsError.EDGE_UNABLE_TO_APPLY_CONFIG.exception(request.getComponentId(),
					config.getPid() + ": Properties is 'null'");
		}

		// Reset all target properties to avoid missing old references
		for (var k = properties.keys(); k.hasMoreElements();) {
			var property = k.nextElement();
			if (property.endsWith(".target")) {
				properties.put(property, "(enabled=true)");
			}
		}

		for (Property property : request.getProperties()) {
			// do not allow certain properties to be updated, like pid and service.pid
			if (!EdgeConfig.ignorePropertyKey(property.getName())) {
				var jValue = property.getValue();
				if (jValue == null || jValue == JsonNull.INSTANCE) {
					// Remove NULL property
					properties.remove(property.getName());
				} else {
					// Add updated Property
					var value = JsonUtils.getAsBestType(property.getValue());
					if (value instanceof Object[] && ((Object[]) value).length == 0) {
						value = new String[0];
					}
					properties.put(property.getName(), value);
				}
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
	 * Handles a {@link DeleteComponentConfigRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link DeleteComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(User user,
			DeleteComponentConfigRequest request) throws OpenemsNamedException {
		var config = this.getExistingConfigForId(request.getComponentId());

		try {
			config.delete();
		} catch (IOException e) {
			e.printStackTrace();
			throw OpenemsError.EDGE_UNABLE_TO_DELETE_CONFIG.exception(request.getComponentId(), e.getMessage());
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link ChannelExportXlsxRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link ChannelExportXlsxRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<JsonrpcResponseSuccess> handleChannelExportXlsxRequest(User user,
			ChannelExportXlsxRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("ChannelExportXlsxRequest", Role.ADMIN);
		var component = this.getComponent(request.getComponentId());
		if (component == null) {
			throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(request.getComponentId());
		}
		return CompletableFuture.completedFuture(new ChannelExportXlsxResponse(request.getId(), component));
	}

	/**
	 * Updates the Configuration from the given Properties and adds some meta
	 * information.
	 *
	 * @param user       the {@link User}
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
		List<Configuration> configs;
		try {
			configs = this.listConfigurations(componentId);
		} catch (IOException | InvalidSyntaxException e) {
			e.printStackTrace();
			throw OpenemsError.GENERIC.exception("Unable to list configurations for ID [" + componentId + "]. "
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		if (configs.isEmpty()) {
			// Maybe this is a Singleton?
			var factoryPid = this.getComponent(componentId).serviceFactoryPid();
			try {
				return this.cm.getConfiguration(factoryPid, null);
			} catch (IOException e) {
				e.printStackTrace();
				throw OpenemsError.GENERIC.exception(
						"Unable to get Singleton-Component Configuration for ID [" + componentId + "], Factory-PID ["
								+ factoryPid + "]. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}

		// Make sure we only have one config
		if (configs.size() > 1) {
			throw OpenemsError.EDGE_MULTIPLE_COMPONENTS_WITH_ID.exception(componentId);
		}
		return configs.get(0);
	}

	/**
	 * Extends the ConfigurationAdmin 'listConfigurations' method by additionally
	 * searching through Factory default values.
	 *
	 * @param componentId the Component-ID
	 * @return an array of Configurations
	 * @throws InvalidSyntaxException on error
	 * @throws IOException            on error
	 */
	private List<Configuration> listConfigurations(String componentId) throws IOException, InvalidSyntaxException {
		List<Configuration> result = new ArrayList<>();
		var configs = this.cm.listConfigurations(null);
		if (configs == null) {
			return result;
		}

		for (Configuration config : configs) {
			var id = config.getProperties().get("id");
			if (id != null) {
				// Configuration has an 'id' property
				if (id instanceof String && componentId.equals(id)) {
					// 'id' property matches
					result.add(config);
				}

			} else {
				// compare default value for property 'id'
				var factoryPid = config.getFactoryPid();
				if (factoryPid == null) {
					// Singleton?
					factoryPid = config.getPid();
					if (factoryPid == null) {
						continue;
					}
				}
				var factory = this.getEdgeConfig().getFactories().get(factoryPid);
				if (factory == null) {
					continue;
				}
				var defaultValue = JsonUtils.getAsOptionalString(factory.getPropertyDefaultValue("id"));
				if (defaultValue.isPresent() && componentId.equals(defaultValue.get())) {
					result.add(config);
				}
			}
		}
		return result;
	}

	@Override
	public synchronized EdgeConfig getEdgeConfig() {
		return this.edgeConfigWorker.getEdgeConfig();
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		for (ComponentManagerWorker worker : this.workers) {
			worker.configurationEvent(event);
		}
	}

	@Override
	public Clock getClock() {
		var clockProvider = this.clockProvider;
		if (clockProvider != null) {
			return clockProvider.getClock();
		}
		return Clock.systemDefaultZone();
	}

}
