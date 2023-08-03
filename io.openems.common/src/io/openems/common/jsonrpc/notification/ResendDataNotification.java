package io.openems.common.jsonrpc.notification;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

/**
 * Represents a JSON-RPC Notification for resending aggregated data.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "resendData",
 *   "params": {
 *     [channelAddress]: string | number
 *   }
 * }
 * </pre>
 */
public class ResendDataNotification extends AbstractDataNotification {

	public static final String METHOD = "resendData";

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link ResendDataNotification}.
	 *
	 * @param notification the {@link JsonrpcNotification}
	 * @return the {@link ResendDataNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static ResendDataNotification from(JsonrpcNotification notification) throws OpenemsNamedException {
		return new ResendDataNotification(parseParams(notification.getParams()));
	}

	public ResendDataNotification(TreeBasedTable<Long, String, JsonElement> data) {
		super(ResendDataNotification.METHOD, data);
	}

}