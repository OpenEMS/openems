package io.openems.common.jsonrpc.notification;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

/**
 * Represents a JSON-RPC Notification for aggregatedData data sent from Edge to
 * Backend.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "aggregatedData",
 *   "params": {
 *     [timestamp: epoch in milliseconds]: {
 *       [channelAddress]: {@link JsonElement}
 *     }
 *   }
 * }
 * </pre>
 */
public class AggregatedDataNotification extends AbstractDataNotification {

	public static final String METHOD = "aggregatedData";

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link AggregatedDataNotification}.
	 *
	 * @param notification the {@link JsonrpcNotification}
	 * @return the {@link AggregatedDataNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static AggregatedDataNotification from(JsonrpcNotification notification) throws OpenemsNamedException {
		return new AggregatedDataNotification(parseParams(notification.getParams()));
	}

	public AggregatedDataNotification(TreeBasedTable<Long, String, JsonElement> data) {
		super(AggregatedDataNotification.METHOD, data);
	}

	public AggregatedDataNotification() {
		super(AggregatedDataNotification.METHOD, TreeBasedTable.create());
	}

}
