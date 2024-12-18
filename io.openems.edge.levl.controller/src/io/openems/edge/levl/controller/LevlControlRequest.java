package io.openems.edge.levl.controller;

import com.google.gson.JsonObject;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import java.time.Instant;
import java.util.Objects;

public class LevlControlRequest {	
	public static final String METHOD = "sendLevlControlRequest";
	public static final int QUARTER_HOUR_SECONDS = 900;
	protected int sellToGridLimitW;
	protected int buyFromGridLimitW;
	protected String levlRequestId;
	protected String timestamp;
	protected long energyWs;
	protected Instant start;
	protected Instant deadline;
	protected int levlSocWh;
	protected double socLowerBoundPercent;
	protected double socUpperBoundPercent;
	protected double efficiencyPercent;
	protected boolean influenceSellToGrid;
	
	
	protected LevlControlRequest(JsonObject params, Instant now) throws OpenemsError.OpenemsNamedException {
		try {
			this.parseFields(params, now);
		} catch (NullPointerException e) {
			throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("missing fields in request: " + e.getMessage());
		} catch (NumberFormatException e) {
			throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("wrong field type in request: " + e.getMessage());
		}
		if (this.efficiencyPercent < 0) {
			throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("efficiencyPercent must be > 0");
		}
		if (this.efficiencyPercent > 100) {
			throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("efficiencyPercent must be <= 100");
		}
	}
	
	//Just for testing
	protected LevlControlRequest() {
		
	}

	//Just for testing
	protected LevlControlRequest(int sellToGridLimitW, int buyFromGridLimitW, String levlRequestId, String timestamp,
			long energyWs, Instant start, Instant deadline, int levlSocWh, int socLowerBoundPercent,
			int socUpperBoundPercent, double efficiencyPercent, boolean influenceSellToGrid) {
		this.sellToGridLimitW = sellToGridLimitW;
		this.buyFromGridLimitW = buyFromGridLimitW;
		this.levlRequestId = levlRequestId;
		this.timestamp = timestamp;
		this.energyWs = energyWs;
		this.start = start;
		this.deadline = deadline;
		this.levlSocWh = levlSocWh;
		this.socLowerBoundPercent = socLowerBoundPercent;
		this.socUpperBoundPercent = socUpperBoundPercent;
		this.efficiencyPercent = efficiencyPercent;
		this.influenceSellToGrid = influenceSellToGrid;
	}

	//Just for testing
	protected LevlControlRequest(int startDelay, int duration, Instant now) {
		this.start = now.plusSeconds(startDelay);
		this.deadline = this.start.plusSeconds(duration);
	}

	/**
	 * Generates a levl control request object based on the JSON-RPC request.
	 * 
	 * @param request the JSON-RPC request
	 * @param now the current time
	 * @return the levl control request
	 * @throws OpenemsNamedException on error
	 */
	protected static LevlControlRequest from(JsonrpcRequest request, Instant now) throws OpenemsNamedException {
		var params = request.getParams();
		return new LevlControlRequest(params, now);
	}

	private void parseFields(JsonObject params, Instant now) throws OpenemsNamedException {
		this.levlRequestId = JsonUtils.getAsString(params, "levlRequestId");
		this.timestamp = JsonUtils.getAsString(params, "levlRequestTimestamp");
		this.energyWs = JsonUtils.getAsLong(params, "levlPowerW") * QUARTER_HOUR_SECONDS;
		this.start = now.plusSeconds(JsonUtils.getAsInt(params, "levlChargeDelaySec"));
		this.deadline = this.start.plusSeconds(JsonUtils.getAsInt(params, "levlChargeDurationSec"));
		this.levlSocWh = JsonUtils.getAsInt(params, "levlSocWh");
		this.socLowerBoundPercent = JsonUtils.getAsDouble(params, "levlSocLowerBoundPercent");
		this.socUpperBoundPercent = JsonUtils.getAsDouble(params, "levlSocUpperBoundPercent");
		this.sellToGridLimitW = JsonUtils.getAsInt(params, "sellToGridLimitW");
		this.buyFromGridLimitW = JsonUtils.getAsInt(params, "buyFromGridLimitW");
		this.efficiencyPercent = JsonUtils.getAsDouble(params, "efficiencyPercent");
		this.influenceSellToGrid = JsonUtils.getAsBoolean(params, "influenceSellToGrid");
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.buyFromGridLimitW, this.deadline, this.efficiencyPercent, this.energyWs,
				this.influenceSellToGrid, this.levlRequestId, this.levlSocWh, this.sellToGridLimitW,
				this.socLowerBoundPercent, this.socUpperBoundPercent, this.start, this.timestamp);
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
		return this.buyFromGridLimitW == other.buyFromGridLimitW && Objects.equals(this.deadline, other.deadline)
				&& Double.doubleToLongBits(this.efficiencyPercent) == Double.doubleToLongBits(other.efficiencyPercent)
				&& this.energyWs == other.energyWs && this.influenceSellToGrid == other.influenceSellToGrid
				&& Objects.equals(this.levlRequestId, other.levlRequestId) && this.levlSocWh == other.levlSocWh
				&& this.sellToGridLimitW == other.sellToGridLimitW
				&& this.socLowerBoundPercent == other.socLowerBoundPercent
				&& this.socUpperBoundPercent == other.socUpperBoundPercent && Objects.equals(this.start, other.start)
				&& Objects.equals(this.timestamp, other.timestamp);
	}

	@Override
	public String toString() {
		return "LevlControlRequest [sellToGridLimitW=" + this.sellToGridLimitW + ", buyFromGridLimitW="
				+ this.buyFromGridLimitW + ", levlRequestId=" + this.levlRequestId + ", timestamp=" + this.timestamp
				+ ", energyWs=" + this.energyWs + ", start=" + this.start + ", deadline=" + this.deadline
				+ ", levlSocWh=" + this.levlSocWh + ", socLowerBoundPercent=" + this.socLowerBoundPercent
				+ ", socUpperBoundPercent=" + this.socUpperBoundPercent + ", efficiencyPercent="
				+ this.efficiencyPercent + ", influenceSellToGrid=" + this.influenceSellToGrid + "]";
	}
}
