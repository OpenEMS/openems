package io.openems.edge.evcs.keba.kecontact;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles replys to Report Querys sent by {@link ReadWorker}.
 */
public class ReadHandler implements Consumer<String> {

	private final Logger log = LoggerFactory.getLogger(ReadHandler.class);
	private final KebaKeContact parent;
	private boolean receiveReport1 = false;
	private boolean receiveReport2 = false;
	private boolean receiveReport3 = false;

	public ReadHandler(KebaKeContact parent) {
		this.parent = parent;
	}

	@Override
	public void accept(String message) {

		if (message.startsWith("TCH-OK")) {
			log.debug("KEBA confirmed reception of command: TCH-OK");
			this.parent.triggerQuery();

		} else if (message.startsWith("TCH-ERR")) {
			log.warn("KEBA reported command error: TCH-ERR");
			this.parent.triggerQuery();

		} else {
			JsonElement jsonMessageElement;
			try {
				jsonMessageElement = JsonUtils.parse(message);
			} catch (OpenemsNamedException e) {
				log.error("Error while parsing KEBA message: " + e.getMessage());
				return;
			}
			JsonObject jsonMessage = jsonMessageElement.getAsJsonObject();
			// JsonUtils.prettyPrint(jMessage);
			Optional<String> idOpt = JsonUtils.getAsOptionalString(jsonMessage, "ID");
			if (idOpt.isPresent()) {
				// message with ID
				String id = idOpt.get();
				if (id.equals("1")) {
					/*
					 * Reply to report 1
					 */
					receiveReport1 = true;
					setString(KebaChannelId.PRODUCT, jsonMessage, "Product");
					setString(KebaChannelId.SERIAL, jsonMessage, "Serial");
					setString(KebaChannelId.FIRMWARE, jsonMessage, "Firmware");
					setInt(KebaChannelId.COM_MODULE, jsonMessage, "COM-module");

				} else if (id.equals("2")) {
					/*
					 * Reply to report 2
					 */
					receiveReport2 = true;
					setInt(KebaChannelId.STATUS_KEBA, jsonMessage, "State");

					// The setenergy value of KEBA is not used because it is reset by the currtime 0
					// 1 command

					// Set STATUS and Warning STATE Channel
					Channel<Status> stateChannel = this.parent.channel(KebaChannelId.STATUS_KEBA);
					Channel<Status> plugChannel = this.parent.channel(KebaChannelId.PLUG);

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
						Status evcsStatus = parent.status().value().asEnum();
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
									&& parent.setChargePowerLimit().value().orElse(0) > 0) {
								status = Status.CHARGING_FINISHED;
							}
						}
					}

					/*
					 * Check if the maximum energy limit is reached, informs the user and sets the
					 * status
					 */
					int limit = this.parent.setEnergyLimit().value().orElse(0);
					int energy = this.parent.getEnergySession().value().orElse(0);
					if (energy >= limit && limit != 0) {
						try {

							this.parent.setDisplayText().setNextWriteValue(limit + "Wh erreicht");
							status = Status.ENERGY_LIMIT_REACHED;
							this.parent.logInfo(log, "Status: " + status.getName());
						} catch (OpenemsNamedException e) {
							e.printStackTrace();
						}
					}

					this.parent.channel(Evcs.ChannelId.STATUS).setNextValue(status);

					if (status == Status.ERROR) {
						this.parent.channel(KebaChannelId.CHARGINGSTATION_STATE_ERROR).setNextValue(true);
					} else {
						this.parent.channel(KebaChannelId.CHARGINGSTATION_STATE_ERROR).setNextValue(false);
					}

					setInt(KebaChannelId.ERROR_1, jsonMessage, "Error1");
					setInt(KebaChannelId.ERROR_2, jsonMessage, "Error2");
					setInt(KebaChannelId.PLUG, jsonMessage, "Plug");
					setBoolean(KebaChannelId.ENABLE_SYS, jsonMessage, "Enable sys");
					setBoolean(KebaChannelId.ENABLE_USER, jsonMessage, "Enable user");
					setInt(KebaChannelId.MAX_CURR_PERCENT, jsonMessage, "Max curr %");
					setInt(KebaChannelId.CURR_FAILSAFE, jsonMessage, "Curr FS");
					setInt(KebaChannelId.TIMEOUT_FAILSAFE, jsonMessage, "Tmo FS");
					setInt(KebaChannelId.CURR_TIMER, jsonMessage, "Curr timer");
					setInt(KebaChannelId.TIMEOUT_CT, jsonMessage, "Tmo CT");
					setBoolean(KebaChannelId.OUTPUT, jsonMessage, "Output");
					setBoolean(KebaChannelId.INPUT, jsonMessage, "Input");
					setInt(KebaChannelId.CURR_USER, jsonMessage, "Curr user");

					Optional<Integer> currUserMa = JsonUtils.getAsOptionalInt(jsonMessage, "Curr user"); // in [mA]
					if (currUserMa.isPresent()) {
						int chargingTarget = (currUserMa.get() / 1000) * 230
								* this.parent.getPhases().value().orElse(3);
						this.parent.setChargePowerLimit().setNextValue(chargingTarget);
					}

					// Set the maximum Power valid by the Hardware
					// The default value will be 32 A, because an older Keba charging station sets
					// the value to 0 if the car is unplugged
					Optional<Integer> hwPowerMa = JsonUtils.getAsOptionalInt(jsonMessage, "Curr HW"); // in [mA]
					Integer hwPower = null;
					if (hwPowerMa.isPresent()) {
						if (hwPowerMa.get() == 0) {
							hwPower = 32000 * 230 / 1000; // [W]
						} else {
							hwPower = hwPowerMa.get() * 230 / 1000; // [W]
						}
					}

					this.parent.channel(KebaChannelId.MAX_CURR).setNextValue(hwPower);

				} else if (id.equals("3")) {
					/*
					 * Reply to report 3
					 */
					receiveReport3 = true;
					setInt(KebaChannelId.VOLTAGE_L1, jsonMessage, "U1");
					setInt(KebaChannelId.VOLTAGE_L2, jsonMessage, "U2");
					setInt(KebaChannelId.VOLTAGE_L3, jsonMessage, "U3");
					setInt(KebaChannelId.CURRENT_L1, jsonMessage, "I1");
					setInt(KebaChannelId.CURRENT_L2, jsonMessage, "I2");
					setInt(KebaChannelId.CURRENT_L3, jsonMessage, "I3");
					setInt(KebaChannelId.ACTUAL_POWER, jsonMessage, "P");
					setInt(KebaChannelId.COS_PHI, jsonMessage, "PF");
					setInt(KebaChannelId.ENERGY_TOTAL, jsonMessage, "E total");

					// Set the count of the Phases that are currently used
					Channel<Integer> currentL1 = parent.channel(KebaChannelId.CURRENT_L1);
					Channel<Integer> currentL2 = parent.channel(KebaChannelId.CURRENT_L2);
					Channel<Integer> currentL3 = parent.channel(KebaChannelId.CURRENT_L3);

					if (currentL1.value().orElse(0) > 10) {

						if (currentL3.value().orElse(0) > 100) {
							this.parent.logInfo(this.log, "KEBA is loading on three ladder");
							this.parent.getPhases().setNextValue(3);

						} else if (currentL2.value().orElse(0) > 100) {
							this.parent.logInfo(this.log, "KEBA is loading on two ladder");
							this.parent.getPhases().setNextValue(2);

						} else {
							this.parent.logInfo(this.log, "KEBA is loading on one ladder");
							this.parent.getPhases().setNextValue(1);
						}
						Channel<Integer> phases = this.parent.getPhases();
						this.parent.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER)
								.setNextValue(230 /* Spannung */ * 6 /* min Strom */ * phases.value().orElse(3));
						this.parent.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER)
								.setNextValue(230 /* Spannung */ * 32 /* max Strom */ * phases.value().orElse(3));
					} else {

						// set Min & Max Power to Default values that allows the User a power setting
						// between those values
						Channel<Integer> min = this.parent.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);
						Channel<Integer> max = this.parent.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER);
						if (min.value().get() == null || max.value().get() == null) {
							Channel<Integer> maxHW = this.parent.channel(KebaChannelId.MAX_CURR);
							this.parent.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER)
									.setNextValue(230 /* Spannung */ * 6 /* min Strom */ * 3);
							this.parent.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER)
									.setNextValue(230 /* Spannung */ * maxHW.value().orElse(32) /* max Strom */ * 3);
						}
					}

					// Set CHARGE_POWER
					Optional<Integer> powerMw = JsonUtils.getAsOptionalInt(jsonMessage, "P"); // in [mW]
					Integer power = null;
					if (powerMw.isPresent()) {
						power = powerMw.get() / 1000; // convert to [W]
					}
					this.parent.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power);

					// Set ENERGY_SESSION
					this.parent.channel(Evcs.ChannelId.ENERGY_SESSION)
							.setNextValue((JsonUtils.getAsOptionalInt(jsonMessage, "E pres").orElse(0)) * 0.1);
				}

			} else {
				/*
				 * message without ID -> UDP broadcast
				 */
				if (jsonMessage.has("State")) {
					setInt(KebaChannelId.STATUS_KEBA, jsonMessage, "State");
				}
				if (jsonMessage.has("Plug")) {
					setInt(KebaChannelId.PLUG, jsonMessage, "Plug");
				}
				if (jsonMessage.has("Input")) {
					setBoolean(KebaChannelId.INPUT, jsonMessage, "Input");
				}
				if (jsonMessage.has("Enable sys")) {
					setBoolean(KebaChannelId.ENABLE_SYS, jsonMessage, "Enable sys");
				}
				if (jsonMessage.has("E pres")) {
					this.parent.channel(Evcs.ChannelId.ENERGY_SESSION)
							.setNextValue((JsonUtils.getAsOptionalInt(jsonMessage, "E pres").orElse(0)) * 0.1);
				}
			}
		}
	}

	private void set(KebaChannelId channelId, Object value) {
		this.parent.channel(channelId).setNextValue(value);
	}

	private void setString(KebaChannelId channelId, JsonObject jMessage, String name) {
		set(channelId, JsonUtils.getAsOptionalString(jMessage, name).orElse(null));
	}

	private void setInt(KebaChannelId channelId, JsonObject jMessage, String name) {
		set(channelId, JsonUtils.getAsOptionalInt(jMessage, name).orElse(null));
	}

	private void setBoolean(KebaChannelId channelId, JsonObject jMessage, String name) {
		Optional<Integer> enableSysOpt = JsonUtils.getAsOptionalInt(jMessage, name);
		if (enableSysOpt.isPresent()) {
			set(channelId, enableSysOpt.get() == 1);
		} else {
			set(channelId, null);
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

		boolean result = false;
		switch (report) {
		case REPORT1:
			result = receiveReport1;
			receiveReport1 = false;
			break;
		case REPORT2:
			result = receiveReport2;
			receiveReport2 = false;
			break;
		case REPORT3:
			result = receiveReport3;
			receiveReport3 = false;
			break;
		}
		return result;
	}
}
