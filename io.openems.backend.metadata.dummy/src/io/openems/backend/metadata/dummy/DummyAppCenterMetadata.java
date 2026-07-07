package io.openems.backend.metadata.dummy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AppCenterMetadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.utils.JsonUtils;

@Component
public class DummyAppCenterMetadata implements AppCenterMetadata.EdgeData, AppCenterMetadata.UiData, AppCenterMetadata {

	private static final String MASTER_KEY = "0000-0000-0000-0000";

	@Reference
	private MetadataDummy metadata;

	@Override
	public CompletableFuture<JsonObject> sendIsKeyApplicable(String key, String edgeId, String appId) {
		return CompletableFuture.completedFuture(JsonUtils.buildJsonObject() //
				.addProperty("isKeyApplicable", true) //
				.add("additionalInfo", JsonUtils.buildJsonObject() //
						.addProperty("keyId", key) //
						.add("bundles", JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonArray() //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("appId", "App.Evcs.Keba") //
												.onlyIf(appId != null, b -> b.addProperty("appId", appId)) //
												.build())
										.build())
								.build()) //
						.add("registrations", JsonUtils.buildJsonArray() //
								.build()) //
						.add("usages", JsonUtils.buildJsonArray() //
								.build()) //
						.build())
				.build());
	}

	@Override
	public CompletableFuture<JsonArray> sendGetPossibleApps(String key, String edgeId) {
		if (MASTER_KEY.equals(key)) {
			return CompletableFuture.completedFuture(JsonUtils.buildJsonArray() //
					.add(JsonUtils.buildJsonArray() //
							.build()) //
					.build());
		}
		return CompletableFuture.completedFuture(JsonUtils.buildJsonArray() //
				.add(JsonUtils.buildJsonArray() //
						.add(JsonUtils.buildJsonObject() //
								.addProperty("appId", "App.Evcs.Keba") //
								.build()) //
						.build()) //
				.build());
	}

	@Override
	public CompletableFuture<Void> sendAddRegisterKeyHistory(String edgeId, String appId, String key, User user) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> sendAddUnregisterKeyHistory(String edgeId, String appId, String key, User user) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<JsonArray> sendGetRegisteredKeys(String edgeId, String appId) {
		return CompletableFuture.completedFuture(JsonUtils.buildJsonArray() //
				.add(JsonUtils.buildJsonObject() //
						.addProperty("keyId", MASTER_KEY) //
						.add("bundles", JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonArray() //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("id", 1) //
												.addProperty("appId", "App.Evcs.Keba") //
												.build()) //
										.build())
								.build())
						.build()) //
				.build());
	}

	@Override
	public CompletableFuture<String> getSuppliableKey(User user, String edgeId, String appId) {
		return CompletableFuture.completedFuture(MASTER_KEY);
	}

	@Override
	public CompletableFuture<Boolean> isAppFree(User user, String appId) {
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public CompletableFuture<Void> sendAddInstallAppInstanceHistory(String key, String edgeId, String appId,
			UUID instanceId, String userId) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> sendAddDeinstallAppInstanceHistory(String edgeId, String appId, UUID instanceId,
			String userId) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<JsonObject> sendGetInstalledApps(String edgeId) {
		return CompletableFuture.completedFuture(JsonUtils.buildJsonObject() //
				.add("installedApps", JsonUtils.buildJsonArray() //
						.build())
				.build());
	}

}
