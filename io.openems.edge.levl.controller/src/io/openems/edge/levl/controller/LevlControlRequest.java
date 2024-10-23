package io.openems.edge.levl.controller;

import com.google.gson.JsonObject;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.edge.levl.controller.common.Limit;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LevlControlRequest extends JsonrpcRequest {
    protected Clock clock = Clock.systemDefaultZone();
    
    public static final String METHOD = "sendLevlControlRequest";
    public int sellToGridLimitW;
    public int buyFromGridLimitW;
    private String levlRequestId;
    private String timestamp;
    public long energyWs;
	private LocalDateTime start;
	private LocalDateTime deadline;
    public int levlSocWh;
    public int socLowerBoundPercent;
    public int socUpperBoundPercent;
    public double efficiencyPercent;
    public boolean influenceSellToGrid;
    private final JsonObject params;

    /**
     * Creates a new LevlControlRequest from a JsonrpcRequest.
     *
     * @param request the JsonrpcRequest
     * @return a new LevlControlRequest
     * @throws OpenemsError.OpenemsNamedException if the request is invalid
     */
    public static LevlControlRequest from(JsonrpcRequest request) throws OpenemsError.OpenemsNamedException {
        var params = request.getParams();
        return new LevlControlRequest(params);
    }

    /**
     * Creates a new LevlControlRequest from a JsonObject.
     *
     * @param params the JsonObject
     * @throws OpenemsError.OpenemsNamedException if the request is invalid
     */
    public LevlControlRequest(JsonObject params) throws OpenemsError.OpenemsNamedException {
        super(LevlControlRequest.METHOD);
        this.params = params;
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
        if (this.efficiencyPercent > 0) {
            throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("efficiencyPercent must be <= 100");
        }
    }

    /**
     * Parses the fields from a JsonObject.
     *
     * @param params the JsonObject
     */
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

    /**
     * Creates a new Limit instance.
     *
     * @return a new Limit instance
     */
    public Limit createGridPowerLimitW() {
        return new Limit(this.sellToGridLimitW, this.buyFromGridLimitW);
    }
    
    public String getLevlRequestId() {
    	return this.levlRequestId;
    }
    
    public String getTimestamp() {
        return this.timestamp;
    }
    
    protected LocalDateTime getStart() {
        return this.start;
    }
    
    protected LocalDateTime getDeadline() {
        return this.deadline;
    }
    
    @Override
    public JsonObject getParams() {
        return this.params;
    }
}