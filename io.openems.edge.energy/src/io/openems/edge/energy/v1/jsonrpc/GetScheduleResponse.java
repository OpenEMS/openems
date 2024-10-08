package io.openems.edge.energy.v1.jsonrpc;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.energy.v1.optimizer.ScheduleDatas;
import io.openems.edge.energy.v1.optimizer.ScheduleDatas.ScheduleData;

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
	private final ZonedDateTime toDate;
	private final ScheduleDatas scheduleDatas;

	public GetScheduleResponse(ZonedDateTime fromDate, ZonedDateTime toDate, ScheduleDatas scheduleDatas) {
		this(UUID.randomUUID(), fromDate, toDate, scheduleDatas);
	}

	public GetScheduleResponse(UUID id, ZonedDateTime fromDate, ZonedDateTime toDate, ScheduleDatas scheduleDatas) {
		super(id);
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.scheduleDatas = scheduleDatas;
	}

	@Override
	public JsonObject getResult() {
		var s = new TreeMap<ZonedDateTime, JsonObject>();
		var t = this.fromDate;
		// Prefill with empty objects
		while (!t.isAfter(this.toDate)) {
			s.put(t, ScheduleData.emptyJsonObject(t));
			t = t.plusMinutes(15);
		}
		// Replace with actual data
		s.putAll(this.scheduleDatas.toJsonObjects());

		return JsonUtils.buildJsonObject() //
				.add("schedule", s.entrySet().stream() //
						.map(Entry::getValue) //
						.collect(JsonUtils.toJsonArray())) //
				.build();
	}

}
