package io.openems.edge.io.shelly.common;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {

	private Utils() {
	}

	/**
	 * Generates a standard Debug-Log string for Shellys with one relay and power
	 * meter.
	 * 
	 * @param relayChannel       the Relay-Channel
	 * @param activePowerChannel the ActivePower-Channel
	 * @return suitable for {@link OpenemsComponent#debugLog()}
	 */
	public static String generateDebugLog(Channel<Boolean> relayChannel, Channel<Integer> activePowerChannel) {
		var b = new StringBuilder();
		relayChannel.value().asOptional().ifPresentOrElse(//
				v -> b.append(v ? "On" : "Off"), //
				() -> b.append("Unknown"));
		b.append("|");
		b.append(activePowerChannel.value().asString());
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
				b.append("Unknown");
			}
			if (i < digitalOutputChannels.length) {
				b.append("|");
			}
			i++;
		}
		return b.toString();
	}

}
