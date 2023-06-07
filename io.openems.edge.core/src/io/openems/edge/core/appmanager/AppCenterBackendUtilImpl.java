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
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.backend.ControllerApiBackend;

@Component
public class AppCenterBackendUtilImpl implements AppCenterBackendUtil {

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile ControllerApiBackend backend;

	@Activate
	public AppCenterBackendUtilImpl() {
	}

	@Override
	public boolean isKeyApplicable(User user, String key, String appId) {
		try {
			var response = this.handleRequest(user, new AppCenterIsKeyApplicableRequest(key, appId));
			return AppCenterIsKeyApplicableResponse.from(response).isKeyApplicable;
		} catch (OpenemsNamedException e) {
			return false;
		}
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
		if (this.backend == null) {
			return false;
		}
		return this.backend.isConnected();
	}

	private final CompletableFuture<? extends JsonrpcResponseSuccess> handleRequestAsync(User user,
			JsonrpcRequest request) throws OpenemsNamedException {
		return this.getBackend().handleJsonrpcRequest(user, new AppCenterRequest(request));
	}

	private final JsonrpcResponseSuccess handleRequest(User user, JsonrpcRequest request) throws OpenemsNamedException {
		try {
			return this.handleRequestAsync(user, request).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw getOpenemsException(e);
		}
	}

	private final ControllerApiBackend getBackend() throws OpenemsNamedException {
		if (!this.isConnected()) {
			throw new OpenemsException("Backend not connected!");
		}
		return this.backend;
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
