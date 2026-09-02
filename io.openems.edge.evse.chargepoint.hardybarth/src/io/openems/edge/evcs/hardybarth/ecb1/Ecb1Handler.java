package io.openems.edge.evcs.hardybarth.ecb1;

import static io.openems.common.bridge.http.api.HttpMethod.POST;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static java.lang.Math.round;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.function.BooleanConsumer;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.LatestWinsFutureExecutor;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Handles all HTTP communication with the Hardy Barth cPH1 ECB1 REST API.
 *
 * <p>
 * Reads charge-control status and meter data every cycle and dispatches
 * write commands (start / stop / set current) on demand.
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

	private final EvcsHardyBarthEcb1Impl parent;
	private final String baseUrl;
	private final int chargeControlId;
	private final BridgeHttpFactory httpBridgeFactory;
	private final BridgeHttp httpBridge;
	private final HttpBridgeCycleService cycleService;
	private final LatestWinsFutureExecutor targetExecutor = new LatestWinsFutureExecutor();

	/** Last target current sent to the device (A), or -1 if unknown. */
	private int lastTargetCurrentA = -1;

	public Ecb1Handler(EvcsHardyBarthEcb1Impl parent, String ip, int chargeControlId, int meterId,
			BridgeHttpFactory httpBridgeFactory, HttpBridgeCycleServiceDefinition cycleServiceDef,
			BooleanConsumer communicationFailed) {
		this.parent = parent;
		this.baseUrl = "http://" + ip + "/api/v1";
		this.chargeControlId = chargeControlId;
		this.httpBridgeFactory = httpBridgeFactory;
		this.httpBridge = httpBridgeFactory.get();
		this.cycleService = this.httpBridge.createService(cycleServiceDef);

		// Subscribe for charge-control status (every cycle)
		this.cycleService.subscribeCycle(1, //
				this.baseUrl + "/chargecontrols/" + chargeControlId, //
				response -> {
					this.handleChargeControlResponse(response.response());
					communicationFailed.accept(false);
				}, //
				error -> {
					setValue(this.parent, Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, true);
					communicationFailed.accept(true);
				});

		// Subscribe for meter data (every cycle)
		this.cycleService.subscribeCycle(1, //
				this.baseUrl + "/meters/" + meterId, //
				response -> this.handleMeterResponse(response.response()), //
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
	 * @return true (accepted for dispatch)
	 */
	public boolean setTarget(int currentA) {
		if (currentA == this.lastTargetCurrentA) {
			return true;
		}
		this.targetExecutor.submit(() -> {
			if (currentA == 0) {
				this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/stop") //
						.setMethod(POST) //
						.setBodyFormEncoded(Map.of()) //
						.build());
			} else {
				this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/mode/manual/ampere") //
						.setMethod(POST) //
						.setBodyFormEncoded(Map.of("manualmodeamp", String.valueOf(currentA))) //
						.build());
				this.httpBridge.request(BridgeHttp.create(this.chargeControlUrl() + "/start") //
						.setMethod(POST) //
						.setBodyFormEncoded(Map.of()) //
						.build());
			}
			this.lastTargetCurrentA = currentA;
		});
		return true;
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
	void handleChargeControlResponse(String body) {
		JsonObject json;
		try {
			json = JsonUtils.parseToJsonObject(body);
		} catch (Exception e) {
			this.log.warn("Cannot parse ECB1 charge-control response: " + e.getMessage());
			return;
		}

		var cc = json.getAsJsonObject("chargecontrol");
		if (cc == null) {
			return;
		}

		var stateId = getIntOrNull(cc, "stateid");
		var state = getStringOrNull(cc, "state");
		var mode = getStringOrNull(cc, "mode");
		var connected = getBooleanOrNull(cc, "connected");
		var manualModeAmp = getDoubleOrNull(cc, "manualmodeamp");
		var currentPwmAmp = getDoubleOrNull(cc, "currentpwmamp");
		var vendor = getStringOrNull(cc, "vendor");
		var version = getStringOrNull(cc, "version");

		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_STATE_ID, stateId);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_STATE, state);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_MODE, mode);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED, connected);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_MANUAL_MODE_AMP, manualModeAmp);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_CURRENT_PWM_AMP, currentPwmAmp);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_VENDOR, vendor);
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_VERSION, version);

		// Re-set manual mode if the device has drifted to another mode
		if (mode != null && !mode.equals("manual")) {
			this.setManualMode();
		}

		this.parent._setChargingstationCommunicationFailed(false);
		this.parent._setStatus(this.toStatus(state, stateId, connected));
	}

	/**
	 * Parses a GET /api/v1/meters/{id} response and updates ElectricityMeter channels.
	 *
	 * <p>
	 * OBIS values are in SI base units: W for power, A for current, V for voltage,
	 * Wh for energy. The ElectricityMeter channels expect mA for current and mV for
	 * voltage, so we multiply by 1000 before setting.
	 *
	 * @param body raw JSON response body
	 */
	void handleMeterResponse(String body) {
		JsonObject json;
		try {
			json = JsonUtils.parseToJsonObject(body);
		} catch (Exception e) {
			this.log.warn("Cannot parse ECB1 meter response: " + e.getMessage());
			return;
		}

		var meter = json.getAsJsonObject("meter");
		if (meter == null) {
			return;
		}

		var data = meter.getAsJsonObject("data");
		if (data == null) {
			return;
		}

		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_METER_SERIAL, getIntOrNull(meter, "serial"));
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_METER_VENDOR, getStringOrNull(meter, "vendor"));
		setValue(this.parent, EvcsHardyBarthEcb1.ChannelId.RAW_METER_TYPE, getStringOrNull(meter, "type"));

		// Active power (W)
		var powerTotal = roundToInt(getObisDouble(data, OBIS_POWER_TOTAL));
		var powerL1 = roundToInt(getObisDouble(data, OBIS_POWER_L1));
		var powerL2 = roundToInt(getObisDouble(data, OBIS_POWER_L2));
		var powerL3 = roundToInt(getObisDouble(data, OBIS_POWER_L3));

		this.parent._setActivePower(powerTotal);
		this.parent._setActivePowerL1(powerL1);
		this.parent._setActivePowerL2(powerL2);
		this.parent._setActivePowerL3(powerL3);

		// Current (A → mA)
		var currentL1 = roundToInt(scale(getObisDouble(data, OBIS_CURRENT_L1), 1000.0));
		var currentL2 = roundToInt(scale(getObisDouble(data, OBIS_CURRENT_L2), 1000.0));
		var currentL3 = roundToInt(scale(getObisDouble(data, OBIS_CURRENT_L3), 1000.0));

		this.parent._setCurrentL1(currentL1);
		this.parent._setCurrentL2(currentL2);
		this.parent._setCurrentL3(currentL3);

		// Voltage (V → mV)
		var voltageL1 = roundToInt(scale(getObisDouble(data, OBIS_VOLTAGE_L1), 1000.0));
		var voltageL2 = roundToInt(scale(getObisDouble(data, OBIS_VOLTAGE_L2), 1000.0));
		var voltageL3 = roundToInt(scale(getObisDouble(data, OBIS_VOLTAGE_L3), 1000.0));

		this.parent._setVoltageL1(voltageL1);
		this.parent._setVoltageL2(voltageL2);
		this.parent._setVoltageL3(voltageL3);

		// Energy (Wh)
		var energyWh = getObisDouble(data, OBIS_ENERGY_TOTAL);
		Long energyWhLong = energyWh == null ? null : (long) Math.round(energyWh);
		this.parent._setActiveProductionEnergy(energyWhLong);
		this.parent._setActiveConsumptionEnergy(energyWhLong);
	}

	// -------------------------------------------------------------------------
	// Status mapping
	// -------------------------------------------------------------------------

	private Status toStatus(String state, Integer stateId, Boolean connected) {
		if (state == null || state.isEmpty()) {
			return Status.UNDEFINED;
		}
		var firstChar = state.charAt(0);
		return switch (firstChar) {
		case 'A' -> Status.NOT_READY_FOR_CHARGING;
		case 'B' -> {
			// StateID 17 = explicitly paused by the controller
			if (stateId != null && stateId == 17) {
				yield Status.CHARGING_REJECTED;
			}
			yield Status.READY_FOR_CHARGING;
		}
		case 'C', 'D' -> Status.CHARGING;
		case 'E', 'F' -> Status.ERROR;
		default -> Status.UNDEFINED;
		};
	}

	// -------------------------------------------------------------------------
	// JSON helpers
	// -------------------------------------------------------------------------

	private static Double getObisDouble(JsonObject data, String obisCode) {
		var element = data.get(obisCode);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		try {
			return element.getAsDouble();
		} catch (Exception e) {
			return null;
		}
	}

	private static Integer roundToInt(Double value) {
		return value == null ? null : (int) round(value);
	}

	private static Double scale(Double value, double factor) {
		return value == null ? null : value * factor;
	}

	private static Integer getIntOrNull(JsonObject obj, String key) {
		var el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		try {
			return el.getAsInt();
		} catch (Exception e) {
			return null;
		}
	}

	private static String getStringOrNull(JsonObject obj, String key) {
		var el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		return el.getAsString();
	}

	private static Boolean getBooleanOrNull(JsonObject obj, String key) {
		var el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		try {
			return el.getAsBoolean();
		} catch (Exception e) {
			return null;
		}
	}

	private static Double getDoubleOrNull(JsonObject obj, String key) {
		var el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		try {
			return el.getAsDouble();
		} catch (Exception e) {
			return null;
		}
	}
}
