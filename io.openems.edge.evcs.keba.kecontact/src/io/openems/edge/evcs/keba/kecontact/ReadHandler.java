package io.openems.edge.evcs.keba.kecontact;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

import io.openems.edge.evcs.api.Evcs;

/**
 * Handles replys to Report Querys sent by {@link ReadWorker}
 *
 */
public class ReadHandler implements Consumer<String> {

	private final Logger log = LoggerFactory.getLogger(ReadHandler.class);
	private final KebaKeContact parent;

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
			JsonElement jMessageElement;
			try {
				jMessageElement = JsonUtils.parse(message);
			} catch (OpenemsNamedException e) {
				log.error("Error while parsing KEBA message: " + e.getMessage());
				return;
			}
			JsonObject jMessage = jMessageElement.getAsJsonObject();
			// JsonUtils.prettyPrint(jMessage);
			Optional<String> idOpt = JsonUtils.getAsOptionalString(jMessage, "ID");
			if (idOpt.isPresent()) {
				// message with ID
				String id = idOpt.get();
				if (id.equals("1")) {
					/*
					 * Reply to report 1
					 */
					setString(KebaKeContact.ChannelId.PRODUCT, jMessage, "Product");
					setString(KebaKeContact.ChannelId.SERIAL, jMessage, "Serial");
					setString(KebaKeContact.ChannelId.FIRMWARE, jMessage, "Firmware");
					setString(KebaKeContact.ChannelId.COM_MODULE, jMessage, "COM-module");

				} else if (id.equals("2")) {
					/*
					 * Reply to report 2
					 */
					setString(KebaKeContact.ChannelId.STATUS, jMessage, "State");
					setString(KebaKeContact.ChannelId.ERROR_1, jMessage, "Error1");
					setString(KebaKeContact.ChannelId.ERROR_2, jMessage, "Error2");
					setInt(KebaKeContact.ChannelId.PLUG, jMessage, "Plug");
					setBoolean(KebaKeContact.ChannelId.ENABLE_SYS, jMessage, "Enable sys");
					setBoolean(KebaKeContact.ChannelId.ENABLE_USER, jMessage, "Enable user");
					setInt(KebaKeContact.ChannelId.MAX_CURR, jMessage, "Max curr");
					setInt(KebaKeContact.ChannelId.MAX_CURR_PERCENT, jMessage, "Max curr %");
					setInt(KebaKeContact.ChannelId.CURR_HARDWARE, jMessage, "Curr HW");
					setInt(KebaKeContact.ChannelId.CURR_USER, jMessage, "Curr user");
					setInt(KebaKeContact.ChannelId.CURR_FAILSAFE, jMessage, "Curr FS");
					setInt(KebaKeContact.ChannelId.TIMEOUT_FAILSAFE, jMessage, "Tmo FS");
					setInt(KebaKeContact.ChannelId.CURR_TIMER, jMessage, "Curr timer");
					setInt(KebaKeContact.ChannelId.TIMEOUT_CT, jMessage, "Tmo CT");
					setInt(KebaKeContact.ChannelId.ENERGY_LIMIT, jMessage, "Setenergy");
					setBoolean(KebaKeContact.ChannelId.OUTPUT, jMessage, "Output");
					setBoolean(KebaKeContact.ChannelId.INPUT, jMessage, "Input");

				} else if (id.equals("3")) {
					/*
					 * Reply to report 3
					 */
					setInt(KebaKeContact.ChannelId.VOLTAGE_L1, jMessage, "U1");
					setInt(KebaKeContact.ChannelId.VOLTAGE_L2, jMessage, "U2");
					setInt(KebaKeContact.ChannelId.VOLTAGE_L3, jMessage, "U3");
					setInt(KebaKeContact.ChannelId.CURRENT_L1, jMessage, "I1");
					setInt(KebaKeContact.ChannelId.CURRENT_L2, jMessage, "I2");
					setInt(KebaKeContact.ChannelId.CURRENT_L3, jMessage, "I3");
					setInt(KebaKeContact.ChannelId.ACTUAL_POWER, jMessage, "P");
					setInt(KebaKeContact.ChannelId.COS_PHI, jMessage, "PF");
					setInt(KebaKeContact.ChannelId.ENERGY_SESSION, jMessage, "E pres");
					setInt(KebaKeContact.ChannelId.ENERGY_TOTAL, jMessage, "E total");

					// Set CHARGE_POWER
					Optional<Integer> power_mw = JsonUtils.getAsOptionalInt(jMessage, "P"); // in [mW]
					Integer power = null;
					if (power_mw.isPresent()) {
						power = power_mw.get() / 1000; // convert to [W]
					}
					this.parent.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power);
				}

			} else {
				/*
				 * message without ID -> UDP broadcast
				 */
				if (jMessage.has("State")) {
					setInt(KebaKeContact.ChannelId.STATUS, jMessage, "State");
				}
				if (jMessage.has("Plug")) {
					setInt(KebaKeContact.ChannelId.PLUG, jMessage, "Plug");
				}
				if (jMessage.has("Input")) {
					setBoolean(KebaKeContact.ChannelId.INPUT, jMessage, "Input");
				}
				if (jMessage.has("Enable sys")) {
					setBoolean(KebaKeContact.ChannelId.ENABLE_SYS, jMessage, "Enable sys");
				}
				if (jMessage.has("Max curr")) {
					setInt(KebaKeContact.ChannelId.MAX_CURR, jMessage, "Max curr");
				}
				if (jMessage.has("E pres")) {
					setInt(KebaKeContact.ChannelId.ENERGY_SESSION, jMessage, "E pres");
				}
			}
		}
	}

	private void set(KebaKeContact.ChannelId channelId, Object value) {
		this.parent.channel(channelId).setNextValue(value);
	}

	private void setString(KebaKeContact.ChannelId channelId, JsonObject jMessage, String name) {
		set(channelId, JsonUtils.getAsOptionalString(jMessage, name).orElse(null));
	}

	private void setInt(KebaKeContact.ChannelId channelId, JsonObject jMessage, String name) {
		set(channelId, JsonUtils.getAsOptionalInt(jMessage, name).orElse(null));
	}

	private void setBoolean(KebaKeContact.ChannelId channelId, JsonObject jMessage, String name) {
		Optional<Integer> enableSysOpt = JsonUtils.getAsOptionalInt(jMessage, name);
		if (enableSysOpt.isPresent()) {
			set(channelId, enableSysOpt.get() == 1);
		} else {
			set(channelId, null);
		}
	}
}
