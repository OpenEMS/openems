package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

	/**
	 * Create {@link SetGridConnScheduleRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SetGridConnScheduleRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SetGridConnScheduleRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var edgeId = JsonUtils.getAsString(p, "id");
		var s = JsonUtils.getAsJsonArray(p, "schedule");
		var schedule = GridConnSchedule.from(s);
		return new SetGridConnScheduleRequest(r, edgeId, schedule);
	}

	public static final String METHOD = "setGridConnSchedule";

	private final String edgeId;
	private final List<GridConnSchedule> schedule;

	public SetGridConnScheduleRequest(String edgeId) {
		this(edgeId, new ArrayList<>());
	}

	public SetGridConnScheduleRequest(String edgeId, List<GridConnSchedule> schedule) {
		super(SetGridConnScheduleRequest.METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	private SetGridConnScheduleRequest(JsonrpcRequest request, String edgeId, List<GridConnSchedule> schedule) {
		super(request, SetGridConnScheduleRequest.METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	/**
	 * Add a {@link GridConnSchedule} entry.
	 *
	 * @param scheduleEntry GridConnSchedule entry
	 */
	public void addScheduleEntry(GridConnSchedule scheduleEntry) {
		this.schedule.add(scheduleEntry);
	}

	@Override
	public JsonObject getParams() {
		var schedule = new JsonArray();
		for (GridConnSchedule se : this.schedule) {
			schedule.add(se.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.getEdgeId()) //
				.add("schedule", schedule) //
				.build();
	}

	/**
	 * Gets the Edge-ID.
	 *
	 * @return Edge-ID
	 */
	public String getEdgeId() {
		return this.edgeId;
	}

	/**
	 * Gets the list of {@link GridConnSchedule} entries.
	 *
	 * @return entries
	 */
	public List<GridConnSchedule> getSchedule() {
		return this.schedule;
	}

	public static class GridConnSchedule {

		/**
		 * Create a list of {@link GridConnSchedule}s from a {@link JsonArray}.
		 *
		 * @param j the {@link JsonArray}
		 * @return the list of {@link GridConnSchedule}s
		 * @throws OpenemsNamedException on parse error
		 */
		public static List<GridConnSchedule> from(JsonArray j) throws OpenemsNamedException {
			List<GridConnSchedule> schedule = new ArrayList<>();
			for (JsonElement se : j) {
				var startTimestamp = JsonUtils.getAsLong(se, "startTimestamp");
				var duration = JsonUtils.getAsInt(se, "duration");
				var activePowerSetPoint = JsonUtils.getAsInt(se, "activePowerSetPoint");
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

		/**
		 * Gets the start timestamp in epoch seconds.
		 *
		 * @return start timestamp
		 */
		public long getStartTimestamp() {
			return this.startTimestamp;
		}

		/**
		 * Gets the duration in seconds.
		 *
		 * @return duration
		 */
		public int getDuration() {
			return this.duration;
		}

		/**
		 * Gets the Active-Power Setpoint.
		 *
		 * @return the setpoint
		 */
		public int getActivePowerSetPoint() {
			return this.activePowerSetPoint;
		}

		protected JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("startTimestamp", this.getStartTimestamp()) //
					.addProperty("duration", this.getDuration()) //
					.addProperty("activePowerSetPoint", this.activePowerSetPoint) //
					.build();
		}
	}
}
