package io.openems.edge.controller.levl.balancing;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsLong;
import static io.openems.common.utils.JsonUtils.getAsString;

import java.time.Instant;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class LevlControlRequest extends JsonrpcRequest {

	public static final String METHOD = "levlControl";

	private static final int QUARTER_HOUR_SECONDS = 900;

	public record Payload(int sellToGridLimitW, int buyFromGridLimitW, String levlRequestId, String timestamp,
			long energyWs, Instant start, Instant deadline, int levlSocWh, double socLowerBoundPercent,
			double socUpperBoundPercent, double efficiencyPercent, boolean influenceSellToGrid) {
	}

	/**
	 * Creates a {@link LevlControlRequest} from a {@link JsonrpcRequest}.
	 *
	 * @param r   the {@link JsonrpcRequest}
	 * @param now {@link Instant} of now
	 * @return the {@link LevlControlRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final LevlControlRequest from(JsonrpcRequest r, Instant now) throws OpenemsNamedException {
		final var p = r.getParams();
		var sellToGridLimitW = getAsInt(p, "sellToGridLimitW");
		var buyFromGridLimitW = getAsInt(p, "buyFromGridLimitW");
		var levlRequestId = getAsString(p, "levlRequestId");
		var timestamp = getAsString(p, "levlRequestTimestamp");
		var energyWs = getAsLong(p, "levlPowerW") * QUARTER_HOUR_SECONDS;
		var start = now.plusSeconds(getAsInt(p, "levlChargeDelaySec"));
		var deadline = start.plusSeconds(getAsInt(p, "levlChargeDurationSec"));
		var levlSocWh = getAsInt(p, "levlSocWh");
		var socLowerBoundPercent = getAsDouble(p, "levlSocLowerBoundPercent");
		var socUpperBoundPercent = getAsDouble(p, "levlSocUpperBoundPercent");
		var efficiencyPercent = getAsDouble(p, "efficiencyPercent");
		var influenceSellToGrid = getAsBoolean(p, "influenceSellToGrid");

		if (efficiencyPercent < 0) {
			throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("efficiencyPercent must be > 0");
		}
		if (efficiencyPercent > 100) {
			throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("efficiencyPercent must be <= 100");
		}

		return new LevlControlRequest(r,
				new Payload(sellToGridLimitW, buyFromGridLimitW, levlRequestId, timestamp, energyWs, start, deadline,
						levlSocWh, socLowerBoundPercent, socUpperBoundPercent, efficiencyPercent, influenceSellToGrid));
	}

	public final Payload payload;

	private LevlControlRequest(JsonrpcRequest request, Payload payload) {
		super(request, LevlControlRequest.METHOD);
		this.payload = payload;
	}

	public LevlControlRequest(Payload payload) {
		super(LevlControlRequest.METHOD);
		this.payload = payload;
	}

	// Just for testing
	protected LevlControlRequest(int startDelay, int duration, Instant now) {
		this(new Payload(//
				0, 0, null, null, 0, //
				now.plusSeconds(startDelay), now.plusSeconds(startDelay).plusSeconds(duration), //
				0, 0, 0, 0, false));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LevlControlRequest other = (LevlControlRequest) obj;
		return this.payload.equals(other.payload);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this.getClass()) //
				.addValue(this.payload) //
				.toString();
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("sellToGridLimitW", this.payload.sellToGridLimitW) //
				.addProperty("buyFromGridLimitW", this.payload.buyFromGridLimitW) //
				.addProperty("levlRequestId", this.payload.levlRequestId) //
				.addProperty("levlRequestTimestamp", this.payload.timestamp) //
				.addProperty("levlPowerW", this.payload.energyWs) //
				.addProperty("levlChargeDelaySec", this.payload.start) //
				.addProperty("levlChargeDurationSec", this.payload.deadline) //
				.addProperty("levlSocWh", this.payload.levlSocWh) //
				.addProperty("levlSocLowerBoundPercent", this.payload.socLowerBoundPercent) //
				.addProperty("levlSocUpperBoundPercent", this.payload.socUpperBoundPercent) //
				.addProperty("efficiencyPercent", this.payload.efficiencyPercent) //
				.addProperty("influenceSellToGrid", this.payload.influenceSellToGrid) //
				.build();
	}
}
