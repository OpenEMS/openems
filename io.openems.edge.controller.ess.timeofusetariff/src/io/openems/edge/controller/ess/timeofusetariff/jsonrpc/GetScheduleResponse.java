package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import java.time.ZonedDateTime;
import java.util.UUID;

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
 *      'timestamp':...,
 *      'price':...,
 *      'state':...,
 *      'grid':...,
 *      'production':...,
 *      'consumption':...,
 *      'ess':...,
 *      'soc':...,
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetScheduleResponse extends JsonrpcResponseSuccess {

	private final ZonedDateTime fromDate;
	private final ScheduleDatas scheduleDatas;

	public GetScheduleResponse(ZonedDateTime fromDate, ScheduleDatas scheduleDatas) {
		this(UUID.randomUUID(), fromDate, scheduleDatas);
	}

	public GetScheduleResponse(UUID id, ZonedDateTime fromDate, ScheduleDatas scheduleDatas) {
		super(id);
		this.scheduleDatas = scheduleDatas;
		this.fromDate = fromDate;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("schedule", this.scheduleDatas.toJsonArray(this.fromDate)) //
				.build();
	}

}
