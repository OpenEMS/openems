package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Request from an app center request.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "appCenter",
 *   "params": {
 *     "payload": {@link JsonrpcRequest}
 *   }
 * }
 * </pre>
 */
public class OAuthRegistryRequest extends JsonrpcRequest {

	public static final String METHOD = "oauthRegistry";

	/**
	 * Create {@link OAuthRegistryRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link OAuthRegistryRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static OAuthRegistryRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new OAuthRegistryRequest(r, payload);
	}

	private final JsonrpcRequest payload;

	public OAuthRegistryRequest(JsonrpcRequest payload) {
		super(OAuthRegistryRequest.METHOD);
		this.payload = payload;
	}

	public OAuthRegistryRequest(JsonrpcRequest request, JsonrpcRequest payload) {
		super(request, OAuthRegistryRequest.METHOD);
		this.payload = payload;
	}

	/**
	 * Gets the Payload {@link JsonrpcRequest}.
	 *
	 * @return Payload
	 */
	public JsonrpcRequest getPayload() {
		return this.payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}
}
