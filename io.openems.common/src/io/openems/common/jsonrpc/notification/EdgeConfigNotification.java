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

	/**
	 * Parses a {@link JsonObject} to a {@link EdgeConfigNotification}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link EdgeConfigNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static EdgeConfigNotification from(JsonObject j) throws OpenemsNamedException {
		return EdgeConfigNotification.from(GenericJsonrpcNotification.from(j));
	}

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link EdgeConfigNotification}.
	 *
	 * @param n the {@link JsonrpcNotification}
	 * @return the {@link EdgeConfigNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static EdgeConfigNotification from(JsonrpcNotification n) throws OpenemsNamedException {
		var config = EdgeConfig.fromJson(n.getParams());
		return new EdgeConfigNotification(config);
	}

	public static final String METHOD = "edgeConfig";

	private final EdgeConfig config;

	public EdgeConfigNotification(EdgeConfig config) {
		super(EdgeConfigNotification.METHOD);
		this.config = config;
	}

	@Override
	public JsonObject getParams() {
		return this.config.toJson();
	}

	public EdgeConfig getConfig() {
		return this.config;
	}

}
