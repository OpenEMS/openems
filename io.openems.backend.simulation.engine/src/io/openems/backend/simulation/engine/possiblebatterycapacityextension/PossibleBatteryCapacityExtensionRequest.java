package io.openems.backend.simulation.engine.possiblebatterycapacityextension;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Request from an authenticated User.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "possibleBatteryCapacityExtension",
 *   "params": {
 *   }
 * }
 * </pre>
 */
public class PossibleBatteryCapacityExtensionRequest extends JsonrpcRequest {

	public static final String METHOD = "possibleBatteryCapacityExtension";

	/**
	 * Parses a {@link JsonrpcRequest} to a
	 * {@link PossibleBatteryCapacityExtensionRequest}.
	 * 
	 * @param r the {@link JsonrpcRequest} to parse
	 * @return the parsed result
	 * @throws OpenemsNamedException on error
	 */
	public static PossibleBatteryCapacityExtensionRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new PossibleBatteryCapacityExtensionRequest(payload);
	}

	/**
	 * All Power Channels, i.e. Channels that are exported per channel and
	 * timestamp.
	 */
	public static final Set<ChannelAddress> POWERCHANNELS = Stream.of(//
			new ChannelAddress("_sum", "GridActivePower")).collect(Collectors.toCollection(HashSet::new));

	public PossibleBatteryCapacityExtensionRequest() {
		super(PossibleBatteryCapacityExtensionRequest.METHOD);
	}

	public PossibleBatteryCapacityExtensionRequest(JsonrpcRequest request) {
		super(request, PossibleBatteryCapacityExtensionRequest.METHOD);
	}

	public ZonedDateTime getFromDate() {
		return ZonedDateTime.now().minusYears(1).withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
	}

	public ZonedDateTime getToDate() {
		return ZonedDateTime.now().withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

	/**
	 * Gets the {@link ChannelAddress}es.
	 *
	 * @return Set of {@link ChannelAddress}
	 */
	public Set<ChannelAddress> getPowerChannels() {
		return POWERCHANNELS;
	}
}
