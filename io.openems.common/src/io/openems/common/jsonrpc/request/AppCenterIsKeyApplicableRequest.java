package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to determine if a key can be applied.
 *
 * <p>
 * NOTE: in order to get this request handled by the backend the request needs
 * to be wrapped in a {@link AppCenterRequest}.
 * 
 * <p>
 * This is used by Edge and UI.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "isKeyApplicable",
 *   "params": {
 *     "key": String,
 *     "appId": String
 *   }
 * }
 * </pre>
 */
public class AppCenterIsKeyApplicableRequest extends JsonrpcRequest {

	public static final String METHOD = "isKeyApplicable";

	/**
	 * Creates a {@link AppCenterIsKeyApplicableRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterIsKeyApplicableRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterIsKeyApplicableRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterIsKeyApplicableRequest(r, //
				JsonUtils.getAsString(p, "key"), //
				JsonUtils.getAsOptionalString(p, "appId").orElse(null) //
		);
	}

	public final String key;
	public final String appId;

	private AppCenterIsKeyApplicableRequest(JsonrpcRequest request, String key, String appId) {
		super(request, AppCenterIsKeyApplicableRequest.METHOD);
		this.key = key;
		this.appId = appId;
	}

	public AppCenterIsKeyApplicableRequest(String key, String appId) {
		super(AppCenterIsKeyApplicableRequest.METHOD);
		this.key = key;
		this.appId = appId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", this.key) //
				.addPropertyIfNotNull("appId", this.appId) //
				.build();
	}

}
