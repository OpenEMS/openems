package io.openems.edge.levl.controller;

import com.google.gson.JsonObject;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.edge.levl.controller.common.Limit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LevlControlRequest extends JsonrpcRequest {
    public static final String METHOD = "sendLevlControlRequest";
    private int sellToGridLimitW;
    private int buyFromGridLimitW;
    private String levlRequestId;
    private String levlRequestTimestamp;
    private int levlPowerW;
    private int levlChargeDelaySec;
    private int levlChargeDurationSec;
    private int totalRealizedDischargeEnergyWh;
    private int levlSocLowerBoundPercent;
    private int levlSocUpperBoundPercent;
    private BigDecimal efficiencyPercent;
    private boolean influenceSellToGrid;
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
        if (this.efficiencyPercent.compareTo(BigDecimal.ZERO) <= 0) {
            throw OpenemsError.JSONRPC_INVALID_MESSAGE.exception("efficiencyPercent must be > 0");
        }
        if (this.efficiencyPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
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
        this.levlRequestTimestamp = params.get("levlRequestTimestamp").getAsString();
        this.levlPowerW = params.get("levlPowerW").getAsInt();
        this.levlChargeDelaySec = params.get("levlChargeDelaySec").getAsInt();
        this.levlChargeDurationSec = params.get("levlChargeDurationSec").getAsInt();
        this.totalRealizedDischargeEnergyWh = -params.get("levlSocWh").getAsInt();
        this.levlSocLowerBoundPercent = params.get("levlSocLowerBoundPercent").getAsInt();
        this.levlSocUpperBoundPercent = params.get("levlSocUpperBoundPercent").getAsInt();
        this.sellToGridLimitW = params.get("sellToGridLimitW").getAsInt();
        this.buyFromGridLimitW = params.get("buyFromGridLimitW").getAsInt();
        this.efficiencyPercent = params.get("efficiencyPercent").getAsBigDecimal();
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
    
    @Override
    public JsonObject getParams() {
        return this.params;
    }
}