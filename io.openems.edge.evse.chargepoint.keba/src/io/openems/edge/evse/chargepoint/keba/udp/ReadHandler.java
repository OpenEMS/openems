package io.openems.edge.evse.chargepoint.keba.udp;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.getAsOptionalLong;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static java.lang.Math.round;

import java.math.BigInteger;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;
import io.openems.edge.evse.chargepoint.keba.udp.core.Report;

/**
 * Handles replies to Report Queries sent by {@link ReadWorker}.
 */
public class ReadHandler implements BiConsumer<String, LogVerbosity> {

	private final Logger log = LoggerFactory.getLogger(ReadHandler.class);
	private final EvseChargePointKebaUdpImpl parent;

	private boolean receiveReport1 = false;
	private boolean receiveReport2 = false;
	private boolean receiveReport3 = false;

	public ReadHandler(EvseChargePointKebaUdpImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(String message, LogVerbosity logVerbosity) {
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

		// Parse JsonObject
		final JsonObject j;
		try {
			j = parseToJsonObject(message);
		} catch (OpenemsNamedException e) {
			this.log.error("Error while parsing KEBA message: " + e.getMessage());
			return;
		}

		// Log Report (if requested by config)
		switch (logVerbosity) {
		case DEBUG_LOG, WRITES -> doNothing();
		case UDP_REPORTS -> OpenemsComponent.logInfo(keba, this.log, (prettyToString(j)));
		}

		switch (getAsOptionalString(j, "ID").orElse("")) {
		/*
		 * report 1
		 */
		case "1" -> {
			this.receiveReport1 = true;
			this.setString(EvseChargePointKebaUdp.ChannelId.SERIAL, j, "Serial");
			this.setString(EvseChargePointKeba.ChannelId.FIRMWARE, j, "Firmware");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.COM_MODULE, j, "COM-module");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.BACKEND, j, "Backend");

			// Dip-Switches
			var dipSwitch1 = getAsOptionalString(j, "DIP-Sw1");
			var dipSwitch2 = getAsOptionalString(j, "DIP-Sw2");

			if (dipSwitch1.isPresent() && dipSwitch2.isPresent()) {
				this.checkDipSwitchSettings(dipSwitch1.get(), dipSwitch2.get());
			}

			// Product information
			var product = getAsOptionalString(j, "Product");
			setValue(keba, EvseChargePointKebaUdp.ChannelId.PRODUCT, product.orElse(null));
			if (product.isPresent()) {
				this.checkProductInformation(product.get());
			}
		}

		/*
		 * report 2
		 */
		case "2" -> {
			this.receiveReport2 = true;

			// Parse Status and Plug immediately
			this.setInt(EvseChargePointKeba.ChannelId.CHARGING_STATE, j, "State");
			final ChargingState r2State = keba.channel(EvseChargePointKeba.ChannelId.CHARGING_STATE).getNextValue()
					.asEnum();
			this.setPlug(j);

			keba.getChargingStateChannel().setNextValue(r2State);

			this.setInt(EvseChargePointKebaUdp.ChannelId.ERROR_1, j, "Error1");
			this.setInt(EvseChargePointKebaUdp.ChannelId.ERROR_2, j, "Error2");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.AUTH_ON, j, "AuthON");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.AUTH_REQ, j, "Authreq");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.ENABLE_SYS, j, "Enable sys");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.ENABLE_USER, j, "Enable user");
			this.setInt(EvseChargePointKebaUdp.ChannelId.MAX_CURR, j, "Max curr");
			this.setInt(EvseChargePointKebaUdp.ChannelId.MAX_CURR_PERCENT, j, "Max curr %");
			this.setInt(EvseChargePointKebaUdp.ChannelId.CURR_HW, j, "Curr HW");
			this.setInt(EvseChargePointKebaUdp.ChannelId.CURR_USER, j, "Curr user");
			this.setInt(EvseChargePointKebaUdp.ChannelId.CURR_FAILSAFE, j, "Curr FS");
			this.setInt(EvseChargePointKebaUdp.ChannelId.TIMEOUT_FAILSAFE, j, "Tmo FS");
			this.setInt(EvseChargePointKebaUdp.ChannelId.CURR_TIMER, j, "Curr timer");
			this.setInt(EvseChargePointKebaUdp.ChannelId.TIMEOUT_CT, j, "Tmo CT");
			this.setInt(EvseChargePointKebaUdp.ChannelId.SETENERGY, j, "Setenergy");
			this.setInt(EvseChargePointKebaUdp.ChannelId.OUTPUT, j, "Output");
			this.setBoolean(EvseChargePointKebaUdp.ChannelId.INPUT, j, "Input");
			this.setInt(EvseChargePointKeba.ChannelId.PHASE_SWITCH_SOURCE, j, "X2 phaseSwitch source");
			this.setInt(EvseChargePointKeba.ChannelId.PHASE_SWITCH_STATE, j, "X2 phaseSwitch");
		}

		/*
		 * report 3
		 */
		case "3" -> {
			/*
			 * Reply to report 3
			 */
			this.receiveReport3 = true;

			setValue(keba, EvseChargePointKeba.ChannelId.ENERGY_SESSION, getAsOptionalInt(j, "E pres") //
					.map(e -> round(e * 0.1F)) //
					.orElse(null));

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
			var appp = ActivePowerPerPhase.from(activePower, //
					voltageL1, currentL1, voltageL2, currentL2, voltageL3, currentL3);

			// Round power per phase and apply rotated phases

			PhaseRotation.setPhaseRotatedVoltageChannels(keba, voltageL1, voltageL2, voltageL3);
			PhaseRotation.setPhaseRotatedCurrentChannels(keba, currentL1, currentL2, currentL3);
			PhaseRotation.setPhaseRotatedActivePowerChannels(keba, appp.activePowerL1, appp.activePowerL2,
					appp.activePowerL3);

			// Energy
			keba._setActiveProductionEnergy(//
					getAsOptionalLong(j, "E total") //
							.map(e -> round(e * 0.1F)) //
							.orElse(null));

			// TODO use COS_PHI to calculate ReactivePower
			this.setInt(EvseChargePointKeba.ChannelId.POWER_FACTOR, j, "PF");
		}

		/*
		 * message without ID -> UDP broadcast
		 */
		default -> {
			if (j.has("State")) {
				this.setInt(EvseChargePointKeba.ChannelId.CHARGING_STATE, j, "State");
			}
			if (j.has("Plug")) {
				this.setPlug(j);
			}
			if (j.has("Input")) {
				this.setBoolean(EvseChargePointKebaUdp.ChannelId.INPUT, j, "Input");
			}
			if (j.has("Enable sys")) {
				this.setBoolean(EvseChargePointKebaUdp.ChannelId.ENABLE_SYS, j, "Enable sys");
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

		this.parent.channel(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_1).setNextValue(dipSwitch1);
		this.parent.channel(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_2).setNextValue(dipSwitch2);

		var setState = false;
		var hasStaticIp = false;

		// Set Channel for the communication
		setState = dipSwitch1.charAt(2) == '1' == false;
		this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM, setState);

		// Is IP static or dynamic
		var staticIpSum = Integer.parseInt(dipSwitch2.substring(0, 4));
		hasStaticIp = staticIpSum > 0 == true;

		if (hasStaticIp) {
			// Set Channel for "static IP dip-switch not set"
			setState = dipSwitch2.charAt(5) == '1' == false;
			this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP,
					setState);

		} else {
			// Set Channel for "static IP dip-switch wrongly set"
			setState = dipSwitch2.charAt(5) == '1' == true;
			this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP,
					setState);
		}

		// Set Channel for "Master-Slave communication set"
		setState = dipSwitch2.charAt(4) == '1' == true;
		this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM,
				setState);

		// Set Channel for "installation mode set"
		setState = dipSwitch2.charAt(7) == '1' == true;
		this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION,
				setState);

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
		this.parent.channel(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_MAX_HW).setNextValue(hwLimit);
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
		this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.PRODUCT_SERIES_IS_NOT_COMPATIBLE, oldSeries);

		// Energy cannot be measured if there is no meter installed
		var meter = blocks[3].charAt(0);
		var noMeter = meter == '0' == true;
		this.setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId.NO_ENERGY_METER_INSTALLED, noMeter);
	}

	/**
	 * Set the next value of a KebaChannelId state channel.
	 *
	 * @param channel Channel that needs to be set
	 * @param bool    Value that will be set
	 */
	private void setnextStateChannelValue(EvseChargePointKebaUdp.ChannelId channel, boolean bool) {
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

	private CableState setPlug(JsonObject jMessage) {
		final var channelId = EvseChargePointKeba.ChannelId.CABLE_STATE;
		this.setInt(channelId, jMessage, "Plug");
		final CableState cableState = this.parent.channel(channelId).getNextValue().asEnum();
		return cableState;
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
