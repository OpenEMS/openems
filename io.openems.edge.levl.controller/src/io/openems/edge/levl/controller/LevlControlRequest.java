package io.openems.edge.levl.controller;

import com.google.gson.JsonObject;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class LevlControlRequest {
    public static final String METHOD = "sendLevlControlRequest";
    protected static Clock clock = Clock.systemDefaultZone();
    protected int sellToGridLimitW;
    protected int buyFromGridLimitW;
    protected String levlRequestId;
    protected String timestamp;
    protected long energyWs;
    protected LocalDateTime start;
    protected LocalDateTime deadline;
    protected int levlSocWh;
    protected int socLowerBoundPercent;
    protected int socUpperBoundPercent;
    protected double efficiencyPercent;
    protected boolean influenceSellToGrid;

    public LevlControlRequest(int sellToGridLimitW, int buyFromGridLimitW, String levlRequestId, 
                              String timestamp, long energyWs, LocalDateTime start, LocalDateTime deadline, 
                              int levlSocWh, int socLowerBoundPercent, int socUpperBoundPercent, 
                              double efficiencyPercent, boolean influenceSellToGrid) {
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
    
    public LevlControlRequest() {
    	
    }

    public static LevlControlRequest from(JsonrpcRequest request) throws OpenemsError.OpenemsNamedException {
        var params = request.getParams();
        return new LevlControlRequest(params);
    }

    public LevlControlRequest(JsonObject params) throws OpenemsError.OpenemsNamedException {
        try {
            this.parseFields(params);
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

    private void parseFields(JsonObject params) {
        this.levlRequestId = params.get("levlRequestId").getAsString();
        this.timestamp = params.get("levlRequestTimestamp").getAsString();
        this.energyWs = params.get("levlEnergyWs").getAsLong();
        this.start = LocalDateTime.now(this.clock).plusSeconds(params.get("levlChargeDelaySec").getAsInt());
        this.deadline = this.start.plusSeconds(params.get("levlChargeDurationSec").getAsInt());
        this.levlSocWh = params.get("levlSocWh").getAsInt();
        this.socLowerBoundPercent = params.get("levlSocLowerBoundPercent").getAsInt();
        this.socUpperBoundPercent = params.get("levlSocUpperBoundPercent").getAsInt();
        this.sellToGridLimitW = params.get("sellToGridLimitW").getAsInt();
        this.buyFromGridLimitW = params.get("buyFromGridLimitW").getAsInt();
        this.efficiencyPercent = params.get("efficiencyPercent").getAsDouble();
        this.influenceSellToGrid = params.has("influenceSellToGrid")
                ? params.get("influenceSellToGrid").getAsBoolean()
                : true;
    }

	@Override
	public int hashCode() {
		return Objects.hash(buyFromGridLimitW, deadline, efficiencyPercent, energyWs, influenceSellToGrid,
				levlRequestId, levlSocWh, sellToGridLimitW, socLowerBoundPercent, socUpperBoundPercent, start,
				timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LevlControlRequest other = (LevlControlRequest) obj;
		return buyFromGridLimitW == other.buyFromGridLimitW
				&& Objects.equals(deadline, other.deadline)
				&& Double.doubleToLongBits(efficiencyPercent) == Double.doubleToLongBits(other.efficiencyPercent)
				&& energyWs == other.energyWs && influenceSellToGrid == other.influenceSellToGrid
				&& Objects.equals(levlRequestId, other.levlRequestId) && levlSocWh == other.levlSocWh
				&& sellToGridLimitW == other.sellToGridLimitW && socLowerBoundPercent == other.socLowerBoundPercent
				&& socUpperBoundPercent == other.socUpperBoundPercent && Objects.equals(start, other.start)
				&& Objects.equals(timestamp, other.timestamp);
	}

	@Override
	public String toString() {
		return "LevlControlRequest [sellToGridLimitW=" + sellToGridLimitW + ", buyFromGridLimitW=" + buyFromGridLimitW
				+ ", levlRequestId=" + levlRequestId + ", timestamp=" + timestamp + ", energyWs=" + energyWs
				+ ", start=" + start + ", deadline=" + deadline + ", levlSocWh=" + levlSocWh + ", socLowerBoundPercent="
				+ socLowerBoundPercent + ", socUpperBoundPercent=" + socUpperBoundPercent + ", efficiencyPercent="
				+ efficiencyPercent + ", influenceSellToGrid=" + influenceSellToGrid + "]";
	}
}
