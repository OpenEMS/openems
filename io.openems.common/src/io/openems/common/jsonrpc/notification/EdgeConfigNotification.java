package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.EdgeConfig;

/**
 * Represents a JSON-RPC Notification for OpenEMS Edge configuration.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeConfig",
 *   "params": {
 *     {@link EdgeConfig#toJson()}   
 *   }
 * }
 * </pre>
 */
public class EdgeConfigNotification extends JsonrpcNotification {

	public static EdgeConfigNotification from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcNotification.from(j));
	}

	public static EdgeConfigNotification from(JsonrpcNotification r) throws OpenemsNamedException {
		EdgeConfig config = EdgeConfig.fromJson(r.getParams());
		return new EdgeConfigNotification(config);
	}

	public final static String METHOD = "edgeConfig";

	private final EdgeConfig config;

	public EdgeConfigNotification(EdgeConfig config) {
		super(METHOD);
		this.config = config;
	}

	@Override
	public JsonObject getParams() {
		return this.config.toJson();
	}

	public EdgeConfig getConfig() {
		return config;
	}

}
