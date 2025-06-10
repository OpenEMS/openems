package io.openems.common.jsonrpc.base;

import com.google.gson.JsonObject;

/**
 * Represents a wrapper around JSON-RPC Notification for compatibility with
 * deprecated OpenEMS Edge.
 *
 * <pre>
 * { "..." }
 * </pre>
 */
public class DeprecatedJsonrpcNotification extends JsonrpcNotification {

	private final JsonObject jMessage;

	@Deprecated
	public DeprecatedJsonrpcNotification(JsonObject jMessage) {
		super("");
		this.jMessage = jMessage;
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

	@Override
	public JsonObject toJsonObject() {
		return this.jMessage;
	}

}
