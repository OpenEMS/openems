package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'getMeters'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     'schedule': [{
 *     	'timestamp':...
 *      'price':...
 *      'state':...
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetScheduleResponse extends JsonrpcResponseSuccess {

	private final JsonArray schedule;

	public GetScheduleResponse(JsonArray schedule) {
		this(UUID.randomUUID(), schedule);
	}

	public GetScheduleResponse(UUID id, JsonArray schedule) {
		super(id);
		this.schedule = schedule;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("schedule", this.schedule) //
				.build();
	}

}
