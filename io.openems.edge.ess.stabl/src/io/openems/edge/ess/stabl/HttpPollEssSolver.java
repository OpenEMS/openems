package io.openems.edge.ess.stabl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.edge.common.channel.value.Value;

/**
 * Sends per-cycle POST requests to the external solver and returns the last
 * cached ranks. Falls back to all-zero ranks until the first response arrives.
 */
public class HttpPollEssSolver {

	private static final Logger log = LoggerFactory.getLogger(HttpPollEssSolver.class);
	private static final Gson gson = new GsonBuilder().create();

	// Fallback static weights
	private static final int[] FALLBACK_STRING_1_WEIGHTS = new int[9];
	private static final int[] FALLBACK_STRING_2_WEIGHTS = new int[9];
	private static final int[] FALLBACK_STRING_3_WEIGHTS = new int[9];

	private volatile int[][] cachedWeights = null;

	/**
	 * Calculates optimal power distribution weights for STABL ESS modules.
	 * 
	 * @param essImpl      the EssStablImpl instance to extract module data from
	 * @param totalPower   the total power to distribute
	 * @param httpBridge   the HTTP bridge for external communication
	 * @param moduleConfig array indicating number of modules per string
	 * @return 2D array of weights [stringIndex][moduleIndex]
	 */
	public int[][] calculateOptimalWeights(EssStablImpl essImpl, int totalPower, BridgeHttp httpBridge,
			int[] moduleConfig, int numberOfStrings, String solverUrl) {

		if (httpBridge != null && moduleConfig != null && moduleConfig.length == 3) {
			this.fireAsyncRequest(essImpl, totalPower, httpBridge, moduleConfig, numberOfStrings, solverUrl);
		}

		if (this.cachedWeights != null) {
			return this.cachedWeights;
		}

		log.debug("No cached weights available yet, using fallback weights");
		return getFallbackWeights(moduleConfig);
	}

	/**
	 * Fires an async HTTP request to the solver. Updates the cache when the
	 * response arrives — does not block the calling thread.
	 */
	private void fireAsyncRequest(EssStablImpl essImpl, int totalPower, BridgeHttp httpBridge, int[] moduleConfig,
			int numberOfStrings, String solverUrl) {
		var requestJson = buildModuleStateRequest(essImpl, totalPower, moduleConfig, numberOfStrings);
		var requestBody = gson.toJson(requestJson);

		var endpoint = new BridgeHttp.Endpoint(//
				solverUrl, //
				HttpMethod.POST, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				requestBody, //
				Map.of("Content-Type", "application/json"));

		httpBridge.request(endpoint).thenAccept(response -> {
			if (response == null || response.data() == null || response.data().isEmpty()) {
				log.warn("Empty or null response from ESS solver");
				return;
			}
			var responseJson = gson.fromJson(response.data(), JsonObject.class);
			var weights = parseWeightsResponse(responseJson, moduleConfig);
			if (weights != null) {
				this.cachedWeights = weights;
				log.debug("Cached new weights from ESS solver");
			}
		}).exceptionally(e -> {
			log.warn("ESS solver request failed: {}", e.getMessage());
			return null;
		});
	}

