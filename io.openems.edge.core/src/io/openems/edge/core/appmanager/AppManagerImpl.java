package io.openems.edge.core.appmanager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetAppAssistant;
import io.openems.edge.core.appmanager.jsonrpc.GetAppInstances;
import io.openems.edge.core.appmanager.jsonrpc.GetApps;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

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

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected List<OpenemsApp> availableApps;

	@Reference
	protected ComponentManager componentManager;

	protected List<OpenemsAppInstance> instantiatedApps = new ArrayList<>();

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

		this.worker.activate(this.id());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext componentContext, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.modified(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.worker.triggerNextRun();
	}

	private synchronized void applyConfig(Config config) {
		var apps = config.apps();
		if (apps.isBlank()) {
			apps = "[]"; // default to empty array
		}
		try {
			this.instantiatedApps = parseInstantiatedApps(JsonUtils.parseToJsonArray(apps));
			this._setWrongAppConfiguration(false);

		} catch (OpenemsNamedException e) {
			this._setWrongAppConfiguration(true);
			e.printStackTrace();
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	/**
	 * Parses the configured apps to a List of {@link OpenemsAppInstance}s.
	 *
	 * @param apps the app configuration from Config.json as {@link JsonArray}
	 * @return List of {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on parse error
	 */
	private static List<OpenemsAppInstance> parseInstantiatedApps(JsonArray apps) throws OpenemsNamedException {
		var result = new ArrayList<OpenemsAppInstance>(apps.size());
		for (var appElement : apps) {
			var json = JsonUtils.getAsJsonObject(appElement);
			var appId = JsonUtils.getAsString(json, "appId");
			var instanceId = JsonUtils.getAsUUID(json, "instanceId");
			var properties = JsonUtils.getAsJsonObject(json, "properties");
			result.add(new OpenemsAppInstance(appId, instanceId, properties));
		}
		return result;
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		this.worker.configurationEvent(event);
	}

	@Override
	public String debugLog() {
		return this.worker.debugLog();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.OWNER);

		switch (request.getMethod()) {

		case GetApps.Request.METHOD:
			return this.handleGetAppsRequest(user, GetApps.Request.from(request));

		case GetAppAssistant.METHOD:
			return this.handleGetAppAssistantRequest(user, GetAppAssistant.Request.from(request));

		case GetAppInstances.METHOD:
			return this.handleGetAppInstancesRequest(user, GetAppInstances.Request.from(request));

		case AddAppInstance.METHOD:
			return this.handleAddAppInstanceRequest(user, AddAppInstance.Request.from(request));

		case UpdateAppInstance.METHOD:
			return this.handleUpdateAppInstanceRequest(user, UpdateAppInstance.Request.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
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
	 * Handles {@link AddAppInstance}.
	 *
	 * @param user    the User
	 * @param request the {@link AddAppInstance} Request
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAddAppInstanceRequest(User user,
			AddAppInstance.Request request) throws OpenemsNamedException {
		// Create new list of Apps
		final var newApps = new ArrayList<>(this.instantiatedApps);
		var instanceId = UUID.randomUUID();
		var app = new OpenemsAppInstance(request.appId, instanceId, request.properties);
		newApps.add(app);

		// Update App-Manager configuration
		try {
			this.updateAppManagerConfiguration(user, newApps);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpenemsException("AddAppInstance: unable to update App-Manager configuration: " + e.getMessage());
		}

		return CompletableFuture.completedFuture(new AddAppInstance.Response(request.id, instanceId));
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
		var foundInstanceId = false;
		final var newApps = new ArrayList<OpenemsAppInstance>();
		synchronized (this.instantiatedApps) {
			// Create new list of Apps
			for (var oldApp : this.instantiatedApps) {
				if (oldApp.instanceId.equals(request.instanceId)) {
					foundInstanceId = true;
					var newApp = new OpenemsAppInstance(oldApp.appId, oldApp.instanceId, request.properties);
					newApps.add(newApp);

				} else {
					newApps.add(oldApp);
				}
			}
		}

		if (!foundInstanceId) {
			throw new OpenemsException("App-Instance-ID [" + request.instanceId + "] is unknown");
		}

		// Update App-Manager configuration
		try {
			this.updateAppManagerConfiguration(user, newApps);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpenemsException("Unable to update App-Manager configuration for ID [" + request.instanceId
					+ "]: " + e.getMessage());
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.id));
	}

	/**
	 * Reconfigure myself to persist the actual App configuration.
	 *
	 * @param user the executing user
	 * @param apps a list of {@link OpenemsAppInstance}s
	 * @throws IOException on error
	 */
	private void updateAppManagerConfiguration(User user, List<OpenemsAppInstance> apps) throws IOException {
		var factoryPid = this.serviceFactoryPid();
		final var config = this.cm.getConfiguration(factoryPid, null);
		var properties = config.getProperties();
		if (properties == null) {
			// No configuration existing yet -> create new configuration
			properties = new Hashtable<>();
		} else {
			// 'Host' configuration exists -> update configuration
		}
		var appsProperty = JsonUtils.buildJsonArray();
		for (var app : apps) {
			appsProperty.add(app.toJsonObject());
		}

		properties.put("apps", JsonUtils.prettyToString(appsProperty.build()));
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, user.getId() + ": " + user.getName());
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
				LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
		config.update(properties);
	}
}
