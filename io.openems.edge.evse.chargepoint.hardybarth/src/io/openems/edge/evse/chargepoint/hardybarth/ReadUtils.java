package io.openems.edge.evse.chargepoint.hardybarth;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static java.lang.Math.round;

import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;

public class ReadUtils {

	public static final float SCALE_FACTOR_MINUS_1 = 0.1F;

	private final EvseChargePointHardyImpl parent;

	private int errorCounter = 0;
	private int undefinedErrorCounter = 0;

	public ReadUtils(EvseChargePointHardyImpl parent) {
		this.parent = parent;
	}

	/**
	 * Set the value for every Evcs.ChannelId.
	 *
	 * @param json given raw data in JSON
	 */
	protected void setEvcsChannelIds(JsonElement json) {
		final var hb = this.parent;
		hb._setActiveProductionEnergy(getValueFromJson(OpenemsType.LONG, json, //
				value -> TypeUtils.<Long>getAsType(OpenemsType.LONG, value), //
				"secc", "port0", "metering", "energy", "active_import", "actual"));

		// Current
		final var currentL1 = getAsInteger(json, 1, "secc", "port0", "metering", "current", "ac", "l1", "actual");
		final var currentL2 = getAsInteger(json, 1, "secc", "port0", "metering", "current", "ac", "l2", "actual");
		final var currentL3 = getAsInteger(json, 1, "secc", "port0", "metering", "current", "ac", "l3", "actual");

		// Checks if value are null and if they are checks if its the third error or
		// more
		// and otherwise returns, so that no new values are written this cycle
		// TODO: find a better long term solution
		if (currentL1 == null || currentL2 == null || currentL3 == null) {
			if (this.handleUndefinedError()) {
				return;
			}
		}

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

		if (activePowerL1 == null || activePowerL2 == null || activePowerL3 == null) {
			if (this.handleUndefinedError()) {
				return;
			}
		}

		if (voltageL1 == null || voltageL2 == null || voltageL3 == null) {
			if (this.handleUndefinedError()) {
				return;
			}
		}

		PhaseRotation.setPhaseRotatedCurrentChannels(hb, currentL1, currentL2, currentL3);
		PhaseRotation.setPhaseRotatedVoltageChannels(hb, voltageL1, voltageL2, voltageL3);
		PhaseRotation.setPhaseRotatedActivePowerChannels(hb, activePowerL1, activePowerL2, activePowerL3);

		// ACTIVE_POWER
		final var activePower = Optional.ofNullable(getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active_total", "actual")) //
				.map(p -> p < 100 ? 0 : p) // Ignore the consumption of the charger itself
				.orElse(null);

		if (activePower == null) {
			if (this.handleUndefinedError()) {
				return;
			}
		}

		hb._setActivePower(activePower);
		this.undefinedErrorCounter = 0;

		// STATUS
		var status = getValueFromJson(STRING, json, value -> {
			var stringValue = TypeUtils.<String>getAsType(STRING, value);
			if (stringValue == null) {
				this.errorCounter++;
				if (this.errorCounter > 3) {
					return ChargePointStatus.UNDEFINED;
				}
			}

			ChargePointStatus rawStatus = switch (stringValue) {
			case "A" -> ChargePointStatus.A;
			case "B" -> ChargePointStatus.B;
			case "C" -> ChargePointStatus.C;
			case "D" -> ChargePointStatus.D;
			case "E" -> {
				this.errorCounter++;
				if (this.errorCounter > 3) {
					yield ChargePointStatus.E;
				}
				yield hb.getChargePointStatus();
			}
			case "F" -> {
				this.errorCounter++;
				if (this.errorCounter > 3) {
					yield ChargePointStatus.F;
				}
				yield hb.getChargePointStatus();

			}
			default -> {
				yield ChargePointStatus.UNDEFINED;
			}
			};
			if (!stringValue.equals("E") || !stringValue.equals("F")) {
				this.errorCounter = 0;
			}
			return rawStatus;
		}, "secc", "port0", "ci", "charge", "cp", "status");
		setValue(hb, EvseChargePointHardy.ChannelId.STATUS, status);

		var isReadyForCharging = switch (status) {
		case A, E, F, UNDEFINED -> false;
		case B, C, D -> true;
		};
		setValue(hb, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReadyForCharging);
	}

	private static Integer getAsInteger(JsonElement json, float scaleFactor, String... jsonPaths) {
		return getValueFromJson(INTEGER, json, //
				value -> value == null ? null //
						: round(TypeUtils.<Integer>getAsType(INTEGER, value) * scaleFactor), //
				jsonPaths);
	}

	private boolean handleUndefinedError() {
		this.undefinedErrorCounter++;
		return this.undefinedErrorCounter <= 3;
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
	 * @param response the {@link HttpResponse} to be handled
	 * @throws OpenemsNamedException when json can not be parsed
	 */
	public void handleGetApiCallResponse(HttpResponse<String> response) throws OpenemsNamedException {
		final var json = JsonUtils.parseToJsonObject(response.data());
		for (var channelId : EvseChargePointHardy.ChannelId.values()) {
			var jsonPaths = channelId.getJsonPaths();
			var value = getValueFromJson(channelId.doc().getType(), json, channelId.converter, jsonPaths);

			// Set the channel-value
			this.parent.channel(channelId).setNextValue(value);

			if (channelId.equals(EvseChargePointHardy.ChannelId.RAW_SALIA_PUBLISH)) {
				// TODO handle master/slave
			}
		}

		this.setEvcsChannelIds(json);
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
