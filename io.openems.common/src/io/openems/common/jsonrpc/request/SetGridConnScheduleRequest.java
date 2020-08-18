package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
 *       "activePowerSetPoint": 0 // in Watt
 *     }]
 *   }
 * }
 * </pre>
 */
public class SetGridConnScheduleRequest extends JsonrpcRequest {

	public static SetGridConnScheduleRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String edgeId = JsonUtils.getAsString(p, "id");
		JsonArray s = JsonUtils.getAsJsonArray(p, "schedule");
		List<GridConnSchedule> schedule = GridConnSchedule.from(s);
		return new SetGridConnScheduleRequest(r.getId(), edgeId, schedule);
	}

	public static final String METHOD = "setGridConnSchedule";

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
			schedule.add(se.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.getEdgeId()) //
				.add("schedule", schedule) //
				.build();
	}

	public String getEdgeId() {
		return this.edgeId;
	}

	public List<GridConnSchedule> getSchedule() {
		return this.schedule;
	}

	public static class GridConnSchedule {

		public static List<GridConnSchedule> from(JsonArray j) throws OpenemsNamedException {
			List<GridConnSchedule> schedule = new ArrayList<>();
			for (JsonElement se : j) {
				long startTimestamp = JsonUtils.getAsLong(se, "startTimestamp");
				int duration = JsonUtils.getAsInt(se, "duration");
				int activePowerSetPoint = JsonUtils.getAsInt(se, "activePowerSetPoint");
				schedule.add(new GridConnSchedule(startTimestamp, duration, activePowerSetPoint));
			}
			schedule.sort(Comparator.comparing(GridConnSchedule::getStartTimestamp).reversed());
			return schedule;
		}

		private final long startTimestamp;
		private final int duration;
		private final int activePowerSetPoint;

		/**
		 * Construct an instance of {@link GridConnSchedule}.
		 * 
		 * @param startTimestamp      epoch in seconds
		 * @param duration            in seconds
		 * @param activePowerSetPoint in Watt
		 */
		public GridConnSchedule(long startTimestamp, int duration, int activePowerSetPoint) {
			this.startTimestamp = startTimestamp;
			this.duration = duration;
			this.activePowerSetPoint = activePowerSetPoint;
		}

		public long getStartTimestamp() {
			return this.startTimestamp;
		}

		public int getDuration() {
			return this.duration;
		}

		public int getActivePowerSetPoint() {
			return this.activePowerSetPoint;
		}

		public JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("startTimestamp", this.getStartTimestamp()) //
					.addProperty("duration", this.getDuration()) //
					.addProperty("activePowerSetPoint", this.activePowerSetPoint) //
					.build();
		}
	}
}
