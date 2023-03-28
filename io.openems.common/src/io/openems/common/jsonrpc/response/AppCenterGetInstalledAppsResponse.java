package io.openems.common.jsonrpc.response;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for getting all installed apps that are
 * defined in the backend metadata.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "installedApps": {@link Instance}[]
 *   }
 * }
 * </pre>
 */
public class AppCenterGetInstalledAppsResponse extends JsonrpcResponseSuccess {

	public final List<Instance> installedApps;

	/**
	 * Creates a {@link AppCenterGetInstalledAppsResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 *
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link AppCenterGetInstalledAppsResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterGetInstalledAppsResponse from(JsonrpcResponseSuccess r) throws OpenemsNamedException {
		final var result = r.getResult();
		return AppCenterGetInstalledAppsResponse.from(r.getId(), result);
	}

	/**
	 * Creates a {@link AppCenterGetInstalledAppsResponse} from a
	 * {@link JsonObject}.
	 *
	 * @param id         the id of the request
	 * @param jsonObject the {@link JsonObject} to parse
	 * @return a {@link AppCenterGetInstalledAppsResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterGetInstalledAppsResponse from(UUID id, JsonObject jsonObject)
			throws OpenemsNamedException {
		final var installedAppsJsonArray = JsonUtils.getAsJsonArray(jsonObject, "installedApps");
		final var installedApps = AppCenterGetInstalledAppsResponse.getInstancesFrom(installedAppsJsonArray);

		return new AppCenterGetInstalledAppsResponse(id, installedApps);
	}

	private static final List<Instance> getInstancesFrom(JsonArray a) {
		return JsonUtils.stream(a) //
				.map(JsonElement::getAsJsonObject) //
				.map(Instance::from) //
				.toList();
	}

	public AppCenterGetInstalledAppsResponse(UUID id, List<Instance> installedApps) {
		super(id);
		this.installedApps = installedApps;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("installedApps", this.installedApps.stream() //
						.map(Instance::toJsonObject) //
						.collect(JsonUtils.toJsonArray())) //
				.build();
	}

	/**
	 * Represents a OpenemsAppInstance from a
	 * {@link AppCenterGetInstalledAppsResponse}.
	 * 
	 * <pre>
	 * { 
	 *   "appId": "App.Api.ModbusTcp.ReadOnly", 
	 *   "instanceId": {@link UUID} 
	 * }
	 * </pre>
	 */
	public static final class Instance {
		public final String appId;
		public final UUID instanceId;

		/**
		 * Creates a {@link Instance} from a {@link JsonObject}.
		 *
		 * @param jsonObject the {@link JsonObject} to parse.
		 * @return {@link Instance}
		 */
		public static final Instance from(JsonObject jsonObject) {
			final var appId = jsonObject.get("appId").getAsString();
			final var instanceId = JsonUtils.getAsOptionalUUID(jsonObject, "instanceId").orElse(null);

			return new Instance(appId, instanceId);
		}

		public Instance(String appId, UUID instanceId) {
			this.appId = appId;
			this.instanceId = instanceId;
		}

		/**
		 * Gets this object as a {@link JsonObject}.
		 *
		 * @return the {@link JsonObject}
		 */
		public final JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("appId", this.appId) //
					.addProperty("instanceId", this.instanceId.toString()) //
					.build();
		}
	}

}
