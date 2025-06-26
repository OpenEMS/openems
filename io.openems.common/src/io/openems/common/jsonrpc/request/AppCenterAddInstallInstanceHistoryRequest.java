package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to add a install app history entry.
 *
 * <p>
 * NOTE: in order to get this request handled by the backend the request needs
 * to be wrapped in a {@link AppCenterRequest}.
 * 
 * <p>
 * This is used by Edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "addInstallInstanceHistory",
 *   "params": {
 *     "key": String,
 *     "appId": String,
 *     "instanceId": UUID,
 *     "userId": String
 *   }
 * }
 * </pre>
 */
public class AppCenterAddInstallInstanceHistoryRequest extends JsonrpcRequest {

	public static final String METHOD = "addInstallInstanceHistory";

	/**
	 * Creates a {@link AppCenterAddInstallInstanceHistoryRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterAddInstallInstanceHistoryRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterAddInstallInstanceHistoryRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterAddInstallInstanceHistoryRequest(r, //
				JsonUtils.getAsString(p, "key"), //
				JsonUtils.getAsString(p, "appId"), //
				JsonUtils.getAsUUID(p, "instanceId"), //
				JsonUtils.getAsOptionalString(p, "userId").orElse(null) //
		);
	}

	public final String key;
	public final String appId;
	public final UUID instanceId;
	public final String userId;

	private AppCenterAddInstallInstanceHistoryRequest(JsonrpcRequest request, String key, String appId, UUID instanceId,
			String userId) {
		super(request, AppCenterAddInstallInstanceHistoryRequest.METHOD);
		this.key = key;
		this.appId = appId;
		this.instanceId = instanceId;
		this.userId = userId;
	}

	public AppCenterAddInstallInstanceHistoryRequest(String key, String appId, UUID instanceId, String userId) {
		super(AppCenterAddInstallInstanceHistoryRequest.METHOD);
		this.key = key;
		this.appId = appId;
		this.instanceId = instanceId;
		this.userId = userId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", this.key) //
				.addProperty("appId", this.appId) //
				.addProperty("instanceId", this.instanceId.toString()) //
				.addPropertyIfNotNull("userId", this.userId) //
				.build();
	}

}