	/**
	 * Builds the observation array request for the Flask solver.
	 *
	 * <p>
	 * Format: {@code {"observation": [power, [soc×n, temp×n], ...]}}
	 * <ul>
	 * <li>Index 0: normalized power (totalPower / MAX_APPARENT_POWER)</li>
	 * <li>Per-string inner array: SOC values (0–1) followed by per-module
	 * temperatures in °C</li>
	 * </ul>
	 */
	private static JsonObject buildModuleStateRequest(EssStablImpl essImpl, int totalPower, int[] moduleConfig,
			int numberOfStrings) {
		JsonObject request = new JsonObject();
		JsonArray observation = new JsonArray();

		int activeStrings = Math.min(numberOfStrings, 3);

		// 1. Normalized power
		double normalizedPower = (double) totalPower / EssStableConstants.MAX_APPARENT_POWER;
		observation.add(normalizedPower);

		// 2. Per-string inner arrays: [soc×n, temp×n]
		for (int stringIndex = 0; stringIndex < activeStrings; stringIndex++) {
			int modulesInString = moduleConfig[stringIndex];
			JsonArray stringArray = new JsonArray();

			// SOC values (normalized 0–1)
			for (int moduleIndex = 0; moduleIndex < modulesInString; moduleIndex++) {
				String socChannelId = "SOC_AVG_STRING_" + (stringIndex + 1) + "_MODULE_" + moduleIndex;
				Value<Integer> socValue = essImpl.getValueFromChannelById(socChannelId);
				double soc = (socValue != null && socValue.isDefined()) ? socValue.get() / 100.0 : 0.5;
				stringArray.add(soc);
			}

			// Per-module temperature values (°C)
			for (int moduleIndex = 0; moduleIndex < modulesInString; moduleIndex++) {
				String tempChannelId = "TEMPERATURE_AVG_STRING_" + (stringIndex + 1) + "_MODULE_" + moduleIndex;
				Value<Integer> tempValue = essImpl.getValueFromChannelById(tempChannelId);
				int temp = (tempValue != null && tempValue.isDefined()) ? tempValue.get() : 25;
				stringArray.add(temp);
			}

			observation.add(stringArray);
		}

		log.debug("Built ESS solver observation: {} strings", activeStrings);
		request.add("observation", observation);
		return request;
	}

	/**
	 * Parses the Server solver response into a 2D weight array.
	 *
	 * <p>
	 * Expected format:
	 *
	 * <pre>
	 * {
	 *   "results": [
	 *     { "battery_powers": [p0..p8], "order": [m0..m8], "action": [...] },
	 *     ...one entry per string...
	 *   ]
	 * }
	 * </pre>
	 *
	 * {@code order} contains 1-based module indices mapping each
	 * {@code battery_powers} value to its target module:
	 * {@code weights[order[i]-1] = battery_powers[i]}.
	 */
	private static int[][] parseWeightsResponse(JsonObject response, int[] moduleConfig) {
		try {
			if (!response.has("results")) {
				log.error("Response missing 'results' field");
				return null;
			}

			JsonArray results = response.getAsJsonArray("results");
			int[][] weights = new int[3][];
			for (int stringIndex = 0; stringIndex < 3; stringIndex++) {
				weights[stringIndex] = new int[moduleConfig[stringIndex]];
			}

			// Only apply solver order to string 3; strings 1 and 2 are fixed to 0
			int string3Index = 2;
			if (results.size() > string3Index) {
				JsonObject string3Result = results.get(string3Index).getAsJsonObject();
				JsonArray order = string3Result.getAsJsonArray("ranks");
				int modulesInString3 = moduleConfig[string3Index];

				log.info("String 3 order from Flask: {}", order);

				for (int i = 0; i < Math.min(order.size(), modulesInString3); i++) {
					weights[string3Index][i] = order.get(i).getAsInt();
				}
			}

			for (int stringIndex = 0; stringIndex < 3; stringIndex++) {
				log.info("String {} weights: {}", stringIndex + 1, java.util.Arrays.toString(weights[stringIndex]));
			}

			return weights;

		} catch (Exception e) {
			log.error("Failed to parse weights response: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Returns fallback static weights when no cached result is available.
	 */
	private static int[][] getFallbackWeights(int[] moduleConfig) {
		int[][] fallbackWeights = new int[3][];
		for (int stringIndex = 0; stringIndex < 3; stringIndex++) {
			int moduleCount = (moduleConfig == null || stringIndex >= moduleConfig.length) ? 9
					: moduleConfig[stringIndex];
			fallbackWeights[stringIndex] = getFallbackWeightsForString(stringIndex, moduleCount);
		}
		return fallbackWeights;
	}

	private static int[] getFallbackWeightsForString(int stringIndex, int moduleCount) {
		int[] sourceWeights = switch (stringIndex) {
		case 0 -> FALLBACK_STRING_1_WEIGHTS;
		case 1 -> FALLBACK_STRING_2_WEIGHTS;
		case 2 -> FALLBACK_STRING_3_WEIGHTS;
		default -> FALLBACK_STRING_1_WEIGHTS;
		};
		int[] weights = new int[Math.min(moduleCount, sourceWeights.length)];
		System.arraycopy(sourceWeights, 0, weights, 0, weights.length);
		return weights;
	}

}
