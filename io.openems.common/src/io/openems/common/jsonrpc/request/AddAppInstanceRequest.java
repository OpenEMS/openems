package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Adds an OpenemsAppInstance. This is used by Ui to install a app on the edge.
 * 
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "addAppInstance",
 *   "params": {
 *     "appId": string,
 *     "alias": string,
 *     "key": string,
 *     "properties": {}
 *   }
 * }
 * </pre>
 */
public class AddAppInstanceRequest extends JsonrpcRequest {

	public static final String METHOD = "addAppInstance";

	/**
	 * Parses a generic {@link JsonrpcRequest} to a {@link AddAppInstance}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link AddAppInstance} Request
	 * @throws OpenemsNamedException on error
	 */
	public static AddAppInstanceRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AddAppInstanceRequest(r, //
				JsonUtils.getAsOptionalString(p, "key").orElse(null), //
				JsonUtils.getAsString(p, "appId"), //
				JsonUtils.getAsOptionalString(p, "alias").orElse(null), //
				JsonUtils.getAsJsonObject(p, "properties") //
		);
	}

	public final String key;

	public final String appId;
	public final String alias;
	public final JsonObject properties;

	private AddAppInstanceRequest(JsonrpcRequest request, String key, String appId, String alias,
			JsonObject properties) {
		super(request, METHOD);
		this.key = key;
		this.appId = appId;
		this.alias = alias;
		this.properties = properties;
	}

	public AddAppInstanceRequest(String appId, String key, String alias, JsonObject properties) {
		super(METHOD);
		this.key = key;
		this.appId = appId;
		this.alias = alias;
		this.properties = properties;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("appId", this.appId) //
				.addPropertyIfNotNull("alias", this.alias) //
				.addPropertyIfNotNull("key", this.key) //
				.add("properties", this.properties) //
				.build();
	}
}