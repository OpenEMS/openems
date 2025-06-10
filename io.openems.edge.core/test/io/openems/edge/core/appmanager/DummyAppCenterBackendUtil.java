package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse.Instance;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse.Bundle;
import io.openems.edge.common.user.User;

public class DummyAppCenterBackendUtil implements AppCenterBackendUtil {

	@Override
	public boolean isKeyApplicable(User user, String key, String appId) {
		return true;
	}

	@Override
	public void addInstallAppInstanceHistory(User user, String key, String appId, UUID instanceId)
			throws OpenemsNamedException {
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> addDeinstallAppInstanceHistory(User user, String appId,
			UUID instanceId) throws OpenemsNamedException {
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(UUID.randomUUID()));
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public List<Bundle> getPossibleApps(String key) {
		return Lists.newArrayList();
	}

	@Override
	public List<Instance> getInstalledApps() throws OpenemsNamedException {
		return Lists.newArrayList();
	}

}
