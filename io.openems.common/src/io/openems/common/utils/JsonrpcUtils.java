package io.openems.common.utils;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcMessage;

public class JsonrpcUtils {

	/**
	 * Simplifies a {@link JsonrpcMessage} by recursively removing unnecessary
	 * elements "jsonrpc" and "id".
	 * 
	 * <p>
	 * Be aware that this actually changes the JsonObject. It does not work on a
	 * copy of the object!
	 *
	 * @param message the {@link JsonrpcMessage}
	 * @return the simplified {@link JsonObject}
	 */
	public static JsonObject simplifyJsonrpcMessage(JsonrpcMessage message) {
		return simplifyJsonrpcMessage(message.toJsonObject());
	}

	private static JsonObject simplifyJsonrpcMessage(JsonObject j) {
		if (j.has("jsonrpc")) {
			j.remove("jsonrpc");
			j.remove("id");
		}
		var paramsOpt = JsonUtils.getAsOptionalJsonObject(j, "params");
		if (paramsOpt.isPresent()) {
			var params = paramsOpt.get();
			// hide passwords
			var password = params.get("password");
			if (password != null && password.isJsonPrimitive()) {
				params.addProperty("password", "HIDDEN");
			}

			// recursive call for payload
			var payloadOpt = JsonUtils.getAsOptionalJsonObject(params, "payload");
			if (payloadOpt.isPresent()) {
				simplifyJsonrpcMessage(payloadOpt.get());
			}
		}
		return j;
	}
}
