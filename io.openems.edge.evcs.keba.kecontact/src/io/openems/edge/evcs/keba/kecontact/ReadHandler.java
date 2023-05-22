package io.openems.edge.evcs.keba.kecontact;

import java.math.BigInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;

/**
 * Handles replies to Report Queries sent by {@link ReadWorker}.
 */
public class ReadHandler implements Consumer<String> {

	private final Logger log = LoggerFactory.getLogger(ReadHandler.class);
	private final KebaKeContactImpl parent;

	private boolean receiveReport1 = false;
	private boolean receiveReport2 = false;
	private boolean receiveReport3 = false;

	public ReadHandler(KebaKeContactImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(String message) {

		if (message.startsWith("TCH-OK")) {
			this.log.debug("KEBA confirmed reception of command: TCH-OK");
			this.parent.triggerQuery();

		} else if (message.startsWith("TCH-ERR")) {
			this.log.warn("KEBA reported command error: TCH-ERR");
			this.parent.triggerQuery();

		} else {
			JsonElement jsonMessageElement;
			try {
				jsonMessageElement = JsonUtils.parse(message);
			} catch (OpenemsNamedException e) {
				this.log.error("Error while parsing KEBA message: " + e.getMessage());
				return;
			}
			this.parent.logInfoInDebugmode(this.log, message);

			var jsonMessage = jsonMessageElement.getAsJsonObject();
			// JsonUtils.prettyPrint(jMessage);
			var idOpt = JsonUtils.getAsOptionalString(jsonMessage, "ID");
			if (idOpt.isPresent()) {
				// message with ID
				var id = idOpt.get();
				if (id.equals("1")) {
					/*
					 * Reply to report 1
					 */
					this.receiveReport1 = true;
					this.setString(KebaKeContact.ChannelId.SERIAL, jsonMessage, "Serial");
					this.setString(KebaKeContact.ChannelId.FIRMWARE, jsonMessage, "Firmware");
					this.setInt(KebaKeContact.ChannelId.COM_MODULE, jsonMessage, "COM-module");

					// Dip-Switches
					var dipSwitch1 = JsonUtils.getAsOptionalString(jsonMessage, "DIP-Sw1");
					var dipSwitch2 = JsonUtils.getAsOptionalString(jsonMessage, "DIP-Sw2");

					if (dipSwitch1.isPresent() && dipSwitch2.isPresent()) {
						this.checkDipSwitchSettings(dipSwitch1.get(), dipSwitch2.get());
					}

					// Product information
					var product = JsonUtils.getAsOptionalString(jsonMessage, "Product");
					if (product.isPresent()) {
						this.parent.channel(KebaKeContact.ChannelId.PRODUCT).setNextValue(product.get());
						this.checkProductInformation(product.get());
					}

				} else if (id.equals("2")) {
					/*
					 * Reply to report 2
					 */
					this.receiveReport2 = true;
					this.setInt(KebaKeContact.ChannelId.STATUS_KEBA, jsonMessage, "State");

					// Value "setenergy" not used, because it is reset by the currtime 0 1 command

					// Set Evcs status
					Channel<Status> stateChannel = this.parent.channel(KebaKeContact.ChannelId.STATUS_KEBA);
					Channel<Plug> plugChannel = this.parent.channel(KebaKeContact.ChannelId.PLUG);

					Plug plug = plugChannel.value().asEnum();
					Status status = stateChannel.value().asEnum();
					if (plug.equals(Plug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED)) {

						// Charging is rejected (by the Software) if the plug is connected but the EVCS
						// still not ready for charging.
						if (status.equals(Status.NOT_READY_FOR_CHARGING)) {
							status = Status.CHARGING_REJECTED;
						}

						// Charging is Finished if 'Plug' is connected, State was charging or already
						// finished and the EVCS is still ready for charging.
						var evcsStatus = this.parent.getStatus();
						switch (evcsStatus) {
						case CHARGING_REJECTED:
						case ENERGY_LIMIT_REACHED:
						case ERROR:
						case NOT_READY_FOR_CHARGING:
						case STARTING:
						case UNDEFINED:
							break;
						case READY_FOR_CHARGING:
						case CHARGING:
						case CHARGING_FINISHED:
							if (status.equals(Status.READY_FOR_CHARGING)
									&& this.parent.getSetChargePowerLimit().orElse(0) > 0) {
								status = Status.CHARGING_FINISHED;
							}
							break;
						}

						/*
						 * Check if the maximum energy limit is reached, informs the user and sets the
						 * status
						 */
						int limit = this.parent.getSetEnergyLimit().orElse(0);
						int energy = this.parent.getEnergySession().orElse(0);
						if (energy >= limit && limit != 0) {
							try {
								this.parent.setDisplayText(limit + "Wh erreicht");
								status = Status.ENERGY_LIMIT_REACHED;
							} catch (OpenemsNamedException e) {
								e.printStackTrace();
							}
						}
					} else {
						// Plug not fully connected
						status = Status.NOT_READY_FOR_CHARGING;
					}

					this.parent._setStatus(status);
					var errorState = status == Status.ERROR == true;
					this.parent.channel(KebaKeContact.ChannelId.CHARGINGSTATION_STATE_ERROR).setNextValue(errorState);

					this.setInt(KebaKeContact.ChannelId.ERROR_1, jsonMessage, "Error1");
					this.setInt(KebaKeContact.ChannelId.ERROR_2, jsonMessage, "Error2");
					this.setInt(KebaKeContact.ChannelId.PLUG, jsonMessage, "Plug");
					this.setBoolean(KebaKeContact.ChannelId.ENABLE_SYS, jsonMessage, "Enable sys");
					this.setBoolean(KebaKeContact.ChannelId.ENABLE_USER, jsonMessage, "Enable user");
					this.setInt(KebaKeContact.ChannelId.MAX_CURR_PERCENT, jsonMessage, "Max curr %");
					this.setInt(KebaKeContact.ChannelId.CURR_FAILSAFE, jsonMessage, "Curr FS");
					this.setInt(KebaKeContact.ChannelId.TIMEOUT_FAILSAFE, jsonMessage, "Tmo FS");
					this.setInt(KebaKeContact.ChannelId.CURR_TIMER, jsonMessage, "Curr timer");
					this.setInt(KebaKeContact.ChannelId.TIMEOUT_CT, jsonMessage, "Tmo CT");
					this.setBoolean(KebaKeContact.ChannelId.OUTPUT, jsonMessage, "Output");
					this.setBoolean(KebaKeContact.ChannelId.INPUT, jsonMessage, "Input");
					this.setInt(KebaKeContact.ChannelId.MAX_CURR, jsonMessage, "Curr HW");
					this.setInt(KebaKeContact.ChannelId.CURR_USER, jsonMessage, "Curr user");

				} else if (id.equals("3")) {
					/*
					 * Reply to report 3
					 */
					this.receiveReport3 = true;
					this.setInt(KebaKeContact.ChannelId.VOLTAGE_L1, jsonMessage, "U1");
					this.setInt(KebaKeContact.ChannelId.VOLTAGE_L2, jsonMessage, "U2");
					this.setInt(KebaKeContact.ChannelId.VOLTAGE_L3, jsonMessage, "U3");
					this.setInt(Evcs.ChannelId.CURRENT_L1, jsonMessage, "I1");
					this.setInt(Evcs.ChannelId.CURRENT_L2, jsonMessage, "I2");
					this.setInt(Evcs.ChannelId.CURRENT_L3, jsonMessage, "I3");
					this.setInt(KebaKeContact.ChannelId.ACTUAL_POWER, jsonMessage, "P");
					this.setInt(KebaKeContact.ChannelId.COS_PHI, jsonMessage, "PF");

					long totalEnergy = Math
							.round(JsonUtils.getAsOptionalLong(jsonMessage, "E total").orElse(0L) * 0.1F);
					this.parent.channel(KebaKeContact.ChannelId.ENERGY_TOTAL).setNextValue(totalEnergy);
					this.parent._setActiveConsumptionEnergy(totalEnergy);

					// Set the count of the Phases that are currently used
					Channel<Integer> currentL1 = this.parent.channel(Evcs.ChannelId.CURRENT_L1);
					Channel<Integer> currentL2 = this.parent.channel(Evcs.ChannelId.CURRENT_L2);
					Channel<Integer> currentL3 = this.parent.channel(Evcs.ChannelId.CURRENT_L3);
					var currentSum = currentL1.getNextValue().orElse(0) + currentL2.getNextValue().orElse(0)
							+ currentL3.getNextValue().orElse(0);
					this.parent._setCurrent(currentSum);

					if (currentSum > 300) {

						this.parent._setStatus(Status.CHARGING);
						var phases = 0;

						if (currentL1.getNextValue().orElse(0) >= 100) {
							phases += 1;
						}
						if (currentL2.getNextValue().orElse(0) >= 100) {
							phases += 1;
						}
						if (currentL3.getNextValue().orElse(0) >= 100) {
							phases += 1;
						}
						this.parent._setPhases(phases);

						this.parent.logInfoInDebugmode(this.log, "Used phases: " + phases);
					}

					/*
					 * Set FIXED_MAXIMUM_HARDWARE_POWER of Evcs - this is setting internally the
					 * dynamically calculated MAXIMUM_HARDWARE_POWER including the current used
					 * phases.
					 */
					Channel<Integer> maxDipSwitchLimitChannel = this.parent
							.channel(KebaKeContact.ChannelId.DIP_SWITCH_MAX_HW);
					int maxDipSwitchPowerLimit = Math.round(
							maxDipSwitchLimitChannel.value().orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) / 1000f)
							* Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();

					// Minimum of hardware setting and component configuration will be set.
					int maximumHardwareLimit = Math.min(maxDipSwitchPowerLimit,
							this.parent.getConfiguredMaximumHardwarePower());

					this.parent._setFixedMaximumHardwarePower(maximumHardwareLimit);

					/*
					 * Set FIXED_MINIMUM_HARDWARE_POWER of Evcs - this is setting internally the
					 * dynamically calculated MINIMUM_HARDWARE_POWER including the current used
					 * phases.
					 */
					this.parent._setFixedMinimumHardwarePower(this.parent.getConfiguredMinimumHardwarePower());

					/*
					 * Set CHARGE_POWER of Evcs
					 */
					var powerMw = JsonUtils.getAsOptionalInt(jsonMessage, "P"); // in [mW]
					Integer power = null;
					if (powerMw.isPresent()) {
						power = powerMw.get() / 1000; // convert to [W]
					}
					this.parent.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power);

					/*
					 * Set ENERGY_SESSION of Evcs
					 */
					this.parent.channel(Evcs.ChannelId.ENERGY_SESSION)
							.setNextValue(JsonUtils.getAsOptionalInt(jsonMessage, "E pres").orElse(0) * 0.1);
				}

			} else {
				/*
				 * message without ID -> UDP broadcast
				 */
				if (jsonMessage.has("State")) {
					this.setInt(KebaKeContact.ChannelId.STATUS_KEBA, jsonMessage, "State");
				}
				if (jsonMessage.has("Plug")) {
					this.setInt(KebaKeContact.ChannelId.PLUG, jsonMessage, "Plug");
				}
				if (jsonMessage.has("Input")) {
					this.setBoolean(KebaKeContact.ChannelId.INPUT, jsonMessage, "Input");
				}
				if (jsonMessage.has("Enable sys")) {
					this.setBoolean(KebaKeContact.ChannelId.ENABLE_SYS, jsonMessage, "Enable sys");
				}
				if (jsonMessage.has("E pres")) {
					this.parent.channel(Evcs.ChannelId.ENERGY_SESSION)
							.setNextValue(JsonUtils.getAsOptionalInt(jsonMessage, "E pres").orElse(0) * 0.1);
				}
			}
		}
	}

	/**
	 * Sets the associated channels depending on the dip-switches.
	 *
	 * @param dipSwitch1 Block one with eight dip-switches
	 * @param dipSwitch2 Block two with eight dip-switches
	 */
	private void checkDipSwitchSettings(String dipSwitch1, String dipSwitch2) {
		dipSwitch1 = hexStringToBinaryString(dipSwitch1);
		dipSwitch2 = hexStringToBinaryString(dipSwitch2);

		this.parent.channel(KebaKeContact.ChannelId.DIP_SWITCH_1).setNextValue(dipSwitch1);
		this.parent.channel(KebaKeContact.ChannelId.DIP_SWITCH_2).setNextValue(dipSwitch2);

		var setState = false;
		var hasStaticIp = false;

		// Set Channel for the communication
		setState = dipSwitch1.charAt(2) == '1' == false;
		this.setnextStateChannelValue(KebaKeContact.ChannelId.DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM, setState);

		// Is IP static or dynamic
		var staticIpSum = Integer.parseInt(dipSwitch2.substring(0, 4));
		hasStaticIp = staticIpSum > 0 == true;

		if (hasStaticIp) {
			// Set Channel for "static IP dip-switch not set"
			setState = dipSwitch2.charAt(5) == '1' == false;
			this.setnextStateChannelValue(KebaKeContact.ChannelId.DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP, setState);

		} else {
			// Set Channel for "static IP dip-switch wrongly set"
			setState = dipSwitch2.charAt(5) == '1' == true;
			this.setnextStateChannelValue(KebaKeContact.ChannelId.DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP, setState);
		}

		// Set Channel for "Master-Slave communication set"
		setState = dipSwitch2.charAt(4) == '1' == true;
		this.setnextStateChannelValue(KebaKeContact.ChannelId.DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM, setState);

		// Set Channel for "installation mode set"
		setState = dipSwitch2.charAt(7) == '1' == true;
		this.setnextStateChannelValue(KebaKeContact.ChannelId.DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION, setState);

		// Set Channel for the configured maximum limit in mA
		Integer hwLimit = null;
		var hwLimitDips = dipSwitch1.substring(5);

		switch (hwLimitDips) {
		case "000":
			hwLimit = 10_000;
			break;
		case "100":
			hwLimit = 13_000;
			break;
		case "010":
			hwLimit = 16_000;
			break;
		case "110":
			hwLimit = 20_000;
			break;
		case "001":
			hwLimit = 25_000;
			break;
		case "101":
			hwLimit = 32_000;
			break;
		}
		this.parent.channel(KebaKeContact.ChannelId.DIP_SWITCH_MAX_HW).setNextValue(hwLimit);
	}

	/**
	 * Sets the associated channels depending on the product information.
	 *
	 * @param product Detailed product information as string
	 */
	private void checkProductInformation(String product) {
		var blocks = product.split("-");

		// e- and b-series cannot be controlled
		var series = blocks[2].charAt(6);
		var oldSeries = series == '0' || series == '1' == true;
		this.setnextStateChannelValue(KebaKeContact.ChannelId.PRODUCT_SERIES_IS_NOT_COMPATIBLE, oldSeries);

		// Energy cannot be measured if there is no meter installed
		var meter = blocks[3].charAt(0);
		var noMeter = meter == '0' == true;
		this.setnextStateChannelValue(KebaKeContact.ChannelId.NO_ENERGY_METER_INSTALLED, noMeter);
	}

	/**
	 * Set the next value of a KebaChannelId state channel.
	 *
	 * @param channel Channel that needs to be set
	 * @param bool    Value that will be set
	 */
	private void setnextStateChannelValue(KebaKeContact.ChannelId channel, boolean bool) {
		this.parent.channel(channel).setNextValue(bool);
	}

	protected static String hexStringToBinaryString(String dipSwitches) {
		dipSwitches = dipSwitches.replace("0x", "");
		var binaryString = new BigInteger(dipSwitches, 16).toString(2);
		var length = binaryString.length();
		for (var i = 0; i < 8 - length; i++) {
			binaryString = "0" + binaryString;
		}
		return binaryString;
	}

	private void set(ChannelId channelId, Object value) {
		this.parent.channel(channelId).setNextValue(value);
	}

	private void setString(KebaKeContact.ChannelId channelId, JsonObject jMessage, String name) {
		this.set(channelId, JsonUtils.getAsOptionalString(jMessage, name).orElse(null));
	}

	private void setInt(ChannelId channelId, JsonObject jMessage, String name) {
		this.set(channelId, JsonUtils.getAsOptionalInt(jMessage, name).orElse(null));
	}

	private void setBoolean(KebaKeContact.ChannelId channelId, JsonObject jMessage, String name) {
		var enableSysOpt = JsonUtils.getAsOptionalInt(jMessage, name);
		if (enableSysOpt.isPresent()) {
			this.set(channelId, enableSysOpt.get() == 1);
		} else {
			this.set(channelId, null);
		}
	}

	/**
	 * returns true or false, if the requested report answered or not and set that
	 * value to false.
	 *
	 * @param report requested Report
	 * @return boolean
	 */
	public boolean hasResultandReset(Report report) {

		var result = false;
		switch (report) {
		case REPORT1:
			result = this.receiveReport1;
			this.receiveReport1 = false;
			break;
		case REPORT2:
			result = this.receiveReport2;
			this.receiveReport2 = false;
			break;
		case REPORT3:
			result = this.receiveReport3;
			this.receiveReport3 = false;
			break;
		}
		return result;
	}
}
