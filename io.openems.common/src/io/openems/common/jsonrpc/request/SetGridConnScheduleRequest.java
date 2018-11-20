package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'setGridConnSchedule'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setGridConnSchedule",
 *   "params": {
 *     "id": "edgeId",
 *     "schedule": [{
 *       "startTimestamp": 1542464697, // epoch in seconds
 *       "duration": 900, // in seconds
 *       "gridConnSetPoint": 0 // in Watt
 *     }]
 *   }
 * }
 * </pre>
 */
public class SetGridConnScheduleRequest extends JsonrpcRequest {

	public static SetGridConnScheduleRequest from(JsonrpcRequest r) throws OpenemsException {
		JsonObject p = r.getParams();
		String edgeId = JsonUtils.getAsString(p, "id");
		JsonArray s = JsonUtils.getAsJsonArray(p, "schedule");
		List<GridConnSchedule> schedule = new ArrayList<>();
		for (JsonElement se : s) {
			long startTimestamp = JsonUtils.getAsLong(se, "startTimestamp");
			int duration = JsonUtils.getAsInt(se, "duration");
			int gridConnSetPoint = JsonUtils.getAsInt(se, "gridConnSetPoint");
			schedule.add(new GridConnSchedule(startTimestamp, duration, gridConnSetPoint));
		}
		return new SetGridConnScheduleRequest(r.getId(), edgeId, schedule);
	}

	public final static String METHOD = "setGridConnSchedule";

	private final String edgeId;
	private final List<GridConnSchedule> schedule;

	public SetGridConnScheduleRequest(String edgeId) {
		this(UUID.randomUUID(), edgeId, new ArrayList<>());
	}

	public SetGridConnScheduleRequest(String edgeId, List<GridConnSchedule> schedule) {
		this(UUID.randomUUID(), edgeId, schedule);
	}

	public SetGridConnScheduleRequest(UUID id, String edgeId, List<GridConnSchedule> schedule) {
		super(id, METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	public void addScheduleEntry(GridConnSchedule scheduleEntry) {
		this.schedule.add(scheduleEntry);
	}

	@Override
	public JsonObject getParams() {
		JsonArray schedule = new JsonArray();
		for (GridConnSchedule se : this.schedule) {
			schedule.add(JsonUtils.buildJsonObject() //
					.addProperty("startTimestamp", se.getStartTimestamp()) //
					.addProperty("duration", se.getDuration()) //
					.addProperty("gridConnSetPoint", se.gridConnSetPoint) //
					.build());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.getEdgeId()) //
				.add("schedule", schedule) //
				.build();
	}

	public String getEdgeId() {
		return edgeId;
	}

	public List<GridConnSchedule> getSchedule() {
		return schedule;
	}

	public static class GridConnSchedule {
		private final long startTimestamp;
		private final int duration;
		private final int gridConnSetPoint;

		/**
		 * @param startTimestamp   epoch in seconds
		 * @param duration         in seconds
		 * @param gridConnSetPoint in Watt
		 */
		public GridConnSchedule(long startTimestamp, int duration, int gridConnSetPoint) {
			this.startTimestamp = startTimestamp;
			this.duration = duration;
			this.gridConnSetPoint = gridConnSetPoint;
		}

		public long getStartTimestamp() {
			return startTimestamp;
		}

		public int getDuration() {
			return duration;
		}

		public int getGridConnSetPoint() {
			return gridConnSetPoint;
		}
	}
}
