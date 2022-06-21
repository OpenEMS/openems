package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelper;
import io.openems.edge.core.appmanager.dependency.Dependency;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetApp;
import io.openems.edge.core.appmanager.jsonrpc.GetAppAssistant;
import io.openems.edge.core.appmanager.jsonrpc.GetAppDescriptor;
import io.openems.edge.core.appmanager.jsonrpc.GetAppInstances;
import io.openems.edge.core.appmanager.jsonrpc.GetApps;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = AppManager.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class AppManagerImpl extends AbstractOpenemsComponent
		implements AppManager, OpenemsComponent, JsonApi, ConfigurationListener {

	private final AppValidateWorker worker;
	private final AppInstallWorker appInstallWorker;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected List<OpenemsApp> availableApps;

	@Reference
	private AppManagerAppHelper appHelper;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ComponentUtil componentUtil;

	protected final List<OpenemsAppInstance> instantiatedApps = new ArrayList<>();

	public AppManagerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AppManager.ChannelId.values() //
		);
		this.worker = new AppValidateWorker(this);
		this.appInstallWorker = new AppInstallWorker(this);
	}

	@Activate
	private void activate(ComponentContext componentContext, Config config) {
		super.activate(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
		this.applyConfig(config);

		this.worker.activate(this.id());
		this.appInstallWorker.activate(this.id());
	}

	/**
	 * Gets an unmodifiable list of the current instantiated apps.
	 *
	 * @return the list of instantiated apps
	 */
	public final List<OpenemsAppInstance> getInstantiatedApps() {
		return Collections.unmodifiableList(this.instantiatedApps);
	}

	/**
	 * formats the given apps into a JSON array string.
	 *
	 * @param apps that should be formated
	 * @return formated apps string
	 */
	private static String getJsonAppsString(List<OpenemsAppInstance> apps) {
		return JsonUtils.prettyToString(apps.stream().map(t -> t.toJsonObject()).collect(JsonUtils.toJsonArray()));
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
			List<Dependency> dependecies = null;
			if (json.has("dependencies")) {
				dependecies = new LinkedList<>();
				var dependecyArray = json.get("dependencies").getAsJsonArray();
				for (int i = 0; i < dependecyArray.size(); i++) {
					var dependecyJson = dependecyArray.get(i).getAsJsonObject();
					var dependecy = new Dependency(dependecyJson.get("key").getAsString(),
							JsonUtils.getAsUUID(dependecyJson, "instanceId"));
					dependecies.add(dependecy);
				}
			}
			result.add(new OpenemsAppInstance(appId, alias, instanceId, properties, dependecies));
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

	protected void checkStatus(OpenemsApp openemsApp) throws OpenemsNamedException {
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

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		this.worker.configurationEvent(event);
	}

	public void foreachAppConfiguration(Consumer<AppConfiguration> consumer, UUID... excludingInstanceIds) {
		for (var appInstance : this.instantiatedApps) {
			var skipInstance = false;
			for (var id : excludingInstanceIds) {
				if (Objects.equals(id, appInstance.instanceId)) {
					skipInstance = true;
					break;
				}
			}
			if (skipInstance) {
				continue;
			}

			try {
				var app = this.findAppById(appInstance.appId);
				consumer.accept(app.getAppConfiguration(ConfigurationTarget.VALIDATE, appInstance.properties, null));
			} catch (OpenemsNamedException e) {
				// move to next app
			} catch (NoSuchElementException e) {
				// app not found for instance
				// this may happen if the app id gets refactored
				// apps which app ids are not known are printed in debug log as 'UNKNOWAPPS'
			}
		}
	}

	private void createComponent(User user, EdgeConfig.Component comp) throws OpenemsNamedException {
		List<Property> properties = comp.getProperties().entrySet().stream()
				.map(t -> new Property(t.getKey(), t.getValue())).collect(Collectors.toList());
		properties.add(new Property("id", comp.getId()));
		properties.add(new Property("alias", comp.getAlias()));

		// user can be null using internal method
		((ComponentManagerImpl) this.componentManager).handleCreateComponentConfigRequest(user,
				new CreateComponentConfigRequest(comp.getFactoryId(), properties));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		this.appInstallWorker.deactivate();
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
	private List<String> deleteComponents(User user, List<EdgeConfig.Component> components,
			List<EdgeConfig.Component> notMyComponents) throws OpenemsNamedException {
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
				// user can be null using internal method
				((ComponentManagerImpl) this.componentManager).handleDeleteComponentConfigRequest(user,
						new DeleteComponentConfigRequest(comp.getId()));
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
	public final OpenemsApp findAppById(String id) throws NoSuchElementException {
		return this.availableApps.stream() //
				.filter(t -> t.getAppId().equals(id)) //
				.findFirst() //
				.get();
	}

	/**
	 * Finds the app instance with the matching id.
	 * 
	 * @param uuid the id of the instance
	 * @returns the instance
	 * @throws NoSuchElementException if no instance is present
	 */
	public final OpenemsAppInstance findInstaceById(UUID uuid) throws NoSuchElementException {
		return this.instantiatedApps.stream() //
				.filter(t -> t.instanceId.equals(uuid)) //
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
	 * @param language           the language of the new config
	 * @return the AppConfiguration with the replaced ID s of the components
	 * @throws OpenemsNamedException on error
	 */
	private AppConfiguration getNewAppConfigWithReplacedIds(OpenemsApp app, OpenemsAppInstance oldAppInstance,
			OpenemsAppInstance newAppInstance, List<EdgeConfig.Component> otherAppComponents, Language language)
			throws OpenemsNamedException {

		var target = oldAppInstance == null ? ConfigurationTarget.ADD : ConfigurationTarget.UPDATE;
		var newAppConfig = app.getAppConfiguration(target, newAppInstance.properties, language);

		final var replacableIds = this.getReplaceableComponentIds(app, newAppInstance.properties);

		for (var comp : ComponentUtilImpl.order(newAppConfig.components)) {
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
			EdgeConfig.Component foundComponent = null;

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
		return app.getAppConfiguration(target, newAppInstance.properties, language);
	}

	/**
	 * Gets the components of all apps except the given.
	 *
	 * @param thisApp the app that components should not be included
	 * @return all components from all app instances except the given thisApp
	 */
	public List<EdgeConfig.Component> getOtherAppComponents(UUID... ignoreIds) {
		List<EdgeConfig.Component> allOtherComponents = new ArrayList<>();
		this.foreachAppConfiguration(c -> {
			allOtherComponents.addAll(c.components);
		}, ignoreIds);
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
		this.foreachAppConfiguration(c -> {
			allOtherIps.addAll(c.ips);
		}, thisApp.instanceId);
		return allOtherIps;
	}

	/**
	 * Gets Scheduler Order s that are needed from the other
	 * {@link OpenemsAppInstance}s. Every Id from the scheduler orders gets append
	 * to the list.
	 *
	 * @param thisApp the app which ip s should not be included
	 * @return all needed Scheduler Order s from the other apps
	 */
	private List<String> getOtherAppSchedulerOrders(OpenemsAppInstance thisApp) {
		List<String> allOtherIps = new ArrayList<>();
		this.foreachAppConfiguration(c -> {
			allOtherIps.addAll(c.schedulerExecutionOrder);
		}, thisApp.instanceId);
		return allOtherIps;
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
	protected final Map<String, String> getReplaceableComponentIds(OpenemsApp app, JsonObject properties)
			throws OpenemsNamedException {
		final var prefix = "?_?_";
		var config = app.getAppConfiguration(ConfigurationTarget.TEST, properties, null);
		var copyBuilder = JsonUtils.buildJsonObject();
		for (var entry : properties.entrySet()) {
			copyBuilder.add(entry.getKey(), entry.getValue());
		}
		for (var comp : config.components) {
			copyBuilder.addProperty(comp.getId(), prefix);
		}
		var copy = copyBuilder.build();
		var configWithNewIds = app.getAppConfiguration(ConfigurationTarget.TEST, copy, null);
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
	protected CompletableFuture<JsonrpcResponseSuccess> handleAddAppInstanceRequest(User user,
			AddAppInstance.Request request) throws OpenemsNamedException {
		var instanceId = UUID.randomUUID();

		var openemsApp = this.findAppById(request.appId);

		synchronized (this.instantiatedApps) {

			var installedApps = this.appHelper.installApp(user, request.properties, request.alias, openemsApp);
//
//			this.checkStatus(openemsApp);
//
//			// create app instance
//			var app = new OpenemsAppInstance(request.appId, request.alias, instanceId, request.properties, null);
//			List<String> errors = new Vector<>();
//			var completable = this.updateAppSettings(errors, user, openemsApp, null, app);
//
//			try {
//				// wait until everything is finished
//				completable.get();
//			} catch (ExecutionException | CancellationException | InterruptedException e) {
//				errors.add(e.getMessage());
//			}
//			if (!errors.isEmpty()) {
//				throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
//			}
//			// Update App-Manager configuration
//			try {
//				this.instantiatedApps.add(app);
//				this.updateAppManagerConfiguration(user, this.instantiatedApps);
//			} catch (OpenemsNamedException e) {
//				throw new OpenemsException(
//						"AddAppInstance: unable to update App-Manager configuration: " + e.getMessage());
//			}
			// Update App-Manager configuration
			try {
				this.instantiatedApps.addAll(installedApps);
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
			List<OpenemsNamedException> errors = new Vector<>();
			var removedApps = this.appHelper.deleteApp(user, instance);

//			var app = this.findAppById(instance.appId);
//
//			var config = app.getAppConfiguration(ConfigurationTarget.DELETE, instance.properties, null);
//
//			var deleteComponents = CompletableFuture.runAsync(() -> {
//				try {
//					var deletedIds = this.deleteComponents(user, config.components,
//							this.getOtherAppComponents(instance.instanceId));
//					deletedIds.addAll(config.schedulerExecutionOrder);
//					// do not remove ids in scheduler from other apps
//					// e. g. Home has ctrlBalancing0 in scheduler
//					// and also KebaEvcs has the ctrlBalancing0 in the scheduler
//					deletedIds.removeAll(this.getOtherAppSchedulerOrders(instance));
//					this.componentUtil.removeIdsInSchedulerIfExisting(user, deletedIds);
//				} catch (OpenemsNamedException e) {
//					errors.add(e);
//				}
//			});
//			// TODO remove 'if' if it works on windows
//			// rewriting network configuration only works on Linux
//			var updateNetworkConfig = CompletableFuture.runAsync(() -> {
//				if (!System.getProperty("os.name").startsWith("Windows")) {
//					var ips = new ArrayList<>(config.ips);
//					ips.removeAll(this.getOtherAppIps(instance));
//					try {
//						this.componentUtil.updateHosts(user, null, ips);
//					} catch (OpenemsNamedException e) {
//						errors.add(e);
//					}
//				}
//			});
//			try {
//				// wait until everything is finished
//				CompletableFuture.allOf(deleteComponents, updateNetworkConfig).get();
//			} catch (ExecutionException | CancellationException | InterruptedException e) {
//				errors.add(new OpenemsException(e.toString()));
//			}
			try {
//				this.instantiatedApps.remove(instance);
				this.instantiatedApps.removeAll(removedApps);
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
				return CompletableFuture.completedFuture(
						new GetAppAssistant.Response(request.id, app.getAppAssistant(user.getLanguage())));
			}
		}
		throw new OpenemsException("App-ID [" + request.appId + "] is unknown");
	}

	/**
	 * Handles {@link GetAppDescriptor}.
	 *
	 * @param user    the User
	 * @param request the {@link GetAppDescriptor} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetAppDescriptorRequest(User user,
			GetAppDescriptor.Request request) throws OpenemsNamedException {
		try {
			var app = this.findAppById(request.appId);
			return CompletableFuture.completedFuture(new GetAppDescriptor.Response(request.id, app.getAppDescriptor()));
		} catch (NoSuchElementException e) {
			throw new OpenemsException("App-ID [" + request.appId + "] is unknown");
		}
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
		return CompletableFuture.completedFuture(new GetApp.Response(request.id, app, instances, user.getLanguage()));
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
		return CompletableFuture.completedFuture(
				new GetApps.Response(request.id, this.availableApps, this.instantiatedApps, user.getLanguage()));
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

		case GetAppDescriptor.METHOD:
			return this.handleGetAppDescriptorRequest(user, GetAppDescriptor.Request.from(request));

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
			try {
				oldApp = this.findInstaceById(request.instanceId);
				newApp = new OpenemsAppInstance(oldApp.appId, request.alias, request.instanceId, request.properties,
						null);
			} catch (NoSuchElementException e) {
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
	private void reconfigure(User user, EdgeConfig.Component myComp, EdgeConfig.Component actualComp)
			throws OpenemsNamedException {
		if (ComponentUtilImpl.isSameConfiguration(null, myComp, actualComp)) {
			return;
		}

		// send update request
		List<Property> properties = myComp.getProperties().entrySet().stream()
				.map(t -> new Property(t.getKey(), t.getValue())) //
				.collect(Collectors.toList());
		properties.add(new Property("alias", myComp.getAlias()));
		var updateRequest = new UpdateComponentConfigRequest(actualComp.getId(), properties);
		// user can be null using internal method
		((ComponentManagerImpl) this.componentManager).handleUpdateComponentConfigRequest(user, updateRequest);
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
		// user can be null using internal method
		((ComponentManagerImpl) this.componentManager).handleUpdateComponentConfigRequest(user, updateRequest);
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
		final var language = user != null ? user.getLanguage() : null;
		AppConfiguration oldAppConfigTemp = null;
		if (oldAppInstance != null) {
			oldAppInstance.properties.addProperty("ALIAS", oldAppInstance.alias);
			try {
				oldAppConfigTemp = app.getAppConfiguration(ConfigurationTarget.VALIDATE, oldAppInstance.properties,
						language);
			} catch (OpenemsNamedException ex) {
				errors.add(ex.getMessage());
			}
		}
		final var oldAppConfig = oldAppConfigTemp;
		// adding alias to the properties in order to access it while defining it in the
		// App Configuration
		newAppInstance.properties.addProperty("ALIAS", newAppInstance.alias);
		final var otherComponents = this.getOtherAppComponents(newAppInstance.instanceId);
		final var newAppConfig = this.getNewAppConfigWithReplacedIds(app, oldAppInstance, newAppInstance,
				otherComponents, language);

		// TODO remove 'if' if it works on windows
		// rewriting network configuration only works on Linux
		if (!System.getProperty("os.name").startsWith("Windows")) {
			try {
				this.componentUtil.updateHosts(user, newAppConfig.ips, oldAppConfig != null ? oldAppConfig.ips : null);
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
						this.componentUtil.updateHosts(user, null, ips);
					} catch (OpenemsNamedException e) {
						errors.add(e.getMessage());
					}
				}
			});
		}

		// adds / updates components
		var updatingComponents = CompletableFuture.runAsync(() -> {
			var createdComponents = new LinkedList<EdgeConfig.Component>();
			// create components
			for (var comp : ComponentUtilImpl.order(newAppConfig.components)) {
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

					var isSameConfigWithoutAlias = ComponentUtilImpl.isSameConfigurationWithoutAlias(null, comp,
							foundComponentWithSameId);
					var isSameConfig = isSameConfigWithoutAlias
							&& comp.getAlias().equals(foundComponentWithSameId.getAlias());

					if (isSameConfig) {
						// same configuration so no reconfiguration needed
						continue;
					}

					// check if it is my component
					if (otherComponents.stream().anyMatch(t -> t.getId().equals(foundComponentWithSameId.getId()))) {
						// not my component but only the alias changed
						if (isSameConfigWithoutAlias) {
							// TODO maybe warning if the alias can't be set
							continue;
						}
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
					createdComponents.add(comp);
				} catch (OpenemsNamedException e) {
					var error = "Component[" + comp.getFactoryId() + "] cant be created!";
					errors.add(error);
					errors.add(e.getMessage());
				}

			}

			// update scheduler
			try {
				var schedulerOrder = new ArrayList<>(newAppConfig.schedulerExecutionOrder);
				// if another app needs this component for the scheduler now add it
				if (createdComponents.isEmpty()) {
					this.foreachAppConfiguration(c -> {

						// if any component id is included
						if (!createdComponents.stream().anyMatch(t -> c.schedulerExecutionOrder.contains(t.getId()))) {
							return;
						}

						var temp = this.componentUtil.insertSchedulerOrder(schedulerOrder, c.schedulerExecutionOrder);
						schedulerOrder.clear();
						schedulerOrder.addAll(temp);

					});
				}
				this.componentUtil.updateScheduler(user, schedulerOrder, createdComponents);
			} catch (OpenemsNamedException e) {
				errors.add("Can't update scheduler execute order. Message: " + e.getMessage());
			}

		});

		// deletes components that were used in the old configuration but are not in the
		// new configuration
		var updateSchedulerDeletingIds = updatingComponents.thenRunAsync(() -> {
			if (oldAppConfig != null) {
				try {
					var deletedIds = this.deleteComponents(user, oldAppConfig.components, otherComponents);
					oldAppConfig.schedulerExecutionOrder.removeAll(newAppConfig.schedulerExecutionOrder);
					deletedIds.addAll(oldAppConfig.schedulerExecutionOrder);
					this.componentUtil.removeIdsInSchedulerIfExisting(user, deletedIds);
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

		return CompletableFuture.allOf(updatingComponents, updateSchedulerDeletingIds, removingAlias);
	}

}
