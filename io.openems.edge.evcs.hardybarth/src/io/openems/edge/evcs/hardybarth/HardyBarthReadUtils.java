package io.openems.edge.evcs.hardybarth;

import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCount;
import static io.openems.edge.evcs.hardybarth.EvcsHardyBarth.SCALE_FACTOR_MINUS_1;
import static java.lang.Math.round;

import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.PhaseRotation.RotatedPhases;
import io.openems.edge.evcs.api.Status;

public class HardyBarthReadUtils {
	private final EvcsHardyBarthImpl parent;

	private int chargingFinishedCounter = 0;
	private int errorCounter = 0;

	public HardyBarthReadUtils(EvcsHardyBarthImpl parent) {
		this.parent = parent;
	}

	/**
	 * Set the value for every Evcs.ChannelId.
	 *
	 * @param json          given raw data in JSON
	 * @param phaseRotation the configured {@link PhaseRotation}
	 */
	protected void setEvcsChannelIds(JsonElement json, PhaseRotation phaseRotation) {
		final var hb = this.parent;

		// Energy
		hb._setEnergySession(getValueFromJson(STRING, json, //
				value -> {
					if (value == null) {
						return null;
					}
					var chargedata = TypeUtils.<String>getAsType(STRING, value).split("\\|");
					if (chargedata.length == 3) {
						return round(TypeUtils.<Float>getAsType(FLOAT, chargedata[2]) * 1000);
					}
					return null;
				}, "secc", "port0", "salia", "chargedata"));
		hb._setActiveProductionEnergy(getValueFromJson(LONG, json, //
				value -> TypeUtils.<Long>getAsType(LONG, value), //
				"secc", "port0", "metering", "energy", "active_import", "actual"));

		// Current
		final var currentL1 = getAsIntOrElse(json, 0, "secc", "port0", "metering", "current", "ac", "l1", "actual");
		final var currentL2 = getAsIntOrElse(json, 0, "secc", "port0", "metering", "current", "ac", "l2", "actual");
		final var currentL3 = getAsIntOrElse(json, 0, "secc", "port0", "metering", "current", "ac", "l3", "actual");

		// Power
		final var activePowerL1 = getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active", "ac", "l1", "actual");
		final var activePowerL2 = getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active", "ac", "l2", "actual");
		final var activePowerL3 = getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active", "ac", "l3", "actual");

		// Voltage
		final var voltageL1 = activePowerL1 == null ? null : round(activePowerL1 * 1_000_000F / currentL1);
		final var voltageL2 = activePowerL2 == null ? null : round(activePowerL2 * 1_000_000F / currentL2);
		final var voltageL3 = activePowerL3 == null ? null : round(activePowerL3 * 1_000_000F / currentL3);

		var rp = RotatedPhases.from(phaseRotation, //
				voltageL1, currentL1, activePowerL1, //
				voltageL2, currentL2, activePowerL2, //
				voltageL3, currentL3, activePowerL3);
		hb._setVoltageL1(rp.voltageL1());
		hb._setVoltageL2(rp.voltageL2());
		hb._setVoltageL3(rp.voltageL3());
		hb._setCurrentL1(rp.currentL1());
		hb._setCurrentL2(rp.currentL2());
		hb._setCurrentL3(rp.currentL3());
		hb._setActivePowerL1(rp.activePowerL1());
		hb._setActivePowerL2(rp.activePowerL2());
		hb._setActivePowerL3(rp.activePowerL3());

		// Phases: keep last value if no power value was given
		var phases = evaluatePhaseCount(rp.activePowerL1(), rp.activePowerL2(), rp.activePowerL3());
		if (phases != null) {
			hb._setPhases(phases);
			this.parent.debugLog("Used phases: " + phases);
		}

		// ACTIVE_POWER
		final var activePower = Optional.ofNullable(getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active_total", "actual")) //
				.map(p -> p < 100 ? 0 : p) // Ignore the consumption of the charger itself
				.orElse(null);
		this.parent._setActivePower(activePower);

		// STATUS
		var status = getValueFromJson(STRING, json, value -> {
			var stringValue = TypeUtils.<String>getAsType(STRING, value);
			if (stringValue == null) {
				this.errorCounter++;
				this.parent.debugLog("Hardy Barth RAW_STATUS would be null! Raw value: " + value);
				if (this.errorCounter > 3) {
					return Status.ERROR;
				}
				return this.parent.getStatus();
			}

			Status rawStatus = switch (stringValue) {
			case "A" -> Status.NOT_READY_FOR_CHARGING;
			case "B" -> {
				var tmpStatus = Status.READY_FOR_CHARGING;

				// Detect if the car is full
				if (this.parent.getSetChargePowerLimit().orElse(0) >= this.parent.getMinimumHardwarePower().orElse(0)
						&& activePower <= 0) {

					if (this.chargingFinishedCounter >= 90) {
						tmpStatus = Status.CHARGING_FINISHED;
					} else {
						this.chargingFinishedCounter++;
					}
				} else {
					this.chargingFinishedCounter = 0;

					// Charging rejected because we are forcing to pause charging
					if (this.parent.getSetChargePowerLimit().orElse(0) == 0) {
						tmpStatus = Status.CHARGING_REJECTED;
					}
				}
				yield tmpStatus;
			}
			case "C", "D" -> Status.CHARGING;
			case "E", "F" -> {
				this.errorCounter++;
				this.parent.debugLog("Hardy Barth RAW_STATUS would be an error! Raw value: " + stringValue
						+ " - Error counter: " + this.errorCounter);
				if (this.errorCounter > 3) {
					yield Status.ERROR;
				}
				yield this.parent.getStatus();
			}
			default -> {
				this.parent.debugLog("State " + stringValue + " is not a valid state");
				yield Status.UNDEFINED;
			}
			};

			if (!stringValue.equals("B")) {
				this.chargingFinishedCounter = 0;
			}
			if (!stringValue.equals("E") || !stringValue.equals("F")) {
				this.errorCounter = 0;
			}

			return rawStatus;
		}, "secc", "port0", "ci", "charge", "cp", "status");

		this.parent._setStatus(status);
	}

	private static Integer getAsInteger(JsonElement json, float scaleFactor, String... jsonPaths) {
		return getValueFromJson(INTEGER, json, //
				value -> value == null ? null //
						: round(TypeUtils.<Integer>getAsType(INTEGER, value) * scaleFactor), //
				jsonPaths);
	}

	private static int getAsIntOrElse(JsonElement json, int orElse, String... jsonPaths) {
		var result = getValueFromJson(INTEGER, json, //
				value -> TypeUtils.<Integer>getAsType(INTEGER, value), //
				jsonPaths);
		return result == null //
				? orElse //
				: result;
	}

	/**
	 * Get the last JSON element and it's value, by running through the given
	 * jsonPath.
	 *
	 * @param openemsType the {@link OpenemsType}s
	 * @param json        Raw JsonElement.
	 * @param converter   Converter, to convert the raw JSON value into a proper
	 *                    Channel.
	 * @param jsonPaths   Whole JSON path, where the JsonElement for the given
	 *                    channel is located.
	 * @param <T>         return type
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	private static <T> T getValueFromJson(OpenemsType openemsType, JsonElement json, Function<Object, T> converter,
			String... jsonPaths) {

		var currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (var i = 0; i < jsonPaths.length; i++) {
			var currentPathMember = jsonPaths[i];
			try {
				if (i == jsonPaths.length - 1) {
					// Last path element
					var value = getJsonElementValue(currentJsonElement, openemsType, jsonPaths[i]);

					// Return the converted value
					return converter.apply(value);
				}
				// Not last path element
				currentJsonElement = JsonUtils.getAsJsonObject(currentJsonElement, currentPathMember);
			} catch (OpenemsNamedException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Handles a Response froma http call for the endpoint /api GET.
	 * 
	 * @param response      the {@link HttpResponse} to be handled
	 * @param phaseRotation the configured {@link PhaseRotation}
	 * @throws OpenemsNamedException when json can not be parsed
	 */
	public void handleGetApiCallResponse(HttpResponse<String> response, PhaseRotation phaseRotation)
			throws OpenemsNamedException {
		final var json = parseToJsonObject(response.data());
		for (var channelId : EvcsHardyBarth.ChannelId.values()) {
			var jsonPaths = channelId.getJsonPaths();
			var value = getValueFromJson(channelId.doc().getType(), json, channelId.converter, jsonPaths);

			// Set the channel-value
			this.parent.channel(channelId).setNextValue(value);

			if (channelId.equals(EvcsHardyBarth.ChannelId.RAW_SALIA_PUBLISH)) {
				this.parent.masterEvcs = false;
			}

		}
		this.setEvcsChannelIds(json, phaseRotation);
	}

	/**
	 * Get Value of the given JsonElement in the required type.
	 *
	 * @param jsonElement Element as JSON.
	 * @param openemsType Required type.
	 * @param memberName  Member name of the JSON Element.
	 * @return Value in the required type.
	 * @throws OpenemsNamedException Failed to get the value.
	 */
	private static Object getJsonElementValue(JsonElement jsonElement, OpenemsType openemsType, String memberName)
			throws OpenemsNamedException {
		return switch (openemsType) {
		case BOOLEAN -> JsonUtils.getAsInt(jsonElement, memberName) == 1;
		case DOUBLE -> JsonUtils.getAsDouble(jsonElement, memberName);
		case FLOAT -> JsonUtils.getAsFloat(jsonElement, memberName);
		case INTEGER -> JsonUtils.getAsInt(jsonElement, memberName);
		case LONG -> JsonUtils.getAsLong(jsonElement, memberName);
		case SHORT -> JsonUtils.getAsShort(jsonElement, memberName);
		case STRING -> JsonUtils.getAsString(jsonElement, memberName);
		};
	}
}