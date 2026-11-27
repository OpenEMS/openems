package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.type.ScheduleUpdate;

/**
 * Represents a JSON-RPC Notification for sending the current energy schedule
 * together with the current optimization status.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "schedule",
 *   "params": {
 *     "status": "OPTIMIZING_FUTURE",
 *     "startedAt": "2026-09-17T08:00:00Z",
 *     "reason": "Start of new period",
 *     "data": [
 *       {
 *         "timestamp": "2026-09-17T08:00:00Z",
 *         "type": "PREDICTION",
 *         "_sum": {
 *           ...
 *         },
 *         "eshs": [
 *           ...
 *         ]
 *       }
 *     ]
 *   }
 * }
 * </pre>
 */
public class ScheduleNotification extends JsonrpcNotification {

	public static final String METHOD = "schedule";

	private final ScheduleUpdate scheduleUpdate;

	public ScheduleNotification(ScheduleUpdate scheduleUpdate) {
		super(METHOD);
		this.scheduleUpdate = scheduleUpdate;
	}

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link ScheduleNotification}.
	 *
	 * @param notification the {@link JsonrpcNotification}
	 * @return the {@link ScheduleNotification}
	 */
	public static ScheduleNotification from(JsonrpcNotification notification) {
		final var scheduleUpdate = ScheduleUpdate.serializer()//
				.deserialize(notification.getParams());

		return new ScheduleNotification(scheduleUpdate);
	}

	@Override
	public JsonObject getParams() {
		return ScheduleUpdate.serializer()//
				.serialize(this.scheduleUpdate)//
				.getAsJsonObject();
	}
}
