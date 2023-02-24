package io.openems.edge.evcs.dezony;

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
	private final DezonyImpl parent;
	private int chargingFinishedCounter = 0;

	public DezonyReadWorker(DezonyImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws OpenemsNamedException {
		final var json = this.parent.api.sendGetRequest("/api/v1/state");
		final var metricLast = this.parent.api.sendGetRequest("/api/v1/metrics/last");
		
		if (json == null || metricLast == null) {
			return;
		}

		// Set value for every Dezony.ChannelId
		for (var channelId : Dezony.ChannelId.values()) {
			var jsonPaths = channelId.getJsonPaths();
			var value = this.getValueFromJson(channelId, json, channelId.converter, jsonPaths);

			// Set the channel-value
			this.parent.channel(channelId).setNextValue(value);
		}
	
		this.setEnergySession(metricLast);

		// Set value for every Evcs.ChannelId
		this.setEvcsChannelIds(json);
	}
	
	// TODO: Implement setting maximum power
	private void setMaximumPower(JsonElement json) {
		final var power = (Integer) this.getValueFromJson(Evcs.ChannelId.MAXIMUM_POWER, OpenemsType.INTEGER, json, value -> {			
			return value;
		}, "charging_current");
		
		
		this.parent._setMaximumPower((int) Math.round(power * (double) this.parent.getPhasesAsInt() * 230.0));
	}

	
	private void setEnergySession(JsonElement json) {
		final var energy = (Double) this.getValueFromJson(Evcs.ChannelId.ENERGY_SESSION, OpenemsType.DOUBLE, json, value -> {			
			return value;
		}, "metric", "energy");
		
		
		this.parent._setEnergySession(energy == null ? null : (int) Math.round(energy));
	}
	
	/**
	 * Set the value for every Evcs.ChannelId.
	 *
	 * @param json Given raw data in JSON
	 */
	private void setEvcsChannelIds(JsonElement json) {
		// ACTIVE_CONSUMPTION_ENERGY
		final var activeConsumptionEnergyArray = this.getArrayFromJson(json,"currDataPoint"); //
		long activeConsumptionEnergy = 0;
		var chargePower = 0;
		
		Integer powerL1 = 0;
		Integer powerL2 = 0;
		Integer powerL3 = 0;
		
		for (var i = 0; i < activeConsumptionEnergyArray.size(); ++i) {
			final var object = activeConsumptionEnergyArray.get(i).getAsJsonObject();
			final var key = object.get("short");
			
			if (key.getAsString().equals("etotal")) {
				activeConsumptionEnergy = object.get("value").getAsInt();
				continue;
			}
			
			if (key.getAsString().equals("currl1")) {
				powerL1 = object.get("value").isJsonNull() ? 0 :object.get("value").getAsInt() * 230;
				continue;
			}
			
			if (key.getAsString().equals("currl2")) {
				powerL2 = object.get("value").isJsonNull() ? 0 :object.get("value").getAsInt() * 230;
				continue;
			}
			
			if (key.getAsString().equals("currl3")) {
				powerL3 = object.get("value").isJsonNull() ? 0 :object.get("value").getAsInt() * 230;
				continue;
			}
			
			if (key.getAsString().equals("ptotal")) {
				chargePower = object.get("value").isJsonNull() ? null :object.get("value").getAsInt();
				this.parent._setChargePower(chargePower);
			}
		}
		
		this.parent._setMaximumPower(chargingFinishedCounter);

		// PHASES
		Integer phases = null;
		
			var sum = powerL1 + powerL2 + powerL3;

			if (sum > 900) {
				phases = 0;

				if (powerL1 >= 300) {
					phases += 1;
				}
				if (powerL2 >= 300) {
					phases += 1;
				}
				if (powerL3 >= 300) {
					phases += 1;
				}
			}

		
		this.parent._setPhases(phases);
		
		if (phases != null) {
			this.parent.debugLog("Used phases: " + phases);
		}

		// STATUS
		var status = (Status) this.getValueFromJson(Dezony.ChannelId.RAW_CHARGE_STATUS_CHARGEPOINT, json, value -> {
			final String stringValue = TypeUtils.getAsType(OpenemsType.STRING, value);
			
			if (stringValue == null) {
				return Status.UNDEFINED;
			}

			var rawStatus = Status.UNDEFINED;
			
			switch (stringValue) {
			case "IDLE":
				rawStatus = Status.NOT_READY_FOR_CHARGING;
				break;
			case "CAR_CONNECTED":
				rawStatus = Status.READY_FOR_CHARGING;

				// Detect if the car is full
				if (this.parent.getSetChargePowerLimit().orElse(0) >= this.parent.getMinimumHardwarePower().orElse(0)) {

					if (this.chargingFinishedCounter >= 90) {
						rawStatus = Status.CHARGING_FINISHED;
					} else {
						this.chargingFinishedCounter++;
					}
				} else {
					this.chargingFinishedCounter = 0;

					// Charging rejected because we are forcing to pause charging
					if (this.parent.getSetChargePowerLimit().orElse(0) == 0) {
						rawStatus = Status.CHARGING_REJECTED;
					}
				}
				break;
			case "CHARGING":
				rawStatus = Status.CHARGING;
				break;
			case "CHARGING_FINISHED":
				rawStatus = Status.CHARGING_FINISHED;
				break;
			case "CHARGING_ERROR":
			case "F":
				rawStatus = Status.ERROR;
				break;
			default:
				rawStatus = Status.UNDEFINED;
				break;
			}
			if (stringValue.equals("B")) {
				this.chargingFinishedCounter = 0;
			}
			return rawStatus;
		}, "state");

		this.parent._setStatus(status);
	}

	/**
	 * Call the getValueFromJson with the detailed information of the channel.
	 *
	 * @param channelId Channel that value will be detect.
	 * @param json      Whole JSON path, where the JsonElement for the given channel
	 *                  is located.
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	private Object getValueForChannel(Dezony.ChannelId channelId, JsonElement json) {
		return this.getValueFromJson(channelId, json, channelId.converter, channelId.getJsonPaths());
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
	private Object getValueFromJson(ChannelId channelId, JsonElement json, Function<Object, Object> converter,
			String... jsonPaths) {
		return this.getValueFromJson(channelId, null, json, converter, jsonPaths);
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
	private Object getValueFromJson(ChannelId channelId, OpenemsType divergentTypeInRawJson, JsonElement json,
			Function<Object, Object> converter, String... jsonPaths) {

		var currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (var i = 0; i < jsonPaths.length; i++) {
			var currentPathMember = jsonPaths[i];
			// System.out.println(currentPathMember);
			try {
				if (i == jsonPaths.length - 1) {
					//
					var openemsType = divergentTypeInRawJson == null ? channelId.doc().getType()
							: divergentTypeInRawJson;

					// Last path element
					var value = this.getJsonElementValue(currentJsonElement, openemsType, jsonPaths[i]);

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
	
	private JsonArray getArrayFromJson(JsonElement json, String... jsonPaths) {

		var currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (var i = 0; i < jsonPaths.length; i++) {
			var currentPathMember = jsonPaths[i];
			// System.out.println(currentPathMember);
			try {
				if (i == jsonPaths.length - 1) {
					return JsonUtils.getAsJsonArray(currentJsonElement, jsonPaths[i]);
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
	 * Get Value of the given JsonElement in the required type.
	 *
	 * @param jsonElement Element as JSON.
	 * @param openemsType Required type.
	 * @param memberName  Member name of the JSON Element.
	 * @return Value in the required type.
	 * @throws OpenemsNamedException Failed to get the value.
	 */
	private Object getJsonElementValue(JsonElement jsonElement, OpenemsType openemsType, String memberName)
			throws OpenemsNamedException {
		final Object value;

		switch (openemsType) {
		case BOOLEAN:
			value = JsonUtils.getAsInt(jsonElement, memberName) == 1;
			break;
		case DOUBLE:
			value = JsonUtils.getAsDouble(jsonElement, memberName);
			break;
		case FLOAT:
			value = JsonUtils.getAsFloat(jsonElement, memberName);
			break;
		case INTEGER:
			value = JsonUtils.getAsInt(jsonElement, memberName);
			break;
		case LONG:
			value = JsonUtils.getAsLong(jsonElement, memberName);
			break;
		case SHORT:
			value = JsonUtils.getAsShort(jsonElement, memberName);
			break;
		case STRING:
			value = JsonUtils.getAsString(jsonElement, memberName);
			break;
		default:
			value = JsonUtils.getAsString(jsonElement, memberName);
			break;
		}
		return value;
	}
}
