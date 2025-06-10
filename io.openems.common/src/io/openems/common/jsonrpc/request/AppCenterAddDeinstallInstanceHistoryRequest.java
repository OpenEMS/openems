package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to add a deinstall app history entry.
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
 *   "method": "addDeinstallInstanceHistory",
 *   "params": {
 *     "appId": String,
 *     "instanceId": UUID,
 *     "userId": String
 *   }
 * }
 * </pre>
 */
public class AppCenterAddDeinstallInstanceHistoryRequest extends JsonrpcRequest {

	public static final String METHOD = "addDeinstallInstanceHistory";

	/**
	 * Creates a {@link AppCenterAddDeinstallInstanceHistoryRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link AppCenterAddDeinstallInstanceHistoryRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterAddDeinstallInstanceHistoryRequest from(JsonrpcRequest r)
			throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterAddDeinstallInstanceHistoryRequest(r, //
				JsonUtils.getAsString(p, "appId"), //
				JsonUtils.getAsUUID(p, "instanceId"), //
				JsonUtils.getAsOptionalString(p, "userId").orElse(null) //
		);
	}

	public final String appId;
	public final UUID instanceId;
	public final String userId;

	private AppCenterAddDeinstallInstanceHistoryRequest(JsonrpcRequest request, String appId, UUID instanceId,
			String userId) {
		super(request, AppCenterAddDeinstallInstanceHistoryRequest.METHOD);
		this.appId = appId;
		this.instanceId = instanceId;
		this.userId = userId;
	}

	public AppCenterAddDeinstallInstanceHistoryRequest(String appId, UUID instanceId, String userId) {
		super(AppCenterAddDeinstallInstanceHistoryRequest.METHOD);
		this.appId = appId;
		this.instanceId = instanceId;
		this.userId = userId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("appId", this.appId) //
				.addProperty("instanceId", this.instanceId.toString()) //
				.addPropertyIfNotNull("userId", this.userId) //
				.build();
	}

}
