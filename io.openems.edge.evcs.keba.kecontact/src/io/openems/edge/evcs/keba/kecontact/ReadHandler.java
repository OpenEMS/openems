package io.openems.edge.evcs.keba.kecontact;

import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.getAsOptionalLong;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCount;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static io.openems.edge.evcs.api.Status.CHARGING;
import static java.lang.Math.round;

import java.math.BigInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.PhaseRotation.RotatedPhases;
import io.openems.edge.evcs.api.Status;

/**
 * Handles replies to Report Queries sent by {@link ReadWorker}.
 */
public class ReadHandler implements Consumer<String> {

	private final Logger log = LoggerFactory.getLogger(ReadHandler.class);
	private final EvcsKebaKeContactImpl parent;

	private boolean receiveReport1 = false;
	private boolean receiveReport2 = false;
	private boolean receiveReport3 = false;

	public ReadHandler(EvcsKebaKeContactImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(String message) {
		final var keba = this.parent;

		if (message.startsWith("TCH-OK")) {
			this.log.debug("KEBA confirmed reception of command: TCH-OK");
			keba.triggerQuery();
			return;
		}

		if (message.startsWith("TCH-ERR")) {
			this.log.warn("KEBA reported command error: TCH-ERR");
			keba.triggerQuery();
			return;
		}

		keba.logInfoInDebugmode(this.log, message);

		// Parse JsonObject
		final JsonObject j;
		try {
			j = JsonUtils.parseToJsonObject(message);
		} catch (OpenemsNamedException e) {
			this.log.error("Error while parsing KEBA message: " + e.getMessage());
			return;
		}

		switch (getAsOptionalString(j, "ID").orElse("")) {
		/*
		 * report 1
		 */
		case "1" -> {
			this.receiveReport1 = true;
			this.setString(EvcsKebaKeContact.ChannelId.SERIAL, j, "Serial");
			this.setString(EvcsKebaKeContact.ChannelId.FIRMWARE, j, "Firmware");
			this.setInt(EvcsKebaKeContact.ChannelId.COM_MODULE, j, "COM-module");

			// Dip-Switches
			var dipSwitch1 = getAsOptionalString(j, "DIP-Sw1");
			var dipSwitch2 = getAsOptionalString(j, "DIP-Sw2");

			if (dipSwitch1.isPresent() && dipSwitch2.isPresent()) {
				this.checkDipSwitchSettings(dipSwitch1.get(), dipSwitch2.get());
			}

			// Product information
			var product = getAsOptionalString(j, "Product");
			keba.channel(EvcsKebaKeContact.ChannelId.PRODUCT).setNextValue(product.orElse(null));
			if (product.isPresent()) {
				this.checkProductInformation(product.get());
			}
		}

		/*
		 * report 2
		 */
		case "2" -> {
			this.receiveReport2 = true;
			this.setInt(EvcsKebaKeContact.ChannelId.STATUS_KEBA, j, "State");

			// Value "setenergy" not used, because it is reset by the currtime 0 1 command

			// Set Evcs status
			Channel<Status> stateChannel = keba.channel(EvcsKebaKeContact.ChannelId.STATUS_KEBA);
			Channel<Plug> plugChannel = keba.channel(EvcsKebaKeContact.ChannelId.PLUG);

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
				switch (keba.getStatus()) {
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
					if (status.equals(Status.READY_FOR_CHARGING) && keba.getSetChargePowerLimit().orElse(0) > 0) {
						status = Status.CHARGING_FINISHED;
					}
					break;
				}

				/*
				 * Check if the maximum energy limit is reached, informs the user and sets the
				 * status
				 */
				int limit = keba.getSetEnergyLimit().orElse(0);
				int energy = keba.getEnergySession().orElse(0);
				if (energy >= limit && limit != 0) {
					status = Status.ENERGY_LIMIT_REACHED;
				}
			} else {
				// Plug not fully connected
				status = Status.NOT_READY_FOR_CHARGING;
			}

			keba._setStatus(status);
			var errorState = status == Status.ERROR == true;
			keba.channel(EvcsKebaKeContact.ChannelId.CHARGINGSTATION_STATE_ERROR).setNextValue(errorState);

			this.setInt(EvcsKebaKeContact.ChannelId.ERROR_1, j, "Error1");
			this.setInt(EvcsKebaKeContact.ChannelId.ERROR_2, j, "Error2");
			this.setInt(EvcsKebaKeContact.ChannelId.PLUG, j, "Plug");
			this.setBoolean(EvcsKebaKeContact.ChannelId.ENABLE_SYS, j, "Enable sys");
			this.setBoolean(EvcsKebaKeContact.ChannelId.ENABLE_USER, j, "Enable user");
			this.setInt(EvcsKebaKeContact.ChannelId.MAX_CURR_PERCENT, j, "Max curr %");
			this.setInt(EvcsKebaKeContact.ChannelId.CURR_FAILSAFE, j, "Curr FS");
			this.setInt(EvcsKebaKeContact.ChannelId.TIMEOUT_FAILSAFE, j, "Tmo FS");
			this.setInt(EvcsKebaKeContact.ChannelId.CURR_TIMER, j, "Curr timer");
			this.setInt(EvcsKebaKeContact.ChannelId.TIMEOUT_CT, j, "Tmo CT");
			this.setBoolean(EvcsKebaKeContact.ChannelId.OUTPUT, j, "Output");
			this.setBoolean(EvcsKebaKeContact.ChannelId.INPUT, j, "Input");
			this.setInt(EvcsKebaKeContact.ChannelId.MAX_CURR, j, "Curr HW");
			this.setInt(EvcsKebaKeContact.ChannelId.CURR_USER, j, "Curr user");
		}

		/*
		 * report 3
		 */
		case "3" -> {
			/*
			 * Reply to report 3
			 */
			this.receiveReport3 = true;

			// Voltage
			final var voltageL1 = getAsOptionalInt(j, "U1").map(v -> v != 0 ? v * 1000 : null).orElse(null);
			final var voltageL2 = getAsOptionalInt(j, "U2").map(v -> v != 0 ? v * 1000 : null).orElse(null);
			final var voltageL3 = getAsOptionalInt(j, "U3").map(v -> v != 0 ? v * 1000 : null).orElse(null);

			// Current
			final var currentL1 = getAsOptionalInt(j, "I1").orElse(0).intValue();
			final var currentL2 = getAsOptionalInt(j, "I2").orElse(0).intValue();
			final var currentL3 = getAsOptionalInt(j, "I3").orElse(0).intValue();

			// Power
			final var activePower = getAsOptionalInt(j, "P") //
					.map(p -> p / 1000) // convert [mW] to [W]
					.orElse(null);
			keba._setActivePower(activePower);

			// Round power per phase and apply rotated phases
			var appp = ActivePowerPerPhase.from(activePower, //
					voltageL1, currentL1, voltageL2, currentL2, voltageL3, currentL3);
			var rp = RotatedPhases.from(keba.config.phaseRotation(), //
					voltageL1, currentL1, appp.activePowerL1, //
					voltageL2, currentL2, appp.activePowerL2, //
					voltageL3, currentL3, appp.activePowerL3);
			keba._setVoltageL1(rp.voltageL1());
			keba._setVoltageL2(rp.voltageL2());
			keba._setVoltageL3(rp.voltageL3());
			keba._setCurrentL1(rp.currentL1());
			keba._setCurrentL2(rp.currentL2());
			keba._setCurrentL3(rp.currentL3());
			keba._setActivePowerL1(rp.activePowerL1());
			keba._setActivePowerL2(rp.activePowerL2());
			keba._setActivePowerL3(rp.activePowerL3());

			// Energy
			keba._setActiveProductionEnergy(//
					getAsOptionalLong(j, "E total") //
							.map(e -> round(e * 0.1F)) //
							.orElse(null));
			keba._setEnergySession(//
					getAsOptionalInt(j, "E pres") //
							.map(e -> round(e * 0.1F)) //
							.orElse(null));

			// TODO use COS_PHI to calculate ReactivePower
			this.setInt(EvcsKebaKeContact.ChannelId.COS_PHI, j, "PF");

			final var phases = evaluatePhaseCount(appp.activePowerL1, appp.activePowerL2, appp.activePowerL3);
			keba._setPhases(phases);
			if (phases != null) {
				keba.logInfoInDebugmode(this.log, "Used phases: " + phases);
				keba._setStatus(CHARGING);
			}

			/*
			 * Set FIXED_MAXIMUM_HARDWARE_POWER of Evcs - this is setting internally the
			 * dynamically calculated MAXIMUM_HARDWARE_POWER including the current used
			 * phases.
			 */
			Channel<Integer> maxDipSwitchLimitChannel = keba.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_MAX_HW);
			int maxDipSwitchPowerLimit = round(maxDipSwitchLimitChannel.value() //
					.orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) / 1000f) * Evcs.DEFAULT_VOLTAGE
					* THREE_PHASE.getValue();

			// Minimum of hardware setting and component configuration will be set.
			int maximumHardwareLimit = Math.min(maxDipSwitchPowerLimit, keba.getConfiguredMaximumHardwarePower());

			keba._setFixedMaximumHardwarePower(maximumHardwareLimit);

			/*
			 * Set FIXED_MINIMUM_HARDWARE_POWER of Evcs - this is setting internally the
			 * dynamically calculated MINIMUM_HARDWARE_POWER including the current used
			 * phases.
			 */
			keba._setFixedMinimumHardwarePower(keba.getConfiguredMinimumHardwarePower());
		}

