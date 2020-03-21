package io.openems.edge.simulator.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Executes a simulation.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSimulation",
 *   "params": {
 *     components: {
 *       "componentId": {
 *     	   "factoryPid": string,
 *         "properties": [{
 *           "name": string,
 *           "value": any 
 *         }]
 *       },
 *       "clock": {
 *         "start": "yyyy-mm-dd HH:MM",
 *         "end": "yyyy-mm-dd HH:MM",
 *         "timeleap": number [s]
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class ExecuteSimulationRequest extends JsonrpcRequest {

	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

	public static class Clock {
		public static Clock from(JsonObject j) throws OpenemsNamedException {
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
			LocalDateTime start = LocalDateTime.parse(JsonUtils.getAsString(j, "start"), dateTimeFormatter);
			LocalDateTime end = LocalDateTime.parse(JsonUtils.getAsString(j, "end"), dateTimeFormatter);
			int timeleap = JsonUtils.getAsInt(j, "timeleap");
			return new Clock(start, end, timeleap);
		}

		public final LocalDateTime start;
		public final LocalDateTime end;
		public final int timeleap;

		private Clock(LocalDateTime start, LocalDateTime end, int timeleap) {
			this.start = start;
			this.end = end;
			this.timeleap = timeleap;
		}
	}

	public static final String METHOD = "executeSimulation";

	public static ExecuteSimulationRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		List<CreateComponentConfigRequest> components = new ArrayList<>();
		JsonArray jComponents = JsonUtils.getAsJsonArray(p, "components");
		for (JsonElement jComponent : jComponents) {
			components.add(CreateComponentConfigRequest.from(JsonUtils.getAsJsonObject(jComponent)));
		}
		Clock clock = Clock.from(JsonUtils.getAsJsonObject(p, "clock"));
		return new ExecuteSimulationRequest(r.getId(), components, clock);
	}

	public final List<CreateComponentConfigRequest> components;
	public final Clock clock;

	public ExecuteSimulationRequest(List<CreateComponentConfigRequest> components, Clock clock) {
		this(UUID.randomUUID(), components, clock);
	}

	public ExecuteSimulationRequest(UUID id, List<CreateComponentConfigRequest> components, Clock clock) {
		super(id, METHOD);
		this.components = components;
		this.clock = clock;
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
