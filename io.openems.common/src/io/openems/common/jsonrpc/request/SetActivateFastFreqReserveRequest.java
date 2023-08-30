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
 * Represents a JSON-RPC Request for 'setActivateFastFreqReserve'.
 *
 * <pre>
 {
 	"jsonrpc": "2.0",
 	"id": "UUID",
 	"method": "setActivateFastFreqReserve",
 	"params": {
 		"id": "edgeId",
 		"schedule": [{
 			"startTimestamp": "1542464697",
 			"duration": "900",
 			"dischargeActivePowerSetPoint": "92000",
 			"frequencyLimit": "49500",
 			"activationRunTime": "LONG_ACTIVATION_RUN",
 			"supportDuration": "LONG_SUPPORT_DURATION"
 		}]
 	}
 }
 * </pre>
 */
//CHECKSTYLE:OFF
public class SetActivateFastFreqReserveRequest extends JsonrpcRequest {

	public static SetActivateFastFreqReserveRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var edgeId = JsonUtils.getAsString(p, "id");
		var s = JsonUtils.getAsJsonArray(p, "schedule");
		var schedule = ActivateFastFreqReserveSchedule.from(s);
		return new SetActivateFastFreqReserveRequest(r, edgeId, schedule);

	}

	public static final String METHOD = "setActivateFastFreqReserve";

	private final String edgeId;
	private final List<ActivateFastFreqReserveSchedule> schedule;

	public SetActivateFastFreqReserveRequest(String edgeId) {
		this(edgeId, new ArrayList<>());
	}

	public SetActivateFastFreqReserveRequest(String edgeId, List<ActivateFastFreqReserveSchedule> schedule) {
		super(SetGridConnScheduleRequest.METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	private SetActivateFastFreqReserveRequest(JsonrpcRequest request, String edgeId,
			List<ActivateFastFreqReserveSchedule> schedule) {
		super(request, SetGridConnScheduleRequest.METHOD);
		this.edgeId = edgeId;
		this.schedule = schedule;
	}

	@Override
	public JsonObject getParams() {
		var schedule = new JsonArray();
		for (ActivateFastFreqReserveSchedule se : this.schedule) {
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

	public List<ActivateFastFreqReserveSchedule> getSchedule() {
		return this.schedule;
	}

//	public static enum ActivationTime {
//		SHORT_ACTIVATION_RUN(700, "Short activation time run, in milliseconds"), //
//		MEDIUM_ACTIVATION_RUN(1000, "Medium activation time run, in milliseconds"), //
//		LONG_ACTIVATION_RUN(1300, "Long activation time run, in milliseconds");
//
//		private final long value;
//		private final String name;
//
//		private ActivationTime(long value, String name) {
//			this.value = value;
//			this.name = name;
//		}
//
//		public long getValue() {
//			return value;
//		}
//
//		public String getName() {
//			return name;
//		}
//	}

	public static final String START_TIME_STAMP = "startTimestamp";
	public static final String DURATION = "duration";
	public static final String DISCHARGE_POWER_SETPOINT = "dischargePowerSetPoint";
	public static final String FREQ_LIMIT = "frequencyLimit";
	public static final String ACTIVATION_RUNTIME = "activationRunTime";
	public static final String SUPPORT_DURATION = "supportDuration";	

	public static class ActivateFastFreqReserveSchedule {
		
		
		public static List<ActivateFastFreqReserveSchedule> from(JsonArray j) throws OpenemsNamedException {
			List<ActivateFastFreqReserveSchedule> schedule = new ArrayList<>();
			for (JsonElement se : j) {
				var startTimestamp = JsonUtils.getAsLong(se, START_TIME_STAMP);
				var duration = JsonUtils.getAsInt(se, DURATION);				
				var dischargePowerSetPoint = JsonUtils.getAsInt(se, DISCHARGE_POWER_SETPOINT);
				var frequencyLimit = JsonUtils.getAsInt(se, FREQ_LIMIT);
				var activationRunTime = JsonUtils.getAsString(se, ACTIVATION_RUNTIME);
				var supportDuration = JsonUtils.getAsString(se, SUPPORT_DURATION);
				schedule.add(new ActivateFastFreqReserveSchedule(startTimestamp, //
						duration, //
						dischargePowerSetPoint, //
						frequencyLimit, //
						activationRunTime, //
						supportDuration));
			}
			schedule.sort(Comparator.comparing(ActivateFastFreqReserveSchedule::getStartTimestamp));
			return schedule;
		}

		private final long startTimestamp;
		private final int duration;
		private final int dischargePowerSetPoint;
		private final int frequencyLimit;
		private final String activationRunTime;
		private final String supportDuration;

		public ActivateFastFreqReserveSchedule(long startTimestamp, //
				int duration, //
				int dischargePowerSetPoint, //
				int frequencyLimit, //
				String activationRunTime, //
				String supportDuration) {
			this.startTimestamp = startTimestamp;
			this.duration = duration;
			this.dischargePowerSetPoint = dischargePowerSetPoint;
			this.frequencyLimit = frequencyLimit;
			this.activationRunTime = activationRunTime;
			this.supportDuration = supportDuration;

		}

		public long getStartTimestamp() {
			return this.startTimestamp;
		}

		public int getDuration() {
			return this.duration;
		}

		public int getFrequencyLimit() {
			return this.frequencyLimit;
		}

		public int getDischargePowerSetPoint() {
			return this.dischargePowerSetPoint;
		}

		public String getActivationRunTime() {
			return this.activationRunTime;
		}

		public String getSupportDuration() {
			return this.supportDuration;
		}

		protected JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("startTimestamp", this.getStartTimestamp()) //
					.addProperty("duration", this.getDuration()) //
					.addProperty("dischargeActivePowerSetPoint", this.dischargePowerSetPoint) //
					.addProperty("frequencyLimit", this.frequencyLimit) //
					.addProperty("activationRunTime", this.activationRunTime) //
					.addProperty("supportDuration", this.supportDuration) //
					.build();
		}

	}

}
//CHECKSTYLE:ON
