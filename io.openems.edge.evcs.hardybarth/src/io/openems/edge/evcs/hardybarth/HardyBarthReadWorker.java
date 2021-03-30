package io.openems.edge.evcs.hardybarth;

import java.util.function.Function;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;

public class HardyBarthReadWorker extends AbstractCycleWorker {

	private final HardyBarthImpl parent;

	public HardyBarthReadWorker(HardyBarthImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws OpenemsNamedException {

		// TODO: Read separate JSON files
		// - separate configuration -> this.api.sendGetRequest("/saliaconf.json");
		// e.g. min & max hardware power
		// - customeredit values -> this.api.sendGetRequest("/customer.json");
		// - rfidtags -> this.api.sendGetRequest("/rfidtags.json");
		// - chargelogs -> this.api.sendGetRequest("/chargelogs.json");

		JsonElement json = this.parent.api.sendGetRequest("/api");
		if (json == null) {
			return;
		}

		// Set value for every HardyBarth.ChannelId
		for (HardyBarth.ChannelId channelId : HardyBarth.ChannelId.values()) {
			String[] jsonPaths = channelId.getJsonPaths();
			Object value = this.getValueFromJson(channelId, json, channelId.converter, jsonPaths);

			// Set the channel-value
			this.parent.channel(channelId).setNextValue(value);

			if (channelId.equals(HardyBarth.ChannelId.RAW_SALIA_PUBLISH)) {
				this.parent.masterEvcs = false;
			}
		}

		// Set value for every Evcs.ChannelId
		this.setEvcsChannelIds(json);
	}

	/**
	 * Set the value for every Evcs.ChannelId.
	 * 
	 * @param json Given raw data in JSON
	 */
	private void setEvcsChannelIds(JsonElement json) {

		// CHARGE_POWER
		Long chargePower = (Long) this.getValueFromJson(Evcs.ChannelId.CHARGE_POWER, json, (value) -> {
			if (value == null) {
				return null;
			}
			return Math.round((Integer) value * 0.1);
		}, "secc", "port0", "metering", "power", "active_total", "actual");
		this.parent._setChargePower(chargePower == null ? null : chargePower.intValue());

		// ENERGY_SESSION
		Double energy = (Double) this.getValueFromJson(Evcs.ChannelId.ENERGY_SESSION, OpenemsType.STRING, json,
				(value) -> {
					if (value == null) {
						return null;
					}
					Double rawEnergy = null;
					String[] chargedata = value.toString().split("\\|");
					if (chargedata.length == 3) {
						rawEnergy = Double.parseDouble(chargedata[2]) * 1000;
					}
					return rawEnergy;

				}, "secc", "port0", "salia", "chargedata");
		this.parent._setEnergySession(energy == null ? null : (int) Math.round(energy));

		// ACTIVE_CONSUMPTION_ENERGY
		Long activeConsumptionEnergy = (Long) this.getValueFromJson(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, json,
				(value) -> (long) (Double.parseDouble(value.toString()) * 0.1), "secc", "port0", "metering", "energy",
				"active_import", "actual"); //
		this.parent._setActiveConsumptionEnergy(activeConsumptionEnergy);

		// PHASES
		Double powerL1 = (Double) this.getValueFromJson(HardyBarth.ChannelId.RAW_ACTIVE_POWER_L1, json,
				(value) -> Double.parseDouble(value.toString()) * 0.1, "secc", "port0", "metering", "power", "active",
				"ac", "l1", "actual");
		Double powerL2 = (Double) this.getValueFromJson(HardyBarth.ChannelId.RAW_ACTIVE_POWER_L2, json,
				(value) -> Double.parseDouble(value.toString()) * 0.1, "secc", "port0", "metering", "power", "active",
				"ac", "l2", "actual");
		Double powerL3 = (Double) this.getValueFromJson(HardyBarth.ChannelId.RAW_ACTIVE_POWER_L3, json,
				(value) -> Double.parseDouble(value.toString()) * 0.1, "secc", "port0", "metering", "power", "active",
				"ac", "l3", "actual");
		Integer phases = null;
		if (powerL1 != null && powerL2 != null && powerL3 != null) {

			Double sum = powerL1 + powerL2 + powerL3;

			if (sum > 300) {
				phases = 0;

				if (powerL1 >= 100) {
					phases += 1;
				}
				if (powerL2 >= 100) {
					phases += 1;
				}
				if (powerL3 >= 100) {
					phases += 1;
				}
			}
		}
		this.parent._setPhases(phases);
		if (phases != null) {
			this.parent.debugLog("Used phases: " + phases);
		}

		// STATUS
		Status status = (Status) this.getValueFromJson(HardyBarth.ChannelId.RAW_CHARGE_STATUS_CHARGEPOINT, json,
				(value) -> {
					Status rawStatus = Status.UNDEFINED;
					switch (value.toString()) {
					case "A":
						rawStatus = Status.NOT_READY_FOR_CHARGING;
						break;
					case "B":
						rawStatus = Status.CHARGING_REJECTED;
						if (this.parent.getSetChargePowerLimit().orElse(0) > this.parent.getMinimumHardwarePower()
								.orElse(0)) {
							rawStatus = Status.CHARGING_FINISHED;
						}
						break;
					case "C":
						rawStatus = Status.CHARGING;
						break;
					case "D":
						rawStatus = Status.CHARGING;
						break;
					case "E":
					case "F":
						rawStatus = Status.ERROR;
						break;
					default:
						rawStatus = Status.UNDEFINED;
						break;
					}
					return rawStatus;
				}, "secc", "port0", "ci", "charge", "cp", "status");

		this.parent._setStatus(status);
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

		JsonElement currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (int i = 0; i < jsonPaths.length; i++) {
			String currentPathMember = jsonPaths[i];
			// System.out.println(currentPathMember);
			try {
				if (i != jsonPaths.length - 1) {
					// Not last path element
					currentJsonElement = JsonUtils.getAsJsonObject(currentJsonElement, currentPathMember);
				} else {
					//
					OpenemsType openemsType = divergentTypeInRawJson == null ? channelId.doc().getType()
							: divergentTypeInRawJson;

					// Last path element
					Object value = this.getJsonElementValue(currentJsonElement, openemsType, jsonPaths[i]);

					// Apply value converter
					value = converter.apply(value);

					// Return the converted value
					return value;
				}
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
