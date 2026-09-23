package io.openems.edge.evse.chargepoint.hardybarth.ecb1;

import static io.openems.common.bridge.http.api.HttpMethod.POST;
import static io.openems.common.utils.JsonUtils.getAsBooleanOrNull;
import static io.openems.common.utils.JsonUtils.getAsDoubleOrNull;
import static io.openems.common.utils.JsonUtils.getAsIntOrNull;
import static io.openems.common.utils.JsonUtils.getAsStringOrNull;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.TypeUtils.multiply;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonObjectPath;
import io.openems.common.jsonrpc.serialization.JsonObjectPathActual;
import io.openems.common.jsonrpc.serialization.JsonObjectPathActual.JsonObjectPathActualNonNull;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.LatestWinsFutureExecutor;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Handles all HTTP communication with the Hardy Barth cPH1 ECB1 REST API.
 *
 * <p>
 * Reads charge-control status and meter data every cycle and dispatches write
 * commands (start / stop / set current) on demand.
 */
public class Ecb1Handler {

	private static final String OBIS_POWER_TOTAL = "1-0:1.4.0";
	private static final String OBIS_ENERGY_TOTAL = "1-0:1.8.0";
	private static final String OBIS_POWER_L1 = "1-0:21.4.0";
	private static final String OBIS_POWER_L2 = "1-0:41.4.0";
	private static final String OBIS_POWER_L3 = "1-0:61.4.0";
	private static final String OBIS_CURRENT_L1 = "1-0:31.4.0";
	private static final String OBIS_CURRENT_L2 = "1-0:51.4.0";
	private static final String OBIS_CURRENT_L3 = "1-0:71.4.0";
	private static final String OBIS_VOLTAGE_L1 = "1-0:32.4.0";
	private static final String OBIS_VOLTAGE_L2 = "1-0:52.4.0";
	private static final String OBIS_VOLTAGE_L3 = "1-0:72.4.0";

	private final Logger log = LoggerFactory.getLogger(Ecb1Handler.class);

	private final Ecb1Parent parent;
	private final String baseUrl;
	private final int chargeControlId;
	private final BridgeHttpFactory httpBridgeFactory;
	private final BridgeHttp httpBridge;
	private final HttpBridgeCycleService cycleService;
	private final LatestWinsFutureExecutor targetExecutor = new LatestWinsFutureExecutor();

	/** Last target current sent to the device (A), or -1 if unknown. */
	private int lastTargetCurrentA = -1;

	public Ecb1Handler(Ecb1Parent parent, String ip, int chargeControlId, int meterId,
			BridgeHttpFactory httpBridgeFactory, HttpBridgeCycleServiceDefinition cycleServiceDef) {
		this.parent = parent;
		this.baseUrl = "http://" + ip + "/api/v1";
		this.chargeControlId = chargeControlId;
		this.httpBridgeFactory = httpBridgeFactory;
		this.httpBridge = httpBridgeFactory.get();
		this.cycleService = this.httpBridge.createService(cycleServiceDef);

		// Subscribe for charge-control status (every cycle)
		this.cycleService.subscribeCycle(1, //
				this.baseUrl + "/chargecontrols/" + chargeControlId, //
				response -> this.handleChargeControlResponse(response.data()), //
				error -> this.parent.onCommunicationFailed(true));

		// Subscribe for meter data (every cycle)
		this.cycleService.subscribeCycle(1, //
				this.baseUrl + "/meters/" + meterId, //
				response -> this.handleMeterResponse(response.data()), //
				error -> this.log.warn("Failed to read ECB1 meter: " + error));

		// Set manual mode on startup
		this.setManualMode();
	}

