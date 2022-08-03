package io.openems.edge.core.appmanager;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;
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

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.session.Role;
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
import io.openems.edge.core.appmanager.validator.Validator;
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

	// TODO maybe a worker which resolves defective apps
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

	@Reference
	protected Validator validator;

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
		return JsonUtils
				.prettyToString(apps.stream().map(OpenemsAppInstance::toJsonObject).collect(JsonUtils.toJsonArray()));
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
				for (var i = 0; i < dependecyArray.size(); i++) {
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

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		this.worker.configurationEvent(event);
	}

	/**
	 * Gets a filter for excluding instances.
	 *
	 * @param excludingInstanceIds the instances that should be excluded
	 * @return the filter
	 */
	public static Predicate<? super OpenemsAppInstance> excludingInstanceIds(UUID... excludingInstanceIds) {
		return i -> !Arrays.stream(excludingInstanceIds).anyMatch(id -> id.equals(i.instanceId));
	}

	/**
	 * Gets an {@link Iterable} that loops thru every existing app instance and its
	 * configuration.
	 *
	 * @return the {@link Iterable}
	 */
	public Iterable<Entry<OpenemsAppInstance, AppConfiguration>> appConfigs() {
		return this.appConfigs(null);
	}

	/**
	 * Gets an {@link Iterable} that loops thru every existing app instance and its
	 * configuration.
	 *
	 * @param filter the filter that gets applied to the instances
	 * @return the {@link Iterable}
	 */
	public Iterable<Entry<OpenemsAppInstance, AppConfiguration>> appConfigs(
			Predicate<? super OpenemsAppInstance> filter) {
		return this.appConfigs(this.instantiatedApps, filter);
	}

	/**
	 * Gets an {@link Iterable} that loops thru every instance and its
	 * configuration.
	 *
	 * @param instances the instances
	 * @param filter    the filter that gets applied to the instances
	 * @return the {@link Iterable}
	 */
	public Iterable<Entry<OpenemsAppInstance, AppConfiguration>> appConfigs(List<OpenemsAppInstance> instances,
			Predicate<? super OpenemsAppInstance> filter) {
		return new Iterable<>() {
			@Override
			public Iterator<Entry<OpenemsAppInstance, AppConfiguration>> iterator() {
				return AppManagerImpl.this.appConfigIterator(instances, filter);
			}
		};
	}

	/**
	 * Gets an {@link Iterator} that loops thru every instance and its
	 * configuration.
	 *
	 * @param instances the instances
	 * @param filter    the filter that gets applied to the instances
	 * @return the {@link Iterator}
	 */
	private Iterator<Entry<OpenemsAppInstance, AppConfiguration>> appConfigIterator(List<OpenemsAppInstance> instances,
			Predicate<? super OpenemsAppInstance> filter) {
		List<OpenemsAppInstance> actualInstances = instances.stream().filter(i -> filter == null || filter.test(i)) //
				.collect(Collectors.toList());
		return new Iterator<>() {

			private final Iterator<OpenemsAppInstance> instanceIterator = actualInstances.iterator();

			private OpenemsAppInstance nextInstance = null;
			private AppConfiguration nextConfiguration = null;

			@Override
			public Entry<OpenemsAppInstance, AppConfiguration> next() {
				var returnValue = new AbstractMap.SimpleEntry<>(this.nextInstance, this.nextConfiguration);
				this.nextInstance = null;
				this.nextConfiguration = null;
				return returnValue;
			}

			@Override
			public boolean hasNext() {
				// value not obtained
				if (this.nextConfiguration != null) {
					return true;
				}
				while (this.instanceIterator.hasNext() && this.nextConfiguration == null) {
					this.nextInstance = this.instanceIterator.next();

					if (this.nextInstance.properties == null) {
						continue;
					}

					try {
						var app = AppManagerImpl.this.findAppById(this.nextInstance.appId);
						this.nextInstance.properties.addProperty("ALIAS", this.nextInstance.alias);
						this.nextConfiguration = app.getAppConfiguration(ConfigurationTarget.VALIDATE,
								this.nextInstance.properties, null);
					} catch (OpenemsNamedException e) {
						// move to next app
					} catch (NoSuchElementException e) {
						// app not found for instance
						// this may happen if the app id gets refactored
						// apps which app ids are not known are printed in debug log as 'UNKNOWNAPPS'
					} finally {
						this.nextInstance.properties.remove("ALIAS");
					}
				}

				return this.nextConfiguration != null;
			}
		};
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
	 * finds the app with the matching id.
	 *
	 * @param id of the app
	 * @return the found app
	 */
	protected final OpenemsApp findAppById(String id) throws NoSuchElementException {
		return this.availableApps.stream() //
				.filter(t -> t.getAppId().equals(id)) //
				.findFirst() //
				.get();
	}

	/**
	 * Finds the app instance with the matching id.
	 *
	 * @param uuid the id of the instance
	 * @return s the instance
	 * @throws NoSuchElementException if no instance is present
	 */
	protected final OpenemsAppInstance findInstanceById(UUID uuid) throws NoSuchElementException {
		return this.instantiatedApps.stream() //
				.filter(t -> t.instanceId.equals(uuid)) //
				.findFirst() //
				.get();
	}

	/**
	 * Gets all {@link AppConfiguration}s from the existing
	 * {@link OpenemsAppInstance}s.
	 *
	 * @param ignoreIds the id's of the instances that should be ignored
	 * @return the {@link AppConfiguration}s
	 */
	public final List<AppConfiguration> getOtherAppConfigurations(UUID... ignoreIds) {
		List<AppConfiguration> allOtherConfigs = new ArrayList<>(this.instantiatedApps.size());
		for (var entry : this.appConfigs(AppManagerImpl.excludingInstanceIds(ignoreIds))) {
			allOtherConfigs.add(entry.getValue());
		}
		return allOtherConfigs;
	}

	/**
	 * Handles {@link AddAppInstance}.
	 *
	 * @param user    the User
	 * @param request the {@link AddAppInstance} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleAddAppInstanceRequest(User user,
			AddAppInstance.Request request) throws OpenemsNamedException {
		var openemsApp = this.findAppById(request.appId);
		synchronized (this.instantiatedApps) {

			var installedValues = this.appHelper.installApp(user, request.properties, request.alias, openemsApp);

			// Update App-Manager configuration
			try {
				// replace old instances with new ones
				this.instantiatedApps.removeAll(installedValues.modifiedOrCreatedApps);
				this.instantiatedApps.addAll(installedValues.modifiedOrCreatedApps);
				this.updateAppManagerConfiguration(user, this.instantiatedApps);
			} catch (OpenemsNamedException e) {
				throw new OpenemsException(
						"AddAppInstance: unable to update App-Manager configuration: " + e.getMessage());
			}
			return CompletableFuture.completedFuture(
					new AddAppInstance.Response(request.id, installedValues.rootInstance, installedValues.warnings));
		}
	}

	/**
	 * Handles a {@link DeleteAppInstance}.
	 *
	 * @param user    the user
	 * @param request the {@link DeleteAppInstance}
	 * @return the request id
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleDeleteAppInstanceRequest(User user,
			DeleteAppInstance.Request request) throws OpenemsNamedException {

		synchronized (this.instantiatedApps) {

			final OpenemsAppInstance instance;
			try {
				instance = this.findInstanceById(request.instanceId);
			} catch (NoSuchElementException e) {
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.id));
			}

			var result = this.appHelper.deleteApp(user, instance);

			try {
				this.instantiatedApps.removeAll(result.deletedApps);
				// replace modified apps
				this.instantiatedApps.removeAll(result.modifiedOrCreatedApps);
				this.instantiatedApps.addAll(result.modifiedOrCreatedApps);
				this.updateAppManagerConfiguration(user, this.instantiatedApps);
			} catch (OpenemsNamedException e) {
				throw new OpenemsException("Unable to update App-Manager configuration for ID [" + request.instanceId
						+ "]: " + e.getMessage());
			}
			return CompletableFuture.completedFuture(new DeleteAppInstance.Response(request.id, result.warnings));
		}
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
		return CompletableFuture
				.completedFuture(new GetApp.Response(request.id, app, instances, user.getLanguage(), this.validator));
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
		return CompletableFuture.completedFuture(new GetApps.Response(request.id, this.availableApps,
				this.instantiatedApps, user.getLanguage(), this.validator));
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

		synchronized (this.instantiatedApps) {
			OpenemsAppInstance oldApp = null;
			OpenemsApp app = null;

			try {
				oldApp = this.findInstanceById(request.instanceId);
				app = this.findAppById(oldApp.appId);
			} catch (NoSuchElementException e) {
				throw new OpenemsException("App-Instance-ID [" + request.instanceId + "] is unknown.");
			}

			var result = this.appHelper.updateApp(user, oldApp, request.properties, request.alias, app);

			// Update App-Manager configuration
			try {
				this.instantiatedApps.removeAll(result.deletedApps);
				// replace old instances with new ones
				this.instantiatedApps.removeAll(result.modifiedOrCreatedApps);
				this.instantiatedApps.addAll(result.modifiedOrCreatedApps);
				this.updateAppManagerConfiguration(user, this.instantiatedApps);
			} catch (OpenemsNamedException e) {
				throw new OpenemsException("Unable to update App-Manager configuration for ID [" + request.instanceId
						+ "]: " + e.getMessage());
			}
			var newInstance = this.findInstanceById(request.instanceId);
			return CompletableFuture
					.completedFuture(new UpdateAppInstance.Response(request.id, newInstance, result.warnings));
		}
	}

	@Modified
	private void modified(ComponentContext componentContext, Config config) throws OpenemsNamedException {
		super.modified(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config);
		this.worker.triggerNextRun();
	}

	/**
	 * updated the AppManager configuration with the given app instances.
	 *
	 * @param user the executing user
	 * @param apps the new apps that should be written in the configuration
	 * @throws OpenemsNamedException when the configuration can not be updated
	 */
	private void updateAppManagerConfiguration(User user, List<OpenemsAppInstance> apps) throws OpenemsNamedException {
		AppManagerImpl.sortApps(apps);
		var p = new Property("apps", getJsonAppsString(apps));
		var updateRequest = new UpdateComponentConfigRequest(SINGLETON_COMPONENT_ID, Arrays.asList(p));
		// user can be null using internal method
		if (user == null) {
			((ComponentManagerImpl) this.componentManager).handleUpdateComponentConfigRequest(user, updateRequest);
		} else {
			this.componentManager.handleJsonrpcRequest(user, updateRequest);
		}
	}

	private static void sortApps(List<OpenemsAppInstance> apps) {
		var compareTransformer = new BiFunction<Integer, Integer, Integer>() {
			@Override
			public Integer apply(Integer t, Integer u) {
				if (t == 0) {
					return 0;
				}
				return (int) (t / Math.abs(t) * Math.pow(10, u));
			}
		};
		apps.sort((o1, o2) -> {
			var value = compareTransformer.apply(o1.appId.compareTo(o2.appId), 3);
			var aliasValue = compareTransformer.apply(
					Optional.ofNullable(o1.alias).orElse("").compareTo(Optional.ofNullable(o2.alias).orElse("")), 2);
			var instanceValue = compareTransformer.apply(o1.instanceId.compareTo(o2.instanceId), 1);
			return value + aliasValue + instanceValue;
		});
	}
}
