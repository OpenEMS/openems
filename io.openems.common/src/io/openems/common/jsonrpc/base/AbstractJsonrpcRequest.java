package io.openems.common.jsonrpc.base;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * This represents a JsonrpcRequest. It could either be a {@link JsonrpcRequest}
 * or a {@link JsonrpcNotification}.
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC
 *      specification</a>
 */
public abstract class AbstractJsonrpcRequest extends JsonrpcMessage {

	private final String method;

	public AbstractJsonrpcRequest(String method) {
		this.method = method;
	}

	public String getMethod() {
		return this.method;
	}

	/**
	 * Returns the complete method name, up to the innermost payload.
	 * 
	 * @return combined method names
	 */
	public String getFullyQualifiedMethod() {
		StringBuilder sb = new StringBuilder(100);
		JsonObject json = this.toJsonObject();

		sb.append(this.method);

		while (true) {
			var paramsOpt = JsonUtils.getAsOptionalJsonObject(json, "params");
			if (paramsOpt.isEmpty()) {
				break;
			}
			var payloadOpt = JsonUtils.getAsOptionalJsonObject(paramsOpt.get(), "payload");
			if (payloadOpt.isEmpty()) {
				break;
			}
			json = payloadOpt.get();
			var methodOpt = JsonUtils.getAsOptionalString(json, "method");
			if (methodOpt.isEmpty()) {
				break;
			}
			sb.append('/').append(methodOpt.get());
		}

		return sb.toString();
	}

	/**
	 * Gets the params {@link JsonObject} of the {@link JsonrpcRequest}.
	 *
	 * @return the params as {@link JsonObject}
	 */
	public abstract JsonObject getParams();

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("method", this.method) //
				.add("params", this.getParams()) //
				.build();
	}
}