package io.openems.edge.simulator.app;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
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
 *         "start": "yyyy-mm-ddTHH:MM:00.00Z", // ISO_INSTANT
 *         "end": "yyyy-mm-ddTHH:MM:00.00Z", // ISO_INSTANT
 *         "timeleap": number [s],
 *         "executeCycleTwice": boolean // Execute every Cycle twice to simulate immediate execution
 *       },
 *       "profiles": {
 *         "meter0/ActivePower": number[],
 *       },
 *       "collect": [
 *         "_sum/GridActivePower",... // Channels to be collected for response
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
public class ExecuteSimulationRequest extends JsonrpcRequest {

	public static final String METHOD = "executeSimulation";

	public static class Clock {

		/**
		 * Create {@link Clock} from {@link JsonObject}.
		 *
		 * @param j the {@link JsonObject}
		 * @return the {@link Clock}
		 * @throws OpenemsNamedException on parse error
		 */
		public static Clock from(JsonObject j) throws OpenemsNamedException {
			var start = DateUtils.parseZonedDateTimeOrError(JsonUtils.getAsString(j, "start"));
			var end = DateUtils.parseZonedDateTimeOrError(JsonUtils.getAsString(j, "end"));
			var timeleapPerCycle = JsonUtils.getAsInt(j, "timeleapPerCycle");
			boolean executeCycleTwice = JsonUtils.getAsOptionalBoolean(j, "executeCycleTwice").orElse(false);
			return new Clock(start, end, timeleapPerCycle, executeCycleTwice);
		}

		public final ZonedDateTime start;
		public final ZonedDateTime end;
		public final int timeleapPerCycle;
		public final boolean executeCycleTwice;

		private Clock(ZonedDateTime start, ZonedDateTime end, int timeleapPerCycle, boolean executeCycleTwice) {
			this.start = start;
			this.end = end;
			this.timeleapPerCycle = timeleapPerCycle;
			this.executeCycleTwice = executeCycleTwice;
		}
	}

	public static class Profile {

		/**
		 * Create {@link Profile} from {@link JsonArray}.
		 *
		 * @param j the {@link JsonArray}
		 * @return the {@link Profile}
		 * @throws OpenemsNamedException on parse error
		 */
		public static Profile from(JsonArray j) throws OpenemsNamedException {
			final List<Integer> values = new ArrayList<>();
			j.forEach(value -> {
				values.add(JsonUtils.getAsOptionalInt(value).orElse(null));
			});
			if (values.isEmpty()) {
				values.add(null);
			}
			return new Profile(values);
		}

		public final List<Integer> values;
		private int currentIndex = 0;

		private Profile(List<Integer> values) {
			this.values = values;
		}

		/**
		 * Gets the currently active value of the {@link Profile}.
		 *
		 * @return the value
		 */
		public synchronized Integer getCurrentValue() {
			return this.values.get(this.currentIndex);
		}

		/**
		 * Selects the next active value in the {@link Profile}.
		 */
		public synchronized void selectNextValue() {
			this.currentIndex += 1;
			if (this.currentIndex > this.values.size() - 1) {
				this.currentIndex = 0;
			}
		}

	}

	/**
	 * Create {@link ExecuteSimulationRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link ExecuteSimulationRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static ExecuteSimulationRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		List<CreateComponentConfigRequest> components = new ArrayList<>();
		var jComponents = JsonUtils.getAsJsonArray(p, "components");
		for (JsonElement jComponent : jComponents) {
			components.add(CreateComponentConfigRequest.from(JsonUtils.getAsJsonObject(jComponent)));
		}
		var clock = Clock.from(JsonUtils.getAsJsonObject(p, "clock"));
		Map<String, Profile> profiles = new HashMap<>();
		var jProfiles = JsonUtils.getAsJsonObject(p, "profiles");
		for (Entry<String, JsonElement> jProfile : jProfiles.entrySet()) {
			profiles.put(jProfile.getKey(), Profile.from(JsonUtils.getAsJsonArray(jProfile.getValue())));
		}
		List<ChannelAddress> collects = new ArrayList<>();
		var jCollects = JsonUtils.getAsJsonArray(p, "collect");
		for (JsonElement jCollect : jCollects) {
			collects.add(ChannelAddress.fromString(JsonUtils.getAsString(jCollect)));
		}
		return new ExecuteSimulationRequest(r, components, clock, profiles, collects);
	}

	public final List<CreateComponentConfigRequest> components;
	public final Clock clock;
	public final Map<String, Profile> profiles;
	public final List<ChannelAddress> collects;

	public ExecuteSimulationRequest(List<CreateComponentConfigRequest> components, Clock clock,
			Map<String, Profile> profiles, List<ChannelAddress> collects) {
		super(UUID.randomUUID(), METHOD, JsonrpcRequest.NO_TIMEOUT);
		this.components = components;
		this.clock = clock;
		this.profiles = profiles;
		this.collects = collects;
	}

	public ExecuteSimulationRequest(JsonrpcRequest request, List<CreateComponentConfigRequest> components, Clock clock,
			Map<String, Profile> profiles, List<ChannelAddress> collects) {
		super(request, METHOD);
		this.components = components;
		this.clock = clock;
		this.profiles = profiles;
		this.collects = collects;
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
