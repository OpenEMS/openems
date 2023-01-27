package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse.Instance;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse.Bundle;
import io.openems.edge.common.user.User;

public interface AppCenterBackendUtil {

	/**
	 * Gets from the backend if the given key can be applied to the given appId.
	 * 
	 * @param user  the executing user
	 * @param key   the key to be validated
	 * @param appId the id of the {@link OpenemsApp}
	 * @return true if the key can be applied
	 */
	public boolean isKeyApplicable(User user, String key, String appId);

	/**
	 * Adds a install app history entry to the given key.
	 * 
	 * @param user       the executing user
	 * @param key        the key the app should get installed with
	 * @param appId      the app id of the created instance
	 * @param instanceId the instanceId of the created instance
	 * @throws OpenemsNamedException on error
	 */
	public void addInstallAppInstanceHistory(User user, String key, String appId, UUID instanceId)
			throws OpenemsNamedException;

	/**
	 * Adds a deinstall app history entry to the key which installed the instance.
	 * 
	 * @param user       the executing user
	 * @param appId      the app id of the removed instance
	 * @param instanceId the instanceId of the removed instance
	 * @return the {@link CompletableFuture} of the request
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> addDeinstallAppInstanceHistory(User user, String appId,
			UUID instanceId) throws OpenemsNamedException;

	/**
	 * Gets if this edge is connected to the backend.
	 * 
	 * @return true if this edge is connected to the backend
	 */
	public boolean isConnected();

	/**
	 * Gets the possible apps that can be installed with the given key.
	 * 
	 * @param key the key that the apps can be installed with
	 * @return a list of bundles with a list of their apps
	 */
	public List<Bundle> getPossibleApps(String key);

	/**
	 * Gets the installed apps that were logged in the backend.
	 * 
	 * @return the instances that should be installed
	 */
	public List<Instance> getInstalledApps() throws OpenemsNamedException;

}
