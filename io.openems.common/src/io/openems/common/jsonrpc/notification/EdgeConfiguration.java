package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
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
public class EdgeConfiguration extends GenericJsonrpcNotification {

	public static EdgeConfiguration from(JsonObject j) throws OpenemsException {
		return from(GenericJsonrpcNotification.from(j));
	}

	public static EdgeConfiguration from(JsonrpcNotification r) throws OpenemsException {
		return new EdgeConfiguration(r.getParams());
	}

	public final static String METHOD = "edgeConfiguration";

	public EdgeConfiguration(JsonObject params) {
		super(METHOD, params);
	}

}
