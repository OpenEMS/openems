package io.openems.edge.io.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class ShellyUtils {

	private ShellyUtils() {
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
		var b = new StringBuilder();
		var i = 1;
		for (var channel : digitalOutputChannels) {
			var valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				b.append(valueOpt.get() ? "x" : "-");
			} else {
				b.append("?");
			}
			if (i < digitalOutputChannels.length) {
				b.append("|");
			}
			i++;
		}
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

}