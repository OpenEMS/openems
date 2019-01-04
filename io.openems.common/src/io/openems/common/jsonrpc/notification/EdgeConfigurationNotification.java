package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

/**
 * Represents a JSON-RPC Notification for OpenEMS Edge configuration.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeConfiguration",
 *   "params": { ... }
 *   }
 * }
 * </pre>
 */
public class EdgeConfigurationNotification extends GenericJsonrpcNotification {

	public static EdgeConfigurationNotification from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcNotification.from(j));
	}

	public static EdgeConfigurationNotification from(JsonrpcNotification r) throws OpenemsNamedException {
		return new EdgeConfigurationNotification(r.getParams());
	}

	public final static String METHOD = "edgeConfiguration";

	public EdgeConfigurationNotification(JsonObject params) {
		super(METHOD, params);
	}

}