	/**
	 * Releases the HTTP bridge on deactivation.
	 */
	public void deactivate() {
		this.targetExecutor.cancel();
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	/**
	 * Sets the charge target current. Call with 0 to stop charging.
	 *
	 * @param currentA target current in Ampere (0 = stop)
	 * @return true if accepted for dispatch; false if the executor was cancelled
	 */
	public boolean setTarget(int currentA) {
		if (currentA == this.lastTargetCurrentA) {
			return true;
		}
		try {
			this.targetExecutor.execute(//
					() -> this.dispatchTarget(currentA), //
					(response, error) -> {
						if (error == null) {
							this.lastTargetCurrentA = currentA;
						}
					});
			return true;
		} catch (RejectedExecutionException e) {
			return false;
		}
	}

	private CompletableFuture<HttpResponse<String>> dispatchTarget(int currentA) {
		if (currentA == 0) {
			return this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/stop") //
					.setMethod(POST) //
					.setBodyFormEncoded(Map.of()) //
					.build());
		}
		this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/mode/manual/ampere") //
				.setMethod(POST) //
				.setBodyFormEncoded(Map.of("manualmodeamp", String.valueOf(currentA))) //
				.build());
		return this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/start") //
				.setMethod(POST) //
				.setBodyFormEncoded(Map.of()) //
				.build());
	}

	/**
	 * Sends a one-shot request to set the ECB1 charge mode to "manual".
	 */
	private void setManualMode() {
		this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/mode") //
				.setMethod(POST) //
				.setBodyFormEncoded(Map.of("mode", "manual")) //
				.build());
	}

	private String chargeControlUrl() {
		return this.baseUrl + "/chargecontrols/" + this.chargeControlId;
	}

	// -------------------------------------------------------------------------
	// Response handlers
	// -------------------------------------------------------------------------

	/**
	 * Parses a GET /api/v1/chargecontrols/{id} response and updates channels.
	 *
	 * @param body raw JSON response body
	 */
	public void handleChargeControlResponse(String body) {
		JsonObject json;
		try {
			json = JsonUtils.parseToJsonObject(body);
		} catch (Exception e) {
			this.log.warn("Cannot parse ECB1 charge-control response: " + e.getMessage());
			json = new JsonObject();
		}

		final var cc = JsonUtils.getAsOptionalJsonObject(json, "chargecontrol").orElse(null);
		final var hb = this.parent;

		final var stateId = getAsIntOrNull(cc, "stateid");
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_STATE_ID, stateId);
		final var state = getAsStringOrNull(cc, "state");
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_STATE, state);
		final var mode = getAsStringOrNull(cc, "mode");
		setValue(this.parent, EvseChargePointHardyBarthEcb1.ChannelId.RAW_MODE, mode);
		final var connected = getAsBooleanOrNull(cc, "connected");
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_CONNECTED, connected);
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_MANUAL_MODE_AMP,
				getAsDoubleOrNull(cc, "manualmodeamp"));
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_CURRENT_PWM_AMP,
				getAsDoubleOrNull(cc, "currentpwmamp"));
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_VENDOR, getAsStringOrNull(cc, "vendor"));
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_VERSION, getAsStringOrNull(cc, "version"));

		// Re-set manual mode if the device has drifted to another mode
		if (mode != null && !mode.equals("manual")) {
			this.setManualMode();
		}

		hb.onCommunicationFailed(false);
		hb.onChargeControlStatus(state, stateId, connected);
	}

	/**
	 * Parses a GET /api/v1/meters/{id} response and updates ElectricityMeter
	 * channels.
	 *
	 * <p>
	 * OBIS values are in SI base units: W for power, A for current, V for voltage,
	 * Wh for energy. The ElectricityMeter channels expect mA for current and mV for
	 * voltage, so we multiply by 1000 before setting.
	 *
	 * @param body raw JSON response body
	 */
	public void handleMeterResponse(String body) {
		JsonObject json;
		try {
			json = JsonUtils.parseToJsonObject(body);
		} catch (OpenemsNamedException e) {
			this.log.warn("Cannot parse ECB1 meter response: " + e.getMessage());
			json = new JsonObject();
		}

		final var meter = JsonUtils.getAsOptionalJsonObject(json, "meter").orElse(null);
		final var data = JsonUtils.getAsOptionalJsonObject(meter, "data").orElse(null);
		final var hb = this.parent;

		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_SERIAL, getAsIntOrNull(meter, "serial"));
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_VENDOR, getAsStringOrNull(meter, "vendor"));
		setValue(hb, EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_TYPE, getAsStringOrNull(meter, "type"));

		// Active power (W)
		setValue(hb, ElectricityMeter.ChannelId.ACTIVE_POWER, getAsDoubleOrNull(data, OBIS_POWER_TOTAL));
		setValue(hb, ElectricityMeter.ChannelId.ACTIVE_POWER_L1, getAsDoubleOrNull(data, OBIS_POWER_L1));
		setValue(hb, ElectricityMeter.ChannelId.ACTIVE_POWER_L2, getAsDoubleOrNull(data, OBIS_POWER_L2));
		setValue(hb, ElectricityMeter.ChannelId.ACTIVE_POWER_L3, getAsDoubleOrNull(data, OBIS_POWER_L3));

		// Current (A → mA)
		setValue(hb, ElectricityMeter.ChannelId.CURRENT_L1, multiply(getAsDoubleOrNull(data, OBIS_CURRENT_L1), 1000.0));
		setValue(hb, ElectricityMeter.ChannelId.CURRENT_L2, multiply(getAsDoubleOrNull(data, OBIS_CURRENT_L2), 1000.0));
		setValue(hb, ElectricityMeter.ChannelId.CURRENT_L3, multiply(getAsDoubleOrNull(data, OBIS_CURRENT_L3), 1000.0));

		// Voltage (V → mV)
		setValue(hb, ElectricityMeter.ChannelId.VOLTAGE_L1, multiply(getAsDoubleOrNull(data, OBIS_VOLTAGE_L1), 1000.0));
		setValue(hb, ElectricityMeter.ChannelId.VOLTAGE_L2, multiply(getAsDoubleOrNull(data, OBIS_VOLTAGE_L2), 1000.0));
		setValue(hb, ElectricityMeter.ChannelId.VOLTAGE_L3, multiply(getAsDoubleOrNull(data, OBIS_VOLTAGE_L3), 1000.0));

		// Energy (Wh)
		setValue(hb, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, getAsDoubleOrNull(data, OBIS_ENERGY_TOTAL));
	}
}
