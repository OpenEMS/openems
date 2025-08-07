package io.openems.edge.io.shelly.common;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {

	private Utils() {
	}

	/**
	 * Generates a standard Debug-Log string for Shellys with one relay and power
	 * meter.
	 * 
	 * @param relayChannels      the Relay-Channel
	 * @param activePowerChannel the ActivePower-Channel
	 * @return suitable for {@link OpenemsComponent#debugLog()}
	 */
	public static String generateDebugLog(Channel<Boolean>[] relayChannels, Channel<Integer> activePowerChannel) {
		var b = new StringBuilder();
		for (int i = 0; i < relayChannels.length; i++) {
			var relayChannel = relayChannels[i];
			relayChannel.value().asOptional().ifPresentOrElse(v -> b.append(v ? "x" : "-"), () -> b.append("?"));
			if (i < relayChannels.length - 1) {
				b.append("|");
			}
		}
		b.append("|").append(activePowerChannel.value().asString());
		return b.toString();
	}

	/**
	 * Generates a standard Debug-Log string for Shellys with multiple relays.
	 * 
	 * @param digitalOutputChannels the Relay-Channels
	 * @return suitable for {@link OpenemsComponent#debugLog()}
	 */
	public static String generateDebugLog(BooleanWriteChannel[] digitalOutputChannels) {
		// TODO share code with AbstractKmtronicRelay.debugLog()
		return stream(digitalOutputChannels) //
				.map(c -> c.value().asOptional().map(v -> v //
						? "x" //
						: "-") //
						.orElse("?")) //
				.collect(joining(" "));
	}

	/**
	 * Generates a standard Debug-Log string for Shellys without digital outputs.
	 * 
	 * @param activePowerChannel the ActivePower-Channel
	 * @return suitable for {@link OpenemsComponent#debugLog()}
	 */
	public static String generateDebugLog(Channel<Integer> activePowerChannel) {
		var b = new StringBuilder();
		b.append(activePowerChannel.value().asString());
		return b.toString();
	}

	/**
	 * Executes a write command to a specified relay channel by constructing and
	 * sending an HTTP request based on the channel's current and intended state.
	 * This method compares the current state with the desired state, and only
	 * proceeds with the HTTP request if they differ, ensuring no unnecessary
	 * commands are sent. The method returns a CompletableFuture that completes when
	 * the HTTP request is finished. It completes normally if the HTTP request
	 * succeeds, and exceptionally if the request fails due to errors.
	 *
	 * @param relayChannel the channel for the relay, specifying the current and
	 *                     desired states
	 * @param baseUrl      the base URL for constructing the final endpoint URL
	 * @param httpBridge   the HTTP bridge to send the request
	 * @param index        the index of the DigitalChannel to write to (used for the
	 *                     URL)
	 * @return CompletableFuture{@code <Void>} that completes when the HTTP
	 *         operation completes. Completes exceptionally if there is an error in
	 *         the HTTP request.
	 */
	public static CompletableFuture<Void> executeWrite(WriteChannel<Boolean> relayChannel, String baseUrl,
			BridgeHttp httpBridge, Integer index) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Boolean readValue = relayChannel.value().get();
		Optional<Boolean> writeValue = relayChannel.getNextWriteValueAndReset();

		if (writeValue.isEmpty()) {
			future.complete(null); // No action needed
			return future;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			future.complete(null); // No change in state
			return future;
		}

		final String url = baseUrl + "/rpc/Switch.Set?id=" + index + "&on=" + (writeValue.get() ? "true" : "false");
		httpBridge.get(url).whenComplete((response, exception) -> {
			if (exception != null) {
				future.completeExceptionally(exception);
			} else {
				future.complete(null);
			}
		});

		return future;
	}

	/**
	 * Subscribes to the /shelly endpoint once to check for authentication status
	 * and device model.
	 * Sets the AUTH_ENABLED_WARNING channel and logs a warning if authentication is
	 * enabled.
	 * Validates the device model against expected models for the component type.
	 * 
	 * @param baseUrl        the base URL of the Shelly device
	 * @param httpBridge     the HTTP bridge for making requests
	 * @param component      the ShellyCommon component
	 * @param logger         the logger for logging messages
	 * @param expectedModels the set of expected model IDs for this component type
	 */
	public static void subscribeAuthenticationCheck(String baseUrl, BridgeHttp httpBridge, ShellyCommon component,
			Logger logger, Set<String> expectedModels) {
		httpBridge.get(baseUrl + "/shelly").thenAccept(response -> {
			try {
				var json = JsonUtils.parseToJsonObject(response.data());
				processShellyInfo(json, component, logger, expectedModels);
			} catch (OpenemsNamedException e) {
				// Log error but don't fail - authentication check is not critical
				logger.debug("Failed to parse /shelly response: " + e.getMessage());
			}
		}).exceptionally(error -> {
			// Log error but don't fail - authentication check is not critical
			logger.debug("Failed to fetch /shelly info: " + error.getMessage());
			return null;
		});
	}

	/**
	 * Subscribes to the /shelly endpoint once to check for authentication status.
	 * Sets the AUTH_ENABLED_WARNING channel and logs a warning if authentication is
	 * enabled.
	 * 
	 * @param baseUrl    the base URL of the Shelly device
	 * @param httpBridge the HTTP bridge for making requests
	 * @param component  the ShellyCommon component
	 * @param logger     the logger for logging messages
	 */
	public static void subscribeAuthenticationCheck(String baseUrl, BridgeHttp httpBridge, ShellyCommon component,
			Logger logger) {
		subscribeAuthenticationCheck(baseUrl, httpBridge, component, logger, null);
	}

	/**
	 * Process the response from /shelly endpoint to check authentication status
	 * and device model.
	 * 
	 * @param json           the parsed JSON response
	 * @param component      the ShellyCommon component
	 * @param logger         the logger for logging messages
	 * @param expectedModels the set of expected model IDs for this component type (can be null)
	 */
	private static void processShellyInfo(JsonElement json, ShellyCommon component, Logger logger, Set<String> expectedModels) {
		try {
			var jsonObj = getAsJsonObject(json);

			// Check auth_en field
			var authEnabled = getAsBoolean(jsonObj, "auth_en");
			component._setAuthEnabledWarning(authEnabled);

			// Log device info
			if (authEnabled) {
				logger.warn(
						"Authentication is enabled on Shelly device [{}]. Please disable authentication in Shelly settings for OpenEMS to work properly.",
						component.id());
			}

			// Get device generation
			var gen = jsonObj.has("gen") ? jsonObj.get("gen").getAsString() : "unknown";
			component.channel(ShellyCommon.ChannelId.DEVICE_GENERATION).setNextValue(gen);

			// Get device model
			var model = getAsString(jsonObj, "model");
			component.channel(ShellyCommon.ChannelId.DEVICE_MODEL).setNextValue(model);

			// Validate device model if expected models are provided
			if (expectedModels != null && !expectedModels.isEmpty()) {
				boolean modelMatches = expectedModels.contains(model);
				component._setWrongDeviceModel(!modelMatches);
				
				if (!modelMatches) {
					logger.error(
							"Wrong Shelly device model detected for component [{}]. Expected models: {}, but got: {}. Please configure the correct Shelly component type.",
							component.id(), expectedModels, model);
				}
			}

		} catch (OpenemsNamedException e) {
			logger.debug("Error parsing /shelly response: " + e.getMessage());
		}
	}

	/**
	 * Process the response from /shelly endpoint to check authentication status.
	 * 
	 * @param json      the parsed JSON response
	 * @param component the ShellyCommon component
	 * @param logger    the logger for logging messages
	 */
	private static void processShellyInfo(JsonElement json, ShellyCommon component, Logger logger) {
		processShellyInfo(json, component, logger, null);
	}

}