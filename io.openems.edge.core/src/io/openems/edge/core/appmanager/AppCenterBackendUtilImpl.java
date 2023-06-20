package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AppCenterAddDeinstallInstanceHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddInstallInstanceHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterGetInstalledAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterGetPossibleAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterIsKeyApplicableRequest;
import io.openems.common.jsonrpc.request.AppCenterRequest;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse.Instance;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse.Bundle;
import io.openems.common.jsonrpc.response.AppCenterIsKeyApplicableResponse;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.backend.ControllerApiBackend;

@Component
public class AppCenterBackendUtilImpl implements AppCenterBackendUtil {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile ControllerApiBackend backend;

	private final ComponentManager componentManager;

	@Activate
	public AppCenterBackendUtilImpl(//
			@Reference ComponentManager componentManager //
	) {
		this.componentManager = componentManager;
	}

	@Override
	public boolean isKeyApplicable(User user, String key, String appId) throws OpenemsNamedException {
		var response = this.handleRequest(user, new AppCenterIsKeyApplicableRequest(key, appId));
		return AppCenterIsKeyApplicableResponse.from(response).isKeyApplicable;
	}

	@Override
	public void addInstallAppInstanceHistory(User user, String key, String appId, UUID instanceId)
			throws OpenemsNamedException {
		this.handleRequest(user, new AppCenterAddInstallInstanceHistoryRequest(key, //
				appId, instanceId, Optional.ofNullable(user).map(u -> u.getId()).orElse(null)));
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> addDeinstallAppInstanceHistory(User user, String appId,
			UUID instanceId) throws OpenemsNamedException {
		return this.handleRequestAsync(user, new AppCenterAddDeinstallInstanceHistoryRequest(appId, //
				instanceId, Optional.ofNullable(user).map(u -> u.getId()).orElse(null)));
	}

	@Override
	public List<Bundle> getPossibleApps(String key) {
		try {
			var response = this.handleRequest(null, new AppCenterGetPossibleAppsRequest(key));
			return AppCenterGetPossibleAppsResponse.from(response).possibleApps;
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<Instance> getInstalledApps() throws OpenemsNamedException {
		var response = this.handleRequest(null, new AppCenterGetInstalledAppsRequest());
		return AppCenterGetInstalledAppsResponse.from(response).installedApps;
	}

	@Override
	public boolean isConnected() {
		final var backendApi = this.getBackend();
		if (backendApi == null) {
			return false;
		}
		return backendApi.isConnected();
	}

	private final CompletableFuture<? extends JsonrpcResponseSuccess> handleRequestAsync(User user,
			JsonrpcRequest request) throws OpenemsNamedException {
		return this.getBackendOrError().handleJsonrpcRequest(user, new AppCenterRequest(request));
	}

	private final JsonrpcResponseSuccess handleRequest(User user, JsonrpcRequest request) throws OpenemsNamedException {
		try {
			return this.handleRequestAsync(user, request).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw getOpenemsException(e);
		}
	}

	private final ControllerApiBackend getBackendOrError() throws OpenemsNamedException {
		final var backendApi = this.getBackend();
		if (backendApi == null || !backendApi.isConnected()) {
			throw new OpenemsException("Backend not connected!");
		}
		return backendApi;
	}

	private final ControllerApiBackend getBackend() {
		if (this.backend != null) {
			return this.backend;
		}
		final var backendApis = this.componentManager.getEnabledComponentsOfType(ControllerApiBackend.class);
		if (backendApis.isEmpty()) {
			return null;
		}
		this.log.warn("BackendApi Controller exists but was not injected!");
		return backendApis.get(0);
	}

	private static final OpenemsNamedException getOpenemsException(Throwable e) {
		return getOpenemsException(e, true);
	}

	private static final OpenemsNamedException getOpenemsException(Throwable e, boolean isRootException) {
		if (e instanceof OpenemsNamedException) {
			return (OpenemsNamedException) e;
		}

		if (e.getCause() != null) {
			final var foundOpenemsException = getOpenemsException(e.getCause(), false);
			if (foundOpenemsException != null) {
				return foundOpenemsException;
			}
		}

		if (!isRootException) {
			return null;
		}

		return new OpenemsException(e.getMessage());
	}

}
