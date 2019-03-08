package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;

/**
 * Represents a JSON-RPC Notification for when UI authentication with session_id
 * failed.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionIdFailed",
 *   "params": {}
 * }
 * </pre>
 */
public class AuthenticateWithSessionIdFailedNotification extends JsonrpcNotification {

	public final static String METHOD = "authenticatedWithSessionIdFailed";

	public AuthenticateWithSessionIdFailedNotification() {
		super(METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