		/*
		 * message without ID -> UDP broadcast
		 */
		default -> {
			if (j.has("State")) {
				this.setInt(EvcsKebaKeContact.ChannelId.STATUS_KEBA, j, "State");
			}
			if (j.has("Plug")) {
				this.setInt(EvcsKebaKeContact.ChannelId.PLUG, j, "Plug");
			}
			if (j.has("Input")) {
				this.setBoolean(EvcsKebaKeContact.ChannelId.INPUT, j, "Input");
			}
			if (j.has("Enable sys")) {
				this.setBoolean(EvcsKebaKeContact.ChannelId.ENABLE_SYS, j, "Enable sys");
			}
			if (j.has("E pres")) {
				keba._setEnergySession(//
						getAsOptionalInt(j, "E pres") //
								.map(e -> round(e * 0.1F)) //
								.orElse(null));
			}
		}
		}
	}

	public record ActivePowerPerPhase(Integer activePowerL1, Integer activePowerL2, Integer activePowerL3) {
		protected static ActivePowerPerPhase from(Integer activePowerSum, Integer voltageL1, int currentL1,
				Integer voltageL2, int currentL2, Integer voltageL3, int currentL3) {
			if (activePowerSum == null) {
				return new ActivePowerPerPhase(null, null, null);
			}

			var pL1 = voltageL1 != null ? voltageL1 / 1000 * currentL1 : 0;
			var pL2 = voltageL2 != null ? voltageL2 / 1000 * currentL2 : 0;
			var pL3 = voltageL3 != null ? voltageL3 / 1000 * currentL3 : 0;
			var pSum = pL1 + pL2 + pL3;
			var factor = activePowerSum / (float) pSum; // distribute power to match sum

			return new ActivePowerPerPhase(round(pL1 * factor), round(pL2 * factor), round(pL3 * factor));
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

		this.parent.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_1).setNextValue(dipSwitch1);
		this.parent.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_2).setNextValue(dipSwitch2);

		var setState = false;
		var hasStaticIp = false;

		// Set Channel for the communication
		setState = dipSwitch1.charAt(2) == '1' == false;
		this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM, setState);

		// Is IP static or dynamic
		var staticIpSum = Integer.parseInt(dipSwitch2.substring(0, 4));
		hasStaticIp = staticIpSum > 0 == true;

		if (hasStaticIp) {
			// Set Channel for "static IP dip-switch not set"
			setState = dipSwitch2.charAt(5) == '1' == false;
			this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP,
					setState);

		} else {
			// Set Channel for "static IP dip-switch wrongly set"
			setState = dipSwitch2.charAt(5) == '1' == true;
			this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP,
					setState);
		}

		// Set Channel for "Master-Slave communication set"
		setState = dipSwitch2.charAt(4) == '1' == true;
		this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM,
				setState);

		// Set Channel for "installation mode set"
		setState = dipSwitch2.charAt(7) == '1' == true;
		this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION, setState);

		// Set Channel for the configured maximum limit in mA
		var hwLimit = switch (dipSwitch1.substring(5)) {
		case "000" -> 10_000;
		case "100" -> 13_000;
		case "010" -> 16_000;
		case "110" -> 20_000;
		case "001" -> 25_000;
		case "101" -> 32_000;
		default -> null;
		};
		this.parent.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_MAX_HW).setNextValue(hwLimit);
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
		this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.PRODUCT_SERIES_IS_NOT_COMPATIBLE, oldSeries);

		// Energy cannot be measured if there is no meter installed
		var meter = blocks[3].charAt(0);
		var noMeter = meter == '0' == true;
		this.setnextStateChannelValue(EvcsKebaKeContact.ChannelId.NO_ENERGY_METER_INSTALLED, noMeter);
	}

	/**
	 * Set the next value of a KebaChannelId state channel.
	 *
	 * @param channel Channel that needs to be set
	 * @param bool    Value that will be set
	 */
	private void setnextStateChannelValue(EvcsKebaKeContact.ChannelId channel, boolean bool) {
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

	private void setString(ChannelId channelId, JsonObject jMessage, String name) {
		this.set(channelId, getAsOptionalString(jMessage, name).orElse(null));
	}

	private void setInt(ChannelId channelId, JsonObject jMessage, String name) {
		this.set(channelId, getAsOptionalInt(jMessage, name).orElse(null));
	}

	private void setBoolean(ChannelId channelId, JsonObject jMessage, String name) {
		var enableSysOpt = getAsOptionalInt(jMessage, name);
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
