package io.openems.edge.controller.ess.fastfrequencyreserve.jsonrpc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;

/**
 * Represents a JSON-RPC Request for 'setActivateFastFreqReserve'.
 *
 * <pre>
 {
 	"jsonrpc": "2.0",
 	"id": "UUID",
 	"method": "setActivateFastFreqReserve",
 	"params": { 		
 		"schedule": [{
 			"startTimestamp": 1542464697,
 			"duration": 900,
 			"dischargeActivePowerSetPoint": 92000,
 			"frequencyLimit": 49500
 			"activationRunTime": "LONG_ACTIVATION_RUN",
 			"supportDuration": "LONG_SUPPORT_DURATION"
 		}]
 	}
 }
 * </pre>
 */
public class SetActivateFastFreqReserveRequest extends JsonrpcRequest {

	/**
	 * Create {@link SetActivateFastFreqReserveRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return the {@link SetActivateFastFreqReserveRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SetActivateFastFreqReserveRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		final var params = request.getParams();
		final var edgeId = JsonUtils.getAsString(params, "id");
		final var scheduleArray = JsonUtils.getAsJsonArray(params, "schedule");
		final var schedule = ActivateFastFreqReserveSchedule.from(scheduleArray);
		return new SetActivateFastFreqReserveRequest(request, edgeId, schedule);
	}

	public static final String METHOD = "setActivateFastFreqReserve";

	private final String edgeId;
	private final List<ActivateFastFreqReserveSchedule> schedule;

	public SetActivateFastFreqReserveRequest(String edgeId) {
		this(edgeId, new ArrayList<>());
	}

	private SetActivateFastFreqReserveRequest(String edgeId, List<ActivateFastFreqReserveSchedule> schedule) {
		super(SetActivateFastFreqReserveRequest.METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	private SetActivateFastFreqReserveRequest(JsonrpcRequest request, String edgeId,
			List<ActivateFastFreqReserveSchedule> schedule) {
		super(request, SetActivateFastFreqReserveRequest.METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	/**
	 * Adds a new schedule entry for activating Fast Frequency Reserve.
	 *
	 * @param scheduleEntry The schedule entry to be added.
	 */
	public void addScheduleEntry(ActivateFastFreqReserveSchedule scheduleEntry) {
		this.schedule.add(scheduleEntry);
	}

	@Override
	public JsonObject getParams() {
		var schedule = new JsonArray();
		for (var se : this.schedule) {
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

	public List<ActivateFastFreqReserveSchedule> getSchedule() {
		return this.schedule;
	}

	/**
	 * Converts a list of ActivateFastFreqReserveSchedule objects to a formatted
	 * string.
	 *
	 * @param scheduleList The list of ActivateFastFreqReserveSchedule objects to
	 *                     convert.
	 * @return A string representation of the schedule list.
	 * @see ActivateFastFreqReserveSchedule#toString()
	 */
	public static String listToString(List<ActivateFastFreqReserveSchedule> scheduleList) {
		return "["//
				+ scheduleList.stream()//
						.map(ActivateFastFreqReserveSchedule::toString)//
						.collect(Collectors.joining(", "))//
				+ "]";
	}

	public record ActivateFastFreqReserveSchedule(long startTimestamp, int duration, int dischargePowerSetPoint,
			int frequencyLimit, ActivationTime activationRunTime, SupportDuration supportDuration) {

		/**
		 * Builds a list of ActivateFastFreqReserveSchedule from a JsonArray.
		 *
		 * @param jsonArray JsonArray
		 * @return list of {@link ActivateFastFreqReserveSchedule}
		 * @throws OpenemsNamedException on error
		 */
		public static List<ActivateFastFreqReserveSchedule> from(JsonArray jsonArray) throws OpenemsNamedException {
			List<ActivateFastFreqReserveSchedule> schedule = new ArrayList<>();
			for (var jsonElement : jsonArray) {
				var newSchedule = new ActivateFastFreqReserveSchedule(
						JsonUtils.getAsLong(jsonElement, "startTimestamp"), //
						JsonUtils.getAsInt(jsonElement, "duration"),
						JsonUtils.getAsInt(jsonElement, "dischargePowerSetPoint"),
						JsonUtils.getAsInt(jsonElement, "frequencyLimit"),
						JsonUtils.getAsEnum(ActivationTime.class, jsonElement, "activationRunTime"),
						JsonUtils.getAsEnum(SupportDuration.class, jsonElement, "supportDuration"));

				// Check for overlap with existing schedules before adding
				if (!overlapsExistingSchedule(schedule, newSchedule)) {
					schedule.add(newSchedule);
				}
			}

			schedule.sort(Comparator.comparing(ActivateFastFreqReserveSchedule::startTimestamp));
			return schedule;
		}

		/**
		 * Checks whether a new schedule overlaps with existing schedules or is an exact
		 * duplicate.
		 *
		 * @param schedule    List of existing schedules to compare against
		 * @param newSchedule The new schedule to check for overlap or duplication
		 * @return {@code true} if the new schedule overlaps with existing schedules or
		 *         is an exact duplicate, {@code false} otherwise
		 */
		private static boolean overlapsExistingSchedule(List<ActivateFastFreqReserveSchedule> schedule,
				ActivateFastFreqReserveSchedule newSchedule) {
			for (ActivateFastFreqReserveSchedule existingSchedule : schedule) {
				if (newSchedule.equals(existingSchedule)) {
					// duplicate found
					return true;
				}
				// Check for overlap
				if (newSchedule.startTimestamp < (existingSchedule.startTimestamp + existingSchedule.duration)
						&& (newSchedule.startTimestamp + newSchedule.duration) > existingSchedule.startTimestamp) {
					return true;
				}
			}
			// No overlap or exact duplicate found
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			var that = (ActivateFastFreqReserveSchedule) o;
			return this.startTimestamp == that.startTimestamp //
					&& this.duration == that.duration //
					&& this.dischargePowerSetPoint == that.dischargePowerSetPoint //
					&& this.frequencyLimit == that.frequencyLimit //
					&& this.activationRunTime.equals(that.activationRunTime) //
					&& this.supportDuration.equals(that.supportDuration);
		}

		@Override
		public String toString() {
			return String.format(
					"{\"startTimestamp\":%d, \"duration\":%d, \"dischargePowerSetPoint\":%d, \"frequencyLimit\":%d, \"activationRunTime\":\"%s\", \"supportDuration\":\"%s\"}",
					this.startTimestamp, this.duration, this.dischargePowerSetPoint, this.frequencyLimit,
					this.activationRunTime, this.supportDuration);
		}

		/**
		 * Converts this ActivateFastFreqReserveSchedule object to a JsonObject.
		 *
		 * @return A JsonObject representing this schedule, where each field is mapped
		 *         to a corresponding property with its value.
		 */
		public JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("startTimestamp", this.startTimestamp()) //
					.addProperty("duration", this.duration()) //
					.addProperty("dischargeActivePowerSetPoint", this.dischargePowerSetPoint()) //
					.addProperty("frequencyLimit", this.frequencyLimit()) //
					.addProperty("activationRunTime", this.activationRunTime()) //
					.addProperty("supportDuration", this.supportDuration()) //
					.build();
		}
	}
}
