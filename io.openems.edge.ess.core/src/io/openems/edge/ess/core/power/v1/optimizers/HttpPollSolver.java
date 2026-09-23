package io.openems.edge.ess.core.power.v1.optimizers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.math3.optim.PointValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.v1.data.TargetDirection;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * HTTP-based external solver that delegates power distribution optimization to
 * an external service via HTTP API calls.
 *
 * <p>
 * This solver extracts ESS SOC data, sends it to an external optimization
 * service, and converts the response back to the format expected by the OpenEMS
 * power solver.
 * </p>
 *
 * <p>
 * If HTTP request fails or response is unavailable, returns zero power
 * distribution.
 * </p>
 */
public class HttpPollSolver {

	private static final Logger log = LoggerFactory.getLogger(HttpPollSolver.class);
	private static final Gson gson = new GsonBuilder().create();

	private static final String SOLVER_ENDPOINT_URL = "http://10.4.0.101:8090/pdist-calculator-ind-m";
	private static final int HTTP_TIMEOUT_SECONDS = 10; // Timeout for HTTP requests

	/**
	 * Applies HTTP-based power distribution optimization.
	 *
	 * @param coefficients   the {@link Coefficients} for mapping results
	 * @param esss           the {@link ManagedSymmetricEss} list with SOC data
	 * @param allInverters   all {@link Inverter}s (for compatibility, may not be
	 *                       used)
	 * @param allConstraints all active {@link Constraint}s to extract power targets
	 * @param direction      the {@link TargetDirection}
	 *                       (CHARGE/DISCHARGE/KEEP_ZERO)
	 * @param httpBridge     the {@link BridgeHttp} instance for external
	 *                       communication
	 * @return a solution {@link PointValuePair} or zero array if HTTP fails
	 */
	public static PointValuePair apply(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			List<Inverter> allInverters, List<Constraint> allConstraints, TargetDirection direction,
			BridgeHttp httpBridge) {

		if (esss.isEmpty()) {
			log.warn("No ESS units available for HTTP optimization, returning zero solution");
			return createZeroSolution(coefficients);
		}

		if (httpBridge == null) {
			log.error("HTTP bridge not available for external optimization, returning zero solution");
			return createZeroSolution(coefficients);
		}

		try {
			// Extract total power setpoint from constraints (call once and pass to both
			// methods)
			double setActivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.ACTIVE);

			if (Double.isNaN(setActivePower) || setActivePower == 0.0) {
				log.warn("No active power setpoint found or setpoint is zero ({}), returning zero solution",
						setActivePower);
				return createZeroSolution(coefficients);
			}

			log.debug("Extracted setActivePower: {} for HTTP optimization", setActivePower);

			// 1. Extract SOC data and build request JSON with setActivePower
			JsonObject requestJson = buildRequestJson(esss, setActivePower);

			// 2. Send HTTP request to external solver
			JsonObject responseJson = sendHttpRequest(requestJson, httpBridge);

			if (responseJson == null) {
				log.warn("Failed to get response from external HTTP solver, returning zero solution");
				return createZeroSolution(coefficients);
			}

			// 3. Convert response back to PointValuePair format using the same
			// setActivePower
			PointValuePair result = convertResponseToPointValuePair(responseJson, coefficients, esss, setActivePower,
					direction);

			if (result == null) {
				log.warn("Failed to convert HTTP response to solution, returning zero solution");
				return createZeroSolution(coefficients);
			}

			log.info("Successfully obtained HTTP optimization solution");
			return result;

		} catch (Exception e) {
			log.error("HTTP solver failed: {}, returning zero solution", e.getMessage(), e);
			return createZeroSolution(coefficients);
		}
	}

	/**
	 * Builds the JSON request payload with ESS SOC data and SetActivePower.
	 *
	 * @param esss           list of ESS units
	 * @param setActivePower the active power setpoint to include in request
	 * @return JSON request object
	 */
	private static JsonObject buildRequestJson(List<ManagedSymmetricEss> esss, double setActivePower) {

		JsonObject request = new JsonObject();
		JsonObject powerDistribution = new JsonObject();

		// Add SetActivePower to the request
		request.addProperty("SetActivePower", setActivePower);

		// Extract SOC data from non-MetaEss units
		var actualEssList = esss.stream().filter(ess -> !(ess instanceof MetaEss)).toList();

		for (ManagedSymmetricEss ess : actualEssList) {
			JsonObject essData = new JsonObject();

			// Add SOC data
			int soc = ess.getSoc().get();
			essData.addProperty("soc", soc);

			powerDistribution.add(ess.id(), essData);
		}

		request.add("powerDistribution", powerDistribution);

		log.debug("Built request JSON with SetActivePower: {}", setActivePower);
		return request;
	}

	/**
	 * Creates a zero solution when HTTP optimization fails.
	 *
	 * @param coefficients the coefficients to determine array size
	 * @return PointValuePair with all zeros
	 */
	private static PointValuePair createZeroSolution(Coefficients coefficients) {
		double[] zeroArray = new double[coefficients.getAll().size()];
		return new PointValuePair(zeroArray, 0.0);
	}

	/**
	 * Sends HTTP request to external solver service.
	 *
	 * @param requestJson the request payload
	 * @param httpBridge  the HTTP bridge for communication
	 * @return response JSON or null if failed
	 */
	private static JsonObject sendHttpRequest(JsonObject requestJson, BridgeHttp httpBridge) {
		try {
			String requestBody = gson.toJson(requestJson);

			// Create endpoint for external solver
			var endpoint = new BridgeHttp.Endpoint(//
					SOLVER_ENDPOINT_URL, //
					HttpMethod.POST, //
					BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
					BridgeHttp.DEFAULT_READ_TIMEOUT, //
					requestBody, //
					Map.of("Content-Type", "application/json"));

			log.debug("Sending request to external solver: {}", requestBody);

			// Make HTTP call and wait for response with timeout
			var responseFuture = httpBridge.request(endpoint);
			var response = responseFuture.get(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);

			if (response != null && response.data() != null && !response.data().isEmpty()) {
				log.debug("Received response from external solver: {}", response.data());
				return gson.fromJson(response.data(), JsonObject.class);
			} else {
				log.warn("Empty or null response from external solver");
				return null;
			}

		} catch (TimeoutException e) {
			log.error("HTTP request timed out after {} seconds", HTTP_TIMEOUT_SECONDS);
			return null;
		} catch (Exception e) {
			log.error("HTTP request failed: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Converts HTTP response back to PointValuePair format expected by solver.
	 *
	 * @param responseJson   the HTTP response
	 * @param coefficients   coefficient mapping for result array
	 * @param esss           ESS list for validation
	 * @param setActivePower The Active power to be set
	 * @param direction      target direction for scaling
	 * @return PointValuePair with power distribution results
	 */
	private static PointValuePair convertResponseToPointValuePair(JsonObject responseJson, Coefficients coefficients,
			List<ManagedSymmetricEss> esss, double setActivePower, TargetDirection direction) {

		try {
			JsonObject powerDistribution = responseJson.getAsJsonObject("powerDistribution");

			if (powerDistribution == null) {
				return null;
			}

			final var finalSetActivePower = setActivePower;

			Map<String, Double> essIdToDistributionMap = new HashMap<>();

			for (String essId : powerDistribution.keySet()) {
				var essResult = powerDistribution.getAsJsonObject(essId);
				if (essResult.has("pdist")) {
					var distributionPercentage = essResult.get("pdist").getAsDouble();
					essIdToDistributionMap.put(essId, distributionPercentage);
				}
			}

			// Convert to coefficient-ordered array
			double[] resultArray = coefficients.getAll().stream().mapToDouble(coefficient -> {
				var essId = coefficient.getEssId();
				var powerType = coefficient.getPwr();

				// Skip MetaEss coefficients
				boolean isMetaEss = esss.stream().filter(ess -> ess.id().equals(essId))
						.anyMatch(ess -> ess instanceof MetaEss);

				if (isMetaEss) {
					return 0.0;
				}

				// Only process ACTIVE power coefficients, set REACTIVE to 0
				if (powerType != Pwr.ACTIVE) {
					return 0.0;
				}

				// Get distribution percentage from HTTP response
				var distributionPercentage = essIdToDistributionMap.get(essId);
				if (distributionPercentage != null) {
					double actualPower = (finalSetActivePower * (distributionPercentage / 100.0));

					return reverseAbsoluteData(actualPower, direction);
				} else {
					return 0.0;
				}
			}).toArray();
			return new PointValuePair(resultArray, 0.0);

		} catch (Exception e) {
			log.error("Failed to convert HTTP response: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Extracts total active power target from MetaEss constraints. Same logic as
	 * KeepAllNearEqual.getPowerSetPoint()
	 *
	 * @param esss           ESS list
	 * @param allConstraints constraint list
	 * @param direction      target direction
	 * @param pwr            power type (ACTIVE/REACTIVE)
	 * @return total power target or NaN if not found
	 */
	private static double getPowerSetPoint(List<ManagedSymmetricEss> esss, List<Constraint> allConstraints,
			TargetDirection direction, Pwr pwr) {

		// Find MetaEss (cluster) ID
		var clusterEssId = esss.stream().filter(MetaEss.class::isInstance).findFirst().map(ManagedSymmetricEss::id)
				.orElse(null);

		if (clusterEssId == null) {
			return Double.NaN;
		}

		var noPowerSetPoint = Double.NaN;

		// Extract EQUALS constraint for the specified power type
		return allConstraints.stream()//
				.filter(constraint -> constraint.relationship == Relationship.EQUALS)
				.filter(constraint -> constraint.coefficients.length == 1)
				.filter(constraint -> clusterEssId.equals(constraint.coefficients[0].getCoefficient().getEssId()))
				.filter(constraint -> constraint.coefficients[0].getCoefficient().getPwr() == pwr)
				.mapToDouble(constraint -> constraint.value)//
				.map(c -> absoluteData(c, direction))//
				.findFirst().orElse(noPowerSetPoint);
	}

	/**
	 * Calculate absolute value or zero based on the TargetDirection. Same as
	 * KeepAllNearEqual.absoluteData()
	 *
	 * @param d         the input value to be processed
	 * @param direction the {@link TargetDirection}
	 * @return the processed value based on the direction
	 */
	private static double absoluteData(double d, TargetDirection direction) {
		return switch (direction) {
		case CHARGE -> Math.abs(d);
		case DISCHARGE -> d;
		case KEEP_ZERO -> 0.0;
		};
	}

	/**
	 * Calculate reverse absolute value or zero based on the TargetDirection. Same
	 * as KeepAllNearEqual.reverseAbsoluteData()
	 *
	 * @param d         the input value to be processed
	 * @param direction the {@link TargetDirection}
	 * @return the processed value based on the direction
	 */
	private static double reverseAbsoluteData(double d, TargetDirection direction) {
		return switch (direction) {
		case CHARGE -> -d;
		case DISCHARGE -> d;
		case KEEP_ZERO -> 0.0;
		};
	}
}
