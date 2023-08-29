package io.openems.common.jsonrpc.notification;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

/**
 * Represents a JSON-RPC Notification for timestamped data sent from Edge to
 * Backend.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "timestampedData",
 *   "params": {
 *     [timestamp: epoch in milliseconds]: {
 *       [channelAddress]: {@link JsonElement}
 *     }
 *   }
 * }
 * </pre>
 */
public class TimestampedDataNotification extends AbstractDataNotification {

	public static final String METHOD = "timestampedData";

	/**
	 * Parses a {@link JsonrpcNotification} to a
	 * {@link TimestampedDataNotification}.
	 *
	 * @param notification the {@link JsonrpcNotification}
	 * @return the {@link TimestampedDataNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static TimestampedDataNotification from(JsonrpcNotification notification) throws OpenemsNamedException {
		return new TimestampedDataNotification(parseParams(notification.getParams()));
	}

	public TimestampedDataNotification(TreeBasedTable<Long, String, JsonElement> data) {
		super(TimestampedDataNotification.METHOD, data);
	}

	public TimestampedDataNotification() {
		super(TimestampedDataNotification.METHOD, TreeBasedTable.create());
	}

}
