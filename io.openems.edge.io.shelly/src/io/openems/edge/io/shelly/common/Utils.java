package io.openems.edge.io.shelly.common;

import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;

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
			relayChannel.value().asOptional().ifPresentOrElse(v -> b.append(v ? "On" : "Off"),
					() -> b.append("Unknown"));
			if (i < relayChannels.length - 1) {
				b.append("|");
			}
		}
		b.append("|").append(activePowerChannel.value().asString());
		return b.toString();
	}

	/**
	 * Executes a write command to a specified relay channel by constructing and
	 * sending an HTTP request based on the channel's current and intended state.
	 * This method compares the current state with the desired state, and only
	 * proceeds with the HTTP request if they differ, ensuring no unnecessary
	 * commands are sent.
	 *
	 * @param relayChannel the channel for the relay, specifying the current and
	 *                     desired states
	 * @param baseUrl      the base URL for constructing the final endpoint URL
	 * @param httpBridge   the HTTP bridge to send the request
	 * @param index        the index of the DigitalChannel to write to (used for the
	 *                     URL)
	 */
	public static void executeWrite(WriteChannel<Boolean> relayChannel, String baseUrl, BridgeHttp httpBridge,
			Integer index) {
		Boolean readValue = relayChannel.value().get();
		Optional<Boolean> writeValue = relayChannel.getNextWriteValueAndReset();
		if (writeValue.isEmpty()) {
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			return;
		}
		final String url = baseUrl + "/rpc/Switch.Set?id=" + index + "&on=" + (writeValue.get() ? "true" : "false");
		httpBridge.get(url).whenComplete((t, e) -> {
			// Handle completion, e.g., error logging or success confirmation.
		});
	}

}
