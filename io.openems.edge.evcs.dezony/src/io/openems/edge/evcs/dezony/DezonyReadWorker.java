package io.openems.edge.evcs.dezony;

import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsLong;
import static io.openems.common.utils.JsonUtils.getAsShort;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;

import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;

public class DezonyReadWorker extends AbstractCycleWorker {

	private final EvcsDezonyImpl parent;

	private final Map<String, Status> dezonyToOpenemsState = Map.of(//
			"IDLE", Status.NOT_READY_FOR_CHARGING, //
			"CAR_CONNECTED", Status.READY_FOR_CHARGING, //
			"CHARGING", Status.CHARGING, //
			"CHARGING_FINISHED", Status.CHARGING_FINISHED, //
			"CHARGING_ERROR", Status.ERROR);

	private int chargingFinishedCounter = 0;

	public DezonyReadWorker(EvcsDezonyImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws OpenemsNamedException {
		final var state = this.parent.api.getState();
		final var lastMetrics = this.parent.api.getLastMetrics();

		if (state == null || lastMetrics == null) {
			return;
		}

		// TODO: if there was an Exception or state/lastMetrics is null, Channels must
		// still be set! Otherwise they keep the old, invalid value.
		this.setEnergySession(lastMetrics);
		this.setEvcsChannelIds(state);
	}

	/**
	 * Set the value for every Evcs.ChannelId.
	 *
	 * @param json Given raw data in JSON
	 */
	private void setEvcsChannelIds(JsonElement json) {
		final var energyArray = getArrayFromJson(json, "currDataPoint");

		this.parent._setActiveProductionEnergy(getValueByKey(energyArray, "etotal"));
		this.parent._setActivePower(getValueByKey(energyArray, "ptotal"));
		this.parent._setSetChargePowerLimit(
				getValueByKey(energyArray, "curlhm") * THREE_PHASE.getValue() * Evcs.DEFAULT_VOLTAGE);
		this.parent._setPhases(this.calculatePhases(energyArray));
		this.parent._setStatus(this.getStatus(json));
	}

	private void setEnergySession(JsonElement json) {
		final var energy = (Double) getValueFromJson(Evcs.ChannelId.ENERGY_SESSION, DOUBLE, json, value -> {
			return value;
		}, "metric", "energy");

		this.parent._setEnergySession(energy == null ? null : (int) Math.round(energy));
	}

	private Status getStatus(JsonElement json) {
		final var rawChargeStatus = (String) getValueFromJson(EvcsDezony.ChannelId.RAW_CHARGE_STATUS_CHARGEPOINT, json,
				value -> {
					final String state = TypeUtils.getAsType(STRING, value);
					return state == null ? "" : state;
				}, "state");

		var status = this.dezonyToOpenemsState.getOrDefault(rawChargeStatus, Status.UNDEFINED);

		if (status.equals(Status.READY_FOR_CHARGING)) {
			int setChargePowerLimit = this.parent.getSetChargePowerLimit().orElse(0);
			int minimumHardwarePower = this.parent.getMinimumHardwarePower().orElse(0);

			if (setChargePowerLimit >= minimumHardwarePower) {
				if (this.chargingFinishedCounter >= 90) {
					status = Status.CHARGING_FINISHED;
				} else {
					this.chargingFinishedCounter++;
				}
			} else {
				this.chargingFinishedCounter = 0;

				if (setChargePowerLimit == 0) {
					status = Status.CHARGING_REJECTED;
				}
			}
		}

		return status;
	}

	private Integer calculatePhases(JsonArray energyArray) {
		final var powerL1 = getValueByKey(energyArray, "currl1") * Evcs.DEFAULT_VOLTAGE;
		final var powerL2 = getValueByKey(energyArray, "currl2") * Evcs.DEFAULT_VOLTAGE;
		final var powerL3 = getValueByKey(energyArray, "currl3") * Evcs.DEFAULT_VOLTAGE;
		final int maxPower = 900;
		final int minPower = 300;

		final var sum = powerL1 + powerL2 + powerL3;

		Integer phases = null;

		if (sum > maxPower) {
			phases = 0;

			if (powerL1 >= minPower) {
				phases += 1;
			}

			if (powerL2 >= minPower) {
				phases += 1;
			}

			if (powerL3 >= minPower) {
				phases += 1;
			}
		}

		if (phases != null) {
			this.parent.debugLog("Used phases: " + phases);
		}

		return phases;
	}

	private static int getValueByKey(JsonArray energyArray, String searchKey) {
		for (var i = 0; i < energyArray.size(); ++i) {
			final var object = energyArray.get(i).getAsJsonObject();
			final var key = object.get("short");

			if (key.getAsString().equals(searchKey)) {
				return object.get("value").isJsonNull() ? 0 : object.get("value").getAsInt();
			}

			continue;
		}

		return 0;
	}

	/**
	 * Call the getValueFromJson without a divergent type in the raw json.
	 *
	 * @param channelId Channel that value will be detect.
	 * @param json      Raw JsonElement.
	 * @param converter Converter, to convert the raw JSON value into a proper
	 *                  Channel.
	 * @param jsonPaths Whole JSON path, where the JsonElement for the given channel
	 *                  is located.
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	private static Object getValueFromJson(ChannelId channelId, JsonElement json, Function<Object, Object> converter,
			String... jsonPaths) {
		return getValueFromJson(channelId, null, json, converter, jsonPaths);
	}

	/**
	 * Get the last JSON element and it's value, by running through the given
	 * jsonPath.
	 *
	 * @param channelId              Channel that value will be detect.
	 * @param divergentTypeInRawJson Divergent type of the value in the depending
	 *                               JsonElement.
	 * @param json                   Raw JsonElement.
	 * @param converter              Converter, to convert the raw JSON value into a
	 *                               proper Channel.
	 * @param jsonPaths              Whole JSON path, where the JsonElement for the
	 *                               given channel is located.
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	private static Object getValueFromJson(ChannelId channelId, OpenemsType divergentTypeInRawJson, JsonElement json,
			Function<Object, Object> converter, String... jsonPaths) {
		var currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (var i = 0; i < jsonPaths.length; i++) {
			var currentPathMember = jsonPaths[i];
			try {
				if (i == jsonPaths.length - 1) {
					//
					var openemsType = divergentTypeInRawJson == null ? channelId.doc().getType()
							: divergentTypeInRawJson;

					// Last path element
					var value = getJsonElementValue(currentJsonElement, openemsType, jsonPaths[i]);

					// Return the converted value
					return converter.apply(value);
				}
				// Not last path element
				currentJsonElement = getAsJsonObject(currentJsonElement, currentPathMember);
			} catch (OpenemsNamedException e) {
				return null;
			}
		}
		return null;
	}

	private static JsonArray getArrayFromJson(JsonElement json, String... jsonPaths) {
		var currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (var i = 0; i < jsonPaths.length; i++) {
			var currentPathMember = jsonPaths[i];
			try {
				if (i == jsonPaths.length - 1) {
					return JsonUtils.getAsJsonArray(currentJsonElement, jsonPaths[i]);
				}
				// Not last path element
				currentJsonElement = getAsJsonObject(currentJsonElement, currentPathMember);
			} catch (OpenemsNamedException e) {
				return null;
			}
		}
		return null;
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
		// NOTE: could have used JsonUtils.getAsType()
		return switch (openemsType) {
		case BOOLEAN -> getAsInt(jsonElement, memberName) == 1;
		case DOUBLE -> getAsDouble(jsonElement, memberName);
		case FLOAT -> getAsFloat(jsonElement, memberName);
		case INTEGER -> getAsInt(jsonElement, memberName);
		case LONG -> getAsLong(jsonElement, memberName);
		case SHORT -> getAsShort(jsonElement, memberName);
		case STRING -> getAsString(jsonElement, memberName);
		};
	}
}
