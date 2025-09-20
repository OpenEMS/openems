package io.openems.backend.common.metadata;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@ProviderType
public interface AppCenterMetadata {

	/**
	 * Sends a request if the key can be applied to the given edge and app id.
	 *
	 * @param key    the key to be validated
	 * @param edgeId the edge the app gets installed
	 * @param appId  the app that gets installed
	 * @return a future with the result as a {@link JsonObject}
	 */
	public CompletableFuture<JsonObject> sendIsKeyApplicable(String key, String edgeId, String appId);

	/**
	 * Sends a request to get all apps that can be installed with the given key.
	 *
	 * @param key    the apps of the key
	 * @param edgeId the apps on which edge
	 * @return a future with the bundles and their apps
	 */
	public CompletableFuture<JsonArray> sendGetPossibleApps(String key, String edgeId);

	@ProviderType
	public static interface EdgeData extends AppCenterMetadata {

		/**
		 * Sends a request to add a install app history entry.
		 *
		 * @param key        the key that the app gets installed with
		 * @param edgeId     the edge the app gets installed on
		 * @param appId      the app that gets installed
		 * @param instanceId the instanceId of the installed app
		 * @param userId     the user who added the instance
		 * @return a future
		 */
		public CompletableFuture<Void> sendAddInstallAppInstanceHistory(String key, String edgeId, String appId,
				UUID instanceId, String userId);

		/**
		 * Sends a request to add a deinstall history entry.
		 *
		 * @param edgeId     the edge the instance gets removed
		 * @param appId      the id of the app
		 * @param instanceId the instanceId of the removed instance
		 * @param userId     the user who removed the instance
		 * @return a future
		 */
		public CompletableFuture<Void> sendAddDeinstallAppInstanceHistory(String edgeId, String appId, UUID instanceId,
				String userId);

		/**
		 * Sends a request to get all installed apps on the edge that are logged in the
		 * backend these apps may not be actually on the edge.
		 *
		 * @param edgeId the apps on which edge
		 * @return a future with the installed apps
		 */
		public CompletableFuture<JsonObject> sendGetInstalledApps(String edgeId);

	}

	@ProviderType
	public static interface UiData extends AppCenterMetadata {

		/**
		 * Sends a request to register a key.
		 *
		 * @param edgeId the edge the key gets registered to
		 * @param appId  the appId that gets registered
		 * @param key    the key to register to the app
		 * @param user   the user who added the registration
		 * @return a future
		 */
		public CompletableFuture<Void> sendAddRegisterKeyHistory(String edgeId, String appId, String key, User user);

		/**
		 * Sends a request to unregister a key.
		 * 
		 * @param edgeId the edge
		 * @param appId  the id of the app
		 * @param key    the key
		 * @param user   the user who removed the registration
		 * @return a future
		 */
		public CompletableFuture<Void> sendAddUnregisterKeyHistory(String edgeId, String appId, String key, User user);

		/**
		 * Sends a request to get all registered keys to the given edge and app.
		 *
		 * @param edgeId the edge the registered key can be applied on
		 * @param appId  the app of the registed key
		 * @return a future with the result as a {@link JsonObject}
		 */
		public CompletableFuture<JsonArray> sendGetRegisteredKeys(String edgeId, String appId);

		/**
		 * Gets a key that can be supplied to the installation of the given app.
		 * 
		 * @param user   the requested user
		 * @param edgeId the edge to install the app on
		 * @param appId  the app to install
		 * @return the key or null if none can be supplied
		 */
		public CompletableFuture<String> getSuppliableKey(User user, String edgeId, String appId);

		/**
		 * Gets if the given app is free.
		 * 
		 * @param user  the requested user
		 * @param appId the id of the app
		 * @return true if the app is free
		 */
		public CompletableFuture<Boolean> isAppFree(User user, String appId);

	}

}