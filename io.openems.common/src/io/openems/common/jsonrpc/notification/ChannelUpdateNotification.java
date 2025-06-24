package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for OpenEMS Channel update.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "channelUpdate",
 *   "params": {
 *   
 *   }
 * }
 * </pre>
 */
public class ChannelUpdateNotification extends JsonrpcNotification {

	/**
	 * Parses a {@link JsonObject} to a {@link ChannelUpdateNotification}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link ChannelUpdateNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static ChannelUpdateNotification from(JsonObject j) throws OpenemsNamedException {
		return ChannelUpdateNotification.from(GenericJsonrpcNotification.from(j));
	}

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link ChannelUpdateNotification}.
	 *
	 * @param n the {@link JsonrpcNotification}
	 * @return the {@link ChannelUpdateNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static ChannelUpdateNotification from(JsonrpcNotification n) throws OpenemsNamedException {
		return new ChannelUpdateNotification();
	}

	public static final String METHOD = "channelUpdate";

	public ChannelUpdateNotification() {
		super(ChannelUpdateNotification.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.build();
	}

}
