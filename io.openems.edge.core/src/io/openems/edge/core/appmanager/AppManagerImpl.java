package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetApp;
import io.openems.edge.core.appmanager.jsonrpc.GetAppAssistant;
import io.openems.edge.core.appmanager.jsonrpc.GetAppInstances;
import io.openems.edge.core.appmanager.jsonrpc.GetApps;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

@Designate(ocd = Config.class, factory = false)
@org.osgi.service.component.annotations.Component(//
		name = AppManager.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class AppManagerImpl extends AbstractOpenemsComponent
		implements AppManager, OpenemsComponent, JsonApi, ConfigurationListener {

	private final AppValidateWorker worker;

	@Reference
	private ConfigurationAdmin cm;
	@Reference
	protected List<OpenemsApp> availableApps;

	@Reference
	protected ComponentManager componentManager;

	protected ComponentUtil componentUtil;

	protected final List<OpenemsAppInstance> instantiatedApps = new ArrayList<>();

	public AppManagerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AppManager.ChannelId.values() //
		);
		this.worker = new AppValidateWorker(this);
	}

	@Activate
	private void activate(ComponentContext componentContext, Config config) {
		super.activate(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}

		this.componentUtil = new ComponentUtil(this.componentManager);
		this.worker.activate(this.id());
		this.applyConfig(config);
	}

	/**
	 * formats the given apps into a JSON array string.
	 *
	 * @param apps that should be formated
	 * @return formated apps string
	 */
	private static String getJsonAppsString(List<OpenemsAppInstance> apps) {
		var appsProperty = JsonUtils.buildJsonArray();
		for (var app : apps) {
			appsProperty.add(app.toJsonObject());
		}
		return JsonUtils.prettyToString(appsProperty.build());
	}

	/**
	 * Parses the configured apps to a List of {@link OpenemsAppInstance}s.
	 *
	 * @param apps the app configuration from Config.json as {@link JsonArray}
	 * @return List of {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on parse error
	 */
	private static List<OpenemsAppInstance> parseInstantiatedApps(JsonArray apps) throws OpenemsNamedException {
		var errors = new ArrayList<String>();
		var result = new ArrayList<OpenemsAppInstance>(apps.size());
		for (var appElement : apps) {
			var json = JsonUtils.getAsJsonObject(appElement);
			var appId = JsonUtils.getAsString(json, "appId");
			var instanceId = JsonUtils.getAsUUID(json, "instanceId");
			var alias = JsonUtils.getAsString(json, "alias");
			var properties = JsonUtils.getAsJsonObject(json, "properties");
			if (result.stream().anyMatch(t -> t.instanceId.equals(instanceId))) {
				errors.add("App with ID[" + instanceId + "] already exists!");
				continue;
			}
			result.add(new OpenemsAppInstance(appId, alias, instanceId, properties));
		}
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
		return result;
	}

	private synchronized void applyConfig(Config config) {
		var apps = config.apps();
		if (apps.isBlank()) {
			apps = "[]"; // default to empty array
		}
		try {
			var instApps = parseInstantiatedApps(JsonUtils.parseToJsonArray(apps));

			// always replace old apps with the new ones
			this.instantiatedApps.clear();
			this.instantiatedApps.addAll(instApps);

			this._setWrongAppConfiguration(false);

		} catch (OpenemsNamedException e) {
			this._setWrongAppConfiguration(true);
			e.printStackTrace();
			return;
		}
	}

	private void checkStatus(OpenemsApp openemsApp) throws OpenemsNamedException {
		var validator = openemsApp.getValidator();
		var status = validator.getStatus();
		switch (validator.getStatus()) {
		case INCOMPATIBLE:
			throw new OpenemsException("App is not compatible! "
					+ validator.getErrorCompatibleMessages().stream().collect(Collectors.joining(";")));
		case COMPATIBLE:
			throw new OpenemsException("App can not be installed! "
					+ validator.getErrorCompatibleMessages().stream().collect(Collectors.joining(";")));
		case INSTALLABLE:
			// continue
			break;
		default:
			throw new OpenemsException("Status '" + status.name() + "' is not implemented.");
		}
	}

	private void checkCardinality(OpenemsApp openemsApp) throws OpenemsNamedException {
		switch (openemsApp.getCardinality()) {
		case SINGLE:
			if (this.instantiatedApps.stream().anyMatch(t -> t.appId.equals(openemsApp.getAppId()))) {
				// only create one instance of this app
				throw new OpenemsException("An instance of the app[" + openemsApp.getAppId() + "] is already created!");
			}
			break;
		case SINGLE_IN_CATEGORY:
			if (this.instantiatedApps.stream().anyMatch(t -> {
				var app = this.findAppById(t.appId);
				if (app.getCardinality() != OpenemsAppCardinality.SINGLE_IN_CATEGORY) {
					return false;
				}
				return Arrays.stream(app.getCategorys())
						.anyMatch(c -> Arrays.stream(openemsApp.getCategorys()).anyMatch(c2 -> c == c2));
			})) {
				// only create one instance with the same category of this app
				throw new OpenemsException("An instance of an app with the same category["
						+ openemsApp.getCategorys().toString() + "] is already created!");
			}
			break;
		case MULTIPLE:
			// any number of this app can be instantiated
			break;
		default:
			throw new NotImplementedException("Usage " + openemsApp.getCardinality().name() + " is not implemented.");
		}
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		this.worker.configurationEvent(event);
	}

	private void createComponent(User user, Component comp) throws OpenemsNamedException {
		List<Property> properties = comp.getProperties().entrySet().stream()
				.map(t -> new Property(t.getKey(), t.getValue())).collect(Collectors.toList());
		properties.add(new Property("id", comp.getId()));
		properties.add(new Property("alias", comp.getAlias()));

		this.componentManager.handleJsonrpcRequest(user,
				new CreateComponentConfigRequest(comp.getFactoryId(), properties));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	@Override
	public String debugLog() {
		return this.worker.debugLog();
	}

	/**
	 * deletes the given components only if they are not in notMyComponents.
	 *
	 * @param user            the executing user
	 * @param components      the components that should be deleted
	 * @param notMyComponents other needed components from the other apps
	 * @return the id s of the components that got deleted
	 */
	private List<String> deleteComponents(User user, List<Component> components, List<Component> notMyComponents)
			throws OpenemsNamedException {
		List<String> errors = new ArrayList<>();
		List<String> deletedIds = new ArrayList<>();
		for (var comp : components) {
			if (notMyComponents.stream().parallel().anyMatch(t -> t.getId().equals(comp.getId()))) {
				continue;
			}
			var component = this.componentManager.getEdgeConfig().getComponent(comp.getId()).orElse(null);
			if (component == null) {
				// component does not exist
				continue;
			}

			try {
				this.componentManager.handleJsonrpcRequest(user, new DeleteComponentConfigRequest(comp.getId()));
				deletedIds.add(comp.getId());
			} catch (OpenemsNamedException e) {
				errors.add(e.toString());
			}
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
		return deletedIds;
	}

	/**
	 * finds the app with the matching id.
	 *
	 * @param id of the app
	 * @return the found app
	 */
	private OpenemsApp findAppById(String id) {
		return this.availableApps.stream() //
				.filter(t -> t.getAppId().equals(id)) //
				.findFirst() //
				.get();
	}

	/**
	 * Gets an App Configuration with component id s, which can be used to create or
	 * rewrite the settings of the component.
	 *
	 * @param app                the {@link OpenemsApp}
	 * @param oldAppInstance     the old {@link OpenemsAppInstance}
	 * @param newAppInstance     the new {@link OpenemsAppInstance}
	 * @param otherAppComponents the components that are used from the other
	 *                           {@link OpenemsAppInstance}
	 * @return the AppConfiguration with the replaced ID s of the components
	 * @throws OpenemsNamedException on error
	 */
	private AppConfiguration getNewAppConfigWithReplacedIds(OpenemsApp app, OpenemsAppInstance oldAppInstance,
			OpenemsAppInstance newAppInstance, List<Component> otherAppComponents) throws OpenemsNamedException {

		var target = oldAppInstance == null ? ConfigurationTarget.ADD : ConfigurationTarget.UPDATE;
		var newAppConfig = app.getAppConfiguration(target, newAppInstance.properties);

		final var replacableIds = this.getReplaceableComponentIds(app, newAppInstance.properties);

		for (var comp : ComponentUtil.order(newAppConfig.components)) {
			// replace old id s with new ones
			for (var entry : comp.getProperties().entrySet()) {
				for (var replaceableId : replacableIds.entrySet()) {
					if (entry.getValue().toString().contains(replaceableId.getKey())) {
						var newId = entry.getValue().toString().replace(replaceableId.getKey(),
								newAppInstance.properties.get(replaceableId.getValue()).getAsString());
						newId = newId.replace("\"", "");
						var newValue = JsonUtils.getAsJsonElement(newId);
						comp.getProperties().put(entry.getKey(), newValue);
					}
				}
			}

			var isNewComponent = true;
			var id = comp.getId();
			var canBeReplaced = replacableIds.containsKey(id);
			Component foundComponent = null;

			// try to find a component with the necessary settings
			if (canBeReplaced) {
				foundComponent = this.componentUtil.getComponentByConfig(comp);
				if (foundComponent != null) {
					id = foundComponent.getId();
				}
			}
			if (foundComponent == null && oldAppInstance != null && oldAppInstance.properties.has(id.toUpperCase())) {
				id = oldAppInstance.properties.get(id.toUpperCase()).getAsString();
				foundComponent = this.componentManager.getEdgeConfig().getComponent(id).orElse(null);
				final var tempId = id;
				// other app uses the same component because they had the same configuration
				// now this app needs the component with a different configuration so now create
				// a new component
				if (foundComponent != null && otherAppComponents.stream().anyMatch(t -> t.getId().equals(tempId))) {
					foundComponent = null;
				}
			}
			isNewComponent = isNewComponent && foundComponent == null;
			if (isNewComponent) {
				// if the id is not already set and there is no component with the default id
				// then use the default id
				foundComponent = this.componentManager.getEdgeConfig().getComponent(comp.getId()).orElse(null);
				if (foundComponent == null) {
					id = comp.getId();
				} else {
					// replace number at the end and get the next available id
					var nextAvailableId = this.componentUtil.getNextAvailableId(id.replaceAll("\\d+", ""),
							otherAppComponents);
					if (!nextAvailableId.equals(id) && !canBeReplaced) {
						// component can not be created because the id is already used
						// and the id can not be set in the configuration
						continue;
					}
					if (canBeReplaced) {
						id = nextAvailableId;
					}
				}
			}

			if (canBeReplaced) {
				newAppInstance.properties.addProperty(replacableIds.get(comp.getId()), id);
			}
		}
		return app.getAppConfiguration(target, newAppInstance.properties);
	}

	/**
	 * Gets the components of all apps except the given.
	 *
	 * @param thisApp the app that components should not be included
	 * @return all components from all app instances except the given thisApp
	 */
	private List<Component> getOtherAppComponents(OpenemsAppInstance thisApp) {
		List<Component> allOtherComponents = new ArrayList<>();
		for (var instance : this.instantiatedApps) {
			if (instance.equals(thisApp)) {
				continue;
			}
			var app = this.availableApps.stream().filter(t -> t.getAppId().equals(instance.appId)).findFirst()
					.orElse(null);
			if (app == null) {
				continue;
			}
			try {
				allOtherComponents
						.addAll(app.getAppConfiguration(ConfigurationTarget.DELETE, instance.properties).components);
			} catch (OpenemsNamedException e) {
				// move to next component
			}
		}
		return allOtherComponents;
	}

	/**
	 * Gets ip s that are needed from the other {@link OpenemsAppInstance}s.
	 *
	 * @param thisApp the app which ip s should not be included
	 * @return all needed ip s from the other apps
	 */
	private List<String> getOtherAppIps(OpenemsAppInstance thisApp) {
		List<String> allOtherIps = new ArrayList<>();
		for (var instance : this.instantiatedApps) {
			if (instance.instanceId.equals(thisApp.instanceId)) {
				continue;
			}
			var app = this.availableApps.stream().filter(t -> t.getAppId().equals(instance.appId)).findFirst()
					.orElse(null);
			if (app == null) {
				continue;
			}
			try {
				allOtherIps.addAll(app.getAppConfiguration(ConfigurationTarget.VALIDATE, instance.properties).ips);
			} catch (OpenemsNamedException e) {
				// move to next component
			}
		}
		return allOtherIps;
	}

	/**
	 * Gets ip s that are needed from the other {@link OpenemsAppInstance}s.
	 *
	 * @param thisApp the app which ip s should not be included
	 * @return all needed ip s from the other apps
	 */
	private List<String> getOtherAppSchedulerIds(OpenemsAppInstance thisApp) {
		List<String> allOtherSchedlerIds = new ArrayList<>();
		for (var instance : this.instantiatedApps) {
			if (instance.instanceId.equals(thisApp.instanceId)) {
				continue;
			}
			var app = this.availableApps.stream().filter(t -> t.getAppId().equals(instance.appId)).findFirst()
					.orElse(null);
			if (app == null) {
				continue;
			}
			try {
				allOtherSchedlerIds.addAll(app.getAppConfiguration(ConfigurationTarget.VALIDATE,
						instance.properties).schedulerExecutionOrder);
			} catch (OpenemsNamedException e) {
				// move to next component
			}
		}
		return allOtherSchedlerIds;
	}

	/**
	 * Gets the component id s that can be replaced.
	 *
	 * @param app        the components of which app
	 * @param properties the default properties to create an app instance of this
	 *                   app
	 * @return a map of the component id s that can be replaced mapped from id to
	 *         key to put the next id
	 * @throws OpenemsNamedException on error
	 */
	private Map<String, String> getReplaceableComponentIds(OpenemsApp app, JsonObject properties)
			throws OpenemsNamedException {
		final var prefix = "?_?_";
		var config = app.getAppConfiguration(ConfigurationTarget.TEST, properties);
		var copyBuilder = JsonUtils.buildJsonObject();
		for (var entry : properties.entrySet()) {
			copyBuilder.add(entry.getKey(), entry.getValue());
		}
		for (var comp : config.components) {
			copyBuilder.addProperty(comp.getId(), prefix);
		}
		var copy = copyBuilder.build();
		var configWithNewIds = app.getAppConfiguration(ConfigurationTarget.TEST, copy);
		Map<String, String> replaceableComponentIds = new HashMap<>();
		for (var comp : configWithNewIds.components) {
			if (comp.getId().startsWith(prefix)) {
				// "METER_ID:meter0"
				var raw = comp.getId().substring(prefix.length());
				// ["METER_ID", "meter0"]
				var pieces = raw.split(":");
				// "METER_ID"
				var property = pieces[0];
				// "meter0"
				var defaultId = pieces[1];
				replaceableComponentIds.put(defaultId, property);
			}
		}
		return replaceableComponentIds;
	}

	/**
	 * Handles {@link AddAppInstance}.
	 *
	 * @param user    the User
	 * @param request the {@link AddAppInstance} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAddAppInstanceRequest(User user,
			AddAppInstance.Request request) throws OpenemsNamedException {
		var instanceId = UUID.randomUUID();

		var openemsApp = this.findAppById(request.appId);

		synchronized (this.instantiatedApps) {

			this.checkCardinality(openemsApp);

			this.checkStatus(openemsApp);

			// create app instance
			var app = new OpenemsAppInstance(request.appId, request.alias, instanceId, request.properties);
			List<String> errors = new Vector<>();
			var completable = this.updateAppSettings(errors, user, openemsApp, null, app);

			try {
				// wait until everything is finished
				completable.get();
			} catch (ExecutionException | CancellationException | InterruptedException e) {
				errors.add(e.getMessage());
			}
			if (completable.isCancelled() || completable.isCompletedExceptionally() || !errors.isEmpty()) {
				throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
			}
			// Update App-Manager configuration
			try {
				this.instantiatedApps.add(app);
				this.updateAppManagerConfiguration(user, this.instantiatedApps);
			} catch (OpenemsNamedException e) {
				throw new OpenemsException(
						"AddAppInstance: unable to update App-Manager configuration: " + e.getMessage());
			}
		}
		return CompletableFuture.completedFuture(new AddAppInstance.Response(request.id, instanceId));
	}

	/**
	 * Handles a {@link DeleteAppInstance}.
	 *
	 * @param user    the user
	 * @param request the {@link DeleteAppInstance}
	 * @return the request id
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleDeleteAppInstanceRequest(User user,
			DeleteAppInstance.Request request) throws OpenemsNamedException {

		synchronized (this.instantiatedApps) {

			final var instance = this.instantiatedApps.stream().filter(t -> t.instanceId.equals(request.instanceId))
					.findFirst().orElse(null);
			if (instance == null) {
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.id));
			}
			var app = this.findAppById(instance.appId);
			var config = app.getAppConfiguration(ConfigurationTarget.DELETE, instance.properties);
			List<OpenemsNamedException> errors = new Vector<>();

			var deleteComponents = CompletableFuture.runAsync(() -> {
				try {
					var deletedIds = this.deleteComponents(user, config.components,
							this.getOtherAppComponents(instance));
					deletedIds.addAll(config.schedulerExecutionOrder);
					deletedIds.removeAll(this.getOtherAppSchedulerIds(instance));
					this.removeIdsInSchedulerIfExisting(user, deletedIds);
				} catch (OpenemsNamedException e) {
					errors.add(e);
				}
			});
			// TODO remove 'if' if it works on windows
			// rewriting network configuration only works on Linux
			var updateNetworkConfig = CompletableFuture.runAsync(() -> {
				if (!System.getProperty("os.name").startsWith("Windows")) {
					var ips = new ArrayList<>(config.ips);
					ips.removeAll(this.getOtherAppIps(instance));
					try {
						this.removeStaticIpsIfExisting(user, ips);
					} catch (OpenemsNamedException e) {
						errors.add(e);
					}
				}
			});
			try {
				// wait until everything is finished
				CompletableFuture.allOf(deleteComponents, updateNetworkConfig).get();
			} catch (ExecutionException | CancellationException | InterruptedException e) {
				errors.add(new OpenemsException(e.toString()));
			}
			try {
				this.instantiatedApps.remove(instance);
				this.updateAppManagerConfiguration(user, this.instantiatedApps);
			} catch (OpenemsNamedException e) {
				errors.add(new OpenemsException(e.toString()));
			}

			if (!errors.isEmpty()) {
				throw new OpenemsException(
						errors.stream().map(OpenemsNamedException::toString).collect(Collectors.joining("|")));
			}
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.id));
	}

	/**
	 * Handles {@link GetAppAssistant}.
	 *
	 * @param user    the User
	 * @param request the {@link GetAppAssistant} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetAppAssistantRequest(User user,
			GetAppAssistant.Request request) throws OpenemsNamedException {
		for (var app : this.availableApps) {
			if (request.appId.equals(app.getAppId())) {
				return CompletableFuture
						.completedFuture(new GetAppAssistant.Response(request.id, app.getAppAssistant()));
			}
		}
		throw new OpenemsException("App-ID [" + request.appId + "] is unknown");
	}

	/**
	 * Handles {@link GetAppInstances}.
	 *
	 * @param user    the User
	 * @param request the {@link GetAppInstances} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetAppInstancesRequest(User user,
			GetAppInstances.Request request) throws OpenemsNamedException {
		var instances = this.instantiatedApps.stream() //
				.filter(i -> i.appId.equals(request.appId)) //
				.collect(Collectors.toList());
		return CompletableFuture.completedFuture(new GetAppInstances.Response(request.id, instances));
	}

	/**
	 * Handles a {@link GetAppRequest}.
	 *
	 * @param user    the User
	 * @param request the {@link GetAppRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetAppRequest(User user, GetApp.Request request)
			throws OpenemsNamedException {
		var app = this.availableApps.stream().filter(t -> t.getAppId().equals(request.appId)).findFirst().get();
		var instances = this.instantiatedApps.stream().filter(t -> t.appId.equals(request.appId))
				.collect(Collectors.toList());
		return CompletableFuture.completedFuture(new GetApp.Response(request.id, app, instances));
	}

	/**
	 * Handles a {@link GetAppsRequest}.
	 *
	 * @param user    the User
	 * @param request the {@link GetAppsRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetAppsRequest(User user, GetApps.Request request)
			throws OpenemsNamedException {
		return CompletableFuture
				.completedFuture(new GetApps.Response(request.id, this.availableApps, this.instantiatedApps));
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.OWNER);

		switch (request.getMethod()) {

		case GetApps.METHOD:
			return this.handleGetAppsRequest(user, GetApps.Request.from(request));

		case GetApp.METHOD:
			return this.handleGetAppRequest(user, GetApp.Request.from(request));

		case GetAppAssistant.METHOD:
			return this.handleGetAppAssistantRequest(user, GetAppAssistant.Request.from(request));

		case GetAppInstances.METHOD:
			return this.handleGetAppInstancesRequest(user, GetAppInstances.Request.from(request));

		case AddAppInstance.METHOD:
			return this.handleAddAppInstanceRequest(user, AddAppInstance.Request.from(request));

		case UpdateAppInstance.METHOD:
			return this.handleUpdateAppInstanceRequest(user, UpdateAppInstance.Request.from(request));

		case DeleteAppInstance.METHOD:
			return this.handleDeleteAppInstanceRequest(user, DeleteAppInstance.Request.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles {@link UpdateAppInstance}.
	 *
	 * @param user    the User
	 * @param request the {@link UpdateAppInstance} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateAppInstanceRequest(User user,
			UpdateAppInstance.Request request) throws OpenemsNamedException {
		OpenemsAppInstance newApp = null;
		OpenemsAppInstance oldApp = null;
		synchronized (this.instantiatedApps) {
			for (var app : this.instantiatedApps) {
				if (app.instanceId.equals(request.instanceId)) {
					oldApp = app;
					newApp = new OpenemsAppInstance(app.appId, request.alias, app.instanceId, request.properties);
					break;
				}
			}

			if (newApp == null) {
				throw new OpenemsException("App-Instance-ID [" + request.instanceId + "] is unknown.");
			}

			var errors = this.reconfigurApp(user, oldApp, newApp);

			// Update App-Manager configuration
			try {
				this.instantiatedApps.remove(oldApp);
				this.instantiatedApps.add(newApp);
				this.updateAppManagerConfiguration(user, this.instantiatedApps);
			} catch (OpenemsNamedException e) {
				throw new OpenemsException("Unable to update App-Manager configuration for ID [" + request.instanceId
						+ "]: " + e.getMessage());
			}

			if (!errors.isEmpty()) {
				throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
			}

		}
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.id));
	}

	@Modified
	private void modified(ComponentContext componentContext, Config config) throws OpenemsNamedException {
		super.modified(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config);
		this.worker.triggerNextRun();
	}

	/**
	 * Reconfigurates an app instance.
	 *
	 * @param user           the executing user
	 * @param oldAppInstance the old app instance with the old configuration
	 * @param newAppInstance the new app instance with the new configuration
	 * @return the errors that occurred during reconfiguration
	 */
	private List<String> reconfigurApp(User user, OpenemsAppInstance oldAppInstance, OpenemsAppInstance newAppInstance)
			throws OpenemsNamedException {
		var app = this.findAppById(newAppInstance.appId);
		List<String> errors = new Vector<>();
		var completable = this.updateAppSettings(errors, user, app, oldAppInstance, newAppInstance);
		try {
			// wait until everything is finished
			completable.get();
		} catch (ExecutionException | CancellationException | InterruptedException e) {
			errors.add(e.getMessage());
		}
		return errors;
	}

	/**
	 * checks if the settings of the component changed if there is a change it
	 * rewrites the settings of the given component.
	 *
	 * @param user       the executing user
	 * @param myComp     the component that configuration should be rewritten
	 * @param actualComp the actual component that exists
	 * @throws OpenemsNamedException when the configuration can not be rewritten
	 */
	private void reconfigure(User user, Component myComp, Component actualComp) throws OpenemsNamedException {
		if (ComponentUtil.isSameConfiguration(null, myComp, actualComp)) {
			return;
		}

		// send update request
		List<Property> properties = myComp.getProperties().entrySet().stream()
				.map(t -> new Property(t.getKey(), t.getValue())) //
				.collect(Collectors.toList());
		properties.add(new Property("alias", myComp.getAlias()));
		var updateRequest = new UpdateComponentConfigRequest(actualComp.getId(), properties);
		this.componentManager.handleJsonrpcRequest(user, updateRequest);
	}

	/**
	 * removes the given id s from the scheduler if they exist in the scheduler.
	 *
	 * @param user       the executing user
	 * @param removedIds the ip s that should be removed
	 * @throws OpenemsNamedException on error
	 */
	private void removeIdsInSchedulerIfExisting(User user, List<String> removedIds) throws OpenemsNamedException {
		if (removedIds == null || removedIds.isEmpty()) {
			return;
		}
		// get current order
		var schedulerComponents = this.componentManager.getEdgeConfig()
				.getComponentsByFactory("Scheduler.AllAlphabetically");
		var schedulerComponent = schedulerComponents.get(0);
		var controllerIdsElement = schedulerComponent.getProperty("controllers.ids").orElse(new JsonArray());
		var controllerIds = JsonUtils.getAsJsonArray(controllerIdsElement);

		// remove id s
		for (var id : removedIds) {
			for (var i = 0; i < controllerIds.size(); i++) {
				if (controllerIds.get(i).getAsString().equals(id)) {
					controllerIds.remove(i);
					break;
				}
			}
		}

		// update order
		var updateRequest = new UpdateComponentConfigRequest(schedulerComponent.getId(),
				Arrays.asList(new Property("controllers.ids", controllerIds)));
		this.componentManager.handleJsonrpcRequest(user, updateRequest);
	}

	/**
	 * removes the given ip s in the host configuration if they exist.
	 *
	 * @param user the executing user
	 * @param ips  the ip s that should be removed
	 * @throws OpenemsNamedException on error
	 */
	private void removeStaticIpsIfExisting(User user, List<String> ips) throws OpenemsNamedException {
		this.updateHosts(user, null, ips);
	}

	/**
	 * updated the AppManager configuration with the given app instances.
	 *
	 * @param user the executing user
	 * @param apps the new apps that should be written in the configuration
	 * @throws OpenemsNamedException when the configuration can not be updated
	 */
	private void updateAppManagerConfiguration(User user, List<OpenemsAppInstance> apps) throws OpenemsNamedException {
		var p = new Property("apps", getJsonAppsString(apps));
		var updateRequest = new UpdateComponentConfigRequest(SINGLETON_COMPONENT_ID, Arrays.asList(p));
		this.componentManager.handleJsonrpcRequest(user, updateRequest);
	}

	/**
	 * creates the needed components of the given app with the given config and
	 * updates components with a new configuration and deletes unused components.
	 *
	 * @param errorList      a list for the errors that occur
	 * @param user           the executing user
	 * @param app            the app that should be created
	 * @param oldAppInstance the old app instance
	 * @param newAppInstance the new app instance
	 * @return the completableFuture of this task
	 */
	public CompletableFuture<Void> updateAppSettings(List<String> errorList, User user, OpenemsApp app,
			OpenemsAppInstance oldAppInstance, OpenemsAppInstance newAppInstance) throws OpenemsNamedException {
		final List<String> errors;
		if (errorList == null) {
			errors = new Vector<>();
		} else {
			errors = errorList;
		}
		AppConfiguration oldAppConfigTemp = null;
		if (oldAppInstance != null) {
			oldAppInstance.properties.addProperty("ALIAS", oldAppInstance.alias);
			try {
				oldAppConfigTemp = app.getAppConfiguration(ConfigurationTarget.VALIDATE, oldAppInstance.properties);
			} catch (OpenemsNamedException ex) {
				errors.add(ex.getMessage());
			}
		}
		final var oldAppConfig = oldAppConfigTemp;
		// adding alias to the properties in order to access it while defining it in the
		// App Configuration
		newAppInstance.properties.addProperty("ALIAS", newAppInstance.alias);
		final var otherComponents = this.getOtherAppComponents(newAppInstance);
		final var newAppConfig = this.getNewAppConfigWithReplacedIds(app, oldAppInstance, newAppInstance,
				otherComponents);

		// TODO remove 'if' if it works on windows
		// rewriting network configuration only works on Linux
		if (!System.getProperty("os.name").startsWith("Windows")) {
			try {
				this.updateHosts(user, newAppConfig.ips, oldAppConfig != null ? oldAppConfig.ips : null);
			} catch (OpenemsNamedException e) {
				var error = "Can not update Host Config";
				errors.add(error);
			}
		}

		try {
			// validate input e. g. ping a specific ip
			app.getValidator().validateConfiguration(ConfigurationTarget.ADD, newAppInstance.properties);
		} catch (OpenemsNamedException ex) {
			// revert network configuration
			errors.add(ex.getMessage());
			return CompletableFuture.runAsync(() -> {
				if (!System.getProperty("os.name").startsWith("Windows")) {
					var ips = new ArrayList<>(newAppConfig.ips);
					ips.removeAll(this.getOtherAppIps(newAppInstance));
					try {
						this.removeStaticIpsIfExisting(user, ips);
					} catch (OpenemsNamedException e) {
						errors.add(e.getMessage());
					}
				}
			});
		}

		// adds / updates components
		var updatingComponents = CompletableFuture.runAsync(() -> {

			// create components
			for (Component comp : ComponentUtil.order(newAppConfig.components)) {
				/**
				 * if comp already exists with same config as needed => use it. if comp exist
				 * with different config and no other app needs it => rewrite settings. if comp
				 * exist with different config and other app needs it => create new comp
				 */
				var foundComponentWithSameId = this.componentManager.getEdgeConfig().getComponent(comp.getId())
						.orElse(null);
				if (oldAppConfig != null) {
					oldAppConfig.components.removeIf(t -> t.getId().equals(comp.getId()));
				}
				if (foundComponentWithSameId != null) {
					if (ComponentUtil.isSameConfiguration(null, comp, foundComponentWithSameId)) {
						// same configuration so no reconfiguration needed
						continue;
					}

					// check if it is my component
					if (otherComponents.stream().anyMatch(t -> t.getId().equals(foundComponentWithSameId.getId()))) {
						errors.add("Configuration of component with id '" + foundComponentWithSameId.getId()
								+ "' can not be rewritten. Because the component belongs to another app.");
						continue;
					}
					try {
						this.reconfigure(user, comp, foundComponentWithSameId);
					} catch (OpenemsNamedException e) {
						errors.add(e.getMessage());
					}
					continue;
				}

				// create new component
				try {
					this.createComponent(user, comp);
				} catch (OpenemsNamedException e) {
					var error = "Component[" + comp.getFactoryId() + "] cant be created!";
					errors.add(error);
					errors.add(e.getMessage());
				}

			}

		});

		// deletes components that were used in the old configuration but are not in the
		// new configuration
		var updateSchedulerDeletingIds = updatingComponents.thenRunAsync(() -> {
			if (oldAppConfig != null) {
				try {
					var deletedIds = this.deleteComponents(user, oldAppConfig.components, otherComponents);
					if (oldAppConfig != null) {
						oldAppConfig.schedulerExecutionOrder.removeAll(newAppConfig.schedulerExecutionOrder);
						deletedIds.addAll(oldAppConfig.schedulerExecutionOrder);
					}
					deletedIds.removeAll(this.getOtherAppSchedulerIds(newAppInstance));
					this.removeIdsInSchedulerIfExisting(user, deletedIds);
				} catch (OpenemsNamedException e) {
					errors.add(e.getMessage());
				}

			}
		});

		// remove alias so it does not get written down twice in the app configuration
		var removingAlias = updateSchedulerDeletingIds.thenRunAsync(() -> {
			if (oldAppInstance != null) {
				oldAppInstance.properties.remove("ALIAS");
			}
			newAppInstance.properties.remove("ALIAS");
		});

		var updateSchedulerIdsOrder = CompletableFuture.runAsync(() -> {
			try {
				this.updateScheduler(user, newAppConfig.schedulerExecutionOrder);
			} catch (OpenemsNamedException e) {
				var error = "Scheduler cant be updated!";
				errors.add(error);
			}
		});

		return CompletableFuture.allOf(updatingComponents, updateSchedulerDeletingIds, updateSchedulerIdsOrder,
				removingAlias);
	}

	/**
	 * updates the host configuration deletes ip s that are in {@link oldIps} but
	 * not in {@link ips} and adds ip s that are in {@link ips} but not in
	 * {@link oldIps}.
	 *
	 * @param user   the executing user
	 * @param ips    the ip s that should be in the configuration
	 * @param oldIps the old ip s that were in the configuration
	 * @throws OpenemsNamedException on error
	 */
	private void updateHosts(User user, List<String> ips, List<String> oldIps) throws OpenemsNamedException {
		if ((ips == null || ips.isEmpty()) && (oldIps == null || oldIps.isEmpty())) {
			return;
		}
		if (ips == null) {
			ips = new ArrayList<>();
		}
		var errors = new ArrayList<String>();
		List<String> deleteIps;
		if (oldIps == null) {
			deleteIps = new ArrayList<>();
		} else {
			deleteIps = new ArrayList<>(oldIps);
			deleteIps.removeAll(ips);
		}

		// parse ip s
		List<Inet4AddressWithNetmask> deleteIpAddresses = new ArrayList<>();
		for (var ip : deleteIps) {
			try {
				deleteIpAddresses.add(Inet4AddressWithNetmask.fromString(ip));
			} catch (OpenemsException e) {
				errors.add("Ip '" + ip + "' can not be parsed.");
			}
		}

		List<Inet4AddressWithNetmask> ipAddresses = new ArrayList<>(ips.size());
		for (var ip : ips) {
			try {
				ipAddresses.add(Inet4AddressWithNetmask.fromString(ip));
			} catch (OpenemsException e) {
				errors.add("Ip '" + ip + "' can not be parsed.");
			}
		}

		var interfaces = this.componentUtil.getInterfaces();
		var eth0 = interfaces.stream().filter(t -> t.getName().equals("eth0")).findFirst().get();
		if (eth0 == null) {
			return;
		}

		// remove already added addresses
		for (var address : eth0.getAddresses().getValue()) {
			ipAddresses.remove(address);
		}

		// remove ip s from old configuration
		eth0.getAddresses().getValue().removeAll(deleteIpAddresses);

		// add ip s from new configuration
		eth0.getAddresses().getValue().addAll(ipAddresses);

		try {
			this.componentUtil.updateInterfaces(user, interfaces);
		} catch (OpenemsException e) {
			errors.add(e.getMessage());
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
	}

	/**
	 * updates the execution order of the scheduler only adds or changes order of
	 * the given id s.
	 *
	 * @param user                    the executing user
	 * @param schedulerExecutionOrder the execution order
	 * @throws OpenemsNamedException when the scheduler can not be updated
	 */
	private void updateScheduler(User user, List<String> schedulerExecutionOrder) throws OpenemsNamedException {
		if (schedulerExecutionOrder == null || schedulerExecutionOrder.isEmpty()) {
			return;
		}
		// get current order
		var schedulerComponents = this.componentManager.getEdgeConfig()
				.getComponentsByFactory("Scheduler.AllAlphabetically");
		var schedulerComponent = schedulerComponents.get(0);
		var controllerIdsElement = schedulerComponent.getProperty("controllers.ids").orElse(new JsonArray());
		var controllerIds = JsonUtils.getAsJsonArray(controllerIdsElement);

		// remove last empty field
		if (controllerIds.size() >= 1 && controllerIds.get(controllerIds.size() - 1).getAsString().isBlank()) {
			controllerIds.remove(controllerIds.size() - 1);
		}

		// place id s in right order
		List<String> order = new ArrayList<>();
		controllerIds.forEach(t -> order.add(t.getAsString()));
		var index = 0;
		for (String id : schedulerExecutionOrder) {
			var idIndex = order.indexOf(id);
			if (idIndex != -1) {
				index = idIndex + 1;
				continue;
			}
			order.add(index++, id);
		}

		var newControllerIds = new JsonArray();
		for (var id : order) {
			newControllerIds.add(id);
		}

		// update order
		var updateRequest = new UpdateComponentConfigRequest(schedulerComponent.getId(),
				Arrays.asList(new Property("controllers.ids", newControllerIds)));
		this.componentManager.handleJsonrpcRequest(user, updateRequest);
	}

}
