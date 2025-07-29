package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.getAsOptionalBoolean;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.getAsOptionalLong;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.component.OpenemsComponent.logInfo;
import static io.openems.edge.common.component.OpenemsComponent.logWarn;
import static java.lang.Math.round;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;
import io.openems.edge.evse.chargepoint.keba.udp.ReadWorker;
import io.openems.edge.evse.chargepoint.keba.udp.core.Report;

/**
 * Handles replies to Report Queries sent by {@link ReadWorker}.
 */
public abstract class AbstractUdpReadHandler<T extends KebaUdp> implements BiConsumer<String, LogVerbosity> {

	protected final T parent;

	private final Logger log = LoggerFactory.getLogger(AbstractUdpReadHandler.class);
	private final EnumMap<Report, Boolean> receiveReport = new EnumMap<Report, Boolean>(Report.class);
	private final EnergySessionHandler energySessionHandler = new EnergySessionHandler();

	protected AbstractUdpReadHandler(T parent) {
		this.parent = parent;
	}

	@Override
	public void accept(String message, LogVerbosity logVerbosity) {
		final var keba = this.parent;

		if (message.startsWith("TCH-OK")) {
			switch (logVerbosity) {
			case DEBUG_LOG, WRITES -> doNothing();
			case UDP_REPORTS -> logInfo(keba, this.log, "KEBA confirmed reception of command: TCH-OK");
			}
			keba.triggerQuery();
			return;
		}

		if (message.startsWith("TCH-ERR")) {
			logWarn(keba, this.log, "KEBA reported command error: TCH-ERR");
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
		case UDP_REPORTS -> logInfo(keba, this.log, (prettyToString(j)));
		}

		final var report = switch (getAsOptionalString(j, "ID").orElse("")) {
		case "1" -> Report.REPORT1;
		case "2" -> Report.REPORT2;
		case "3" -> Report.REPORT3;
		default -> null;
		};

		if (report != null) {
			this.receiveReport.put(report, true);
			switch (report) {
			case REPORT1 -> this._handleReport1(j);
			case REPORT2 -> this._handleReport2(j);
			case REPORT3 -> this._handleReport3(j);
			}
		} else {
			// Message without ID -> UDP broadcast
			getAsOptionalInt(j, "State") //
					.ifPresent(v -> setValue(keba, Keba.ChannelId.CHARGING_STATE, v));
			getAsOptionalInt(j, "Plug") //
					.ifPresent(v -> this.setCableState(v));
			getAsOptionalBoolean(j, "Input") //
					.ifPresent(v -> setValue(keba, KebaUdp.ChannelId.INPUT, v));
			getAsOptionalBoolean(j, "Enable sys") //
					.ifPresent(v -> setValue(keba, KebaUdp.ChannelId.ENABLE_SYS, v));
		}
	}

	private void _handleReport1(JsonObject j) {
		final var keba = this.parent;

		this.setString(KebaUdp.ChannelId.SERIAL, j, "Serial");
		this.setString(KebaUdp.ChannelId.FIRMWARE, j, "Firmware");
		this.setBoolean(KebaUdp.ChannelId.COM_MODULE, j, "COM-module");
		this.setBoolean(KebaUdp.ChannelId.BACKEND, j, "Backend");

		// Dip-Switches
		var dipSwitch1 = getAsOptionalString(j, "DIP-Sw1");
		var dipSwitch2 = getAsOptionalString(j, "DIP-Sw2");
		if (dipSwitch1.isPresent() && dipSwitch2.isPresent()) {
			this.checkDipSwitchSettings(dipSwitch1.get(), dipSwitch2.get());
		}

		// Product information
		var product = getAsOptionalString(j, "Product");
		setValue(keba, KebaUdp.ChannelId.PRODUCT, product.orElse(null));
		if (product.isPresent()) {
			this.checkProductInformation(product.get());
		}

		this.handleReport1();
	}

	protected void handleReport1() {
	}

	private void _handleReport2(JsonObject j) {
		final var chargingState = this.setChargingState(getAsOptionalInt(j, "State").orElse(null));
		final var cableState = this.setCableState(getAsOptionalInt(j, "Plug").orElse(null));
		this.setInt(Keba.ChannelId.CABLE_STATE, j, "Plug");
		this.setInt(KebaUdp.ChannelId.ERROR_1, j, "Error1");
		this.setInt(KebaUdp.ChannelId.ERROR_2, j, "Error2");
		this.setBoolean(KebaUdp.ChannelId.AUTH_ON, j, "AuthON");
		this.setBoolean(KebaUdp.ChannelId.AUTH_REQ, j, "Authreq");
		this.setBoolean(KebaUdp.ChannelId.ENABLE_SYS, j, "Enable sys");
		this.setBoolean(KebaUdp.ChannelId.ENABLE_USER, j, "Enable user");
		this.setInt(KebaUdp.ChannelId.MAX_CURR, j, "Max curr");
		this.setInt(KebaUdp.ChannelId.MAX_CURR_PERCENT, j, "Max curr %");
		this.setInt(KebaUdp.ChannelId.CURR_HW, j, "Curr HW");
		this.setInt(KebaUdp.ChannelId.CURR_USER, j, "Curr user");
		this.setInt(KebaUdp.ChannelId.CURR_FAILSAFE, j, "Curr FS");
		this.setInt(KebaUdp.ChannelId.TIMEOUT_FAILSAFE, j, "Tmo FS");
		this.setInt(KebaUdp.ChannelId.CURR_TIMER, j, "Curr timer");
		this.setInt(KebaUdp.ChannelId.TIMEOUT_CT, j, "Tmo CT");
		this.setInt(KebaUdp.ChannelId.SETENERGY, j, "Setenergy");
		this.setInt(KebaUdp.ChannelId.OUTPUT, j, "Output");
		this.setBoolean(KebaUdp.ChannelId.INPUT, j, "Input");
		this.setInt(Keba.ChannelId.PHASE_SWITCH_SOURCE, j, "X2 phaseSwitch source");
		this.setInt(Keba.ChannelId.PHASE_SWITCH_STATE, j, "X2 phaseSwitch");

		this.handleReport2(chargingState, cableState);
	}

	protected void handleReport2(ChargingState chargingState, CableState cableState) {
	}

	private void _handleReport3(JsonObject j) {
		final var keba = this.parent;

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
		final var appp = ActivePowerPerPhase.from(activePower, //
				voltageL1, currentL1, voltageL2, currentL2, voltageL3, currentL3);

		// Energy
		keba._setActiveProductionEnergy(//
				getAsOptionalLong(j, "E total") //
						.map(e -> round(e * 0.1F)) //
						.orElse(null));
		setValue(keba, EvseKeba.ChannelId.ENERGY_SESSION, this.energySessionHandler.updateFromReport3(//
				getAsOptionalInt(j, "E pres") //
						.map(e -> round(e * 0.1F)) //
						.orElse(null)));

		this.setInt(Keba.ChannelId.POWER_FACTOR, j, "PF");

		this.handleReport3(appp);
	}

	protected void handleReport3(ActivePowerPerPhase appp) {
	}

	protected ChargingState setChargingState(Integer value) {
		final var keba = this.parent;
		final var chargingState = value == null //
				? ChargingState.UNDEFINED //
				: OptionsEnum.getOptionOrUndefined(ChargingState.class, value);

		setValue(keba, Keba.ChannelId.CHARGING_STATE, chargingState);
		return chargingState;
	}

	protected CableState setCableState(Integer value) {
		final var cableState = value == null //
				? CableState.UNDEFINED //
				: OptionsEnum.getOptionOrUndefined(CableState.class, value);
		return this.setCableState(cableState);
	}

	protected CableState setCableState(CableState cableState) {
		final var keba = this.parent;
		setValue(keba, Keba.ChannelId.CABLE_STATE, cableState);
		this.energySessionHandler.updateCableState(cableState);
		return cableState;
	}

	protected record ActivePowerPerPhase(//
			Integer activePowerSum, //
			Integer voltageL1, int currentL1, //
			Integer voltageL2, int currentL2, //
			Integer voltageL3, int currentL3, //
			Integer activePowerL1, Integer activePowerL2, Integer activePowerL3) {

		protected static ActivePowerPerPhase from(//
				Integer activePowerSum, //
				Integer voltageL1, int currentL1, //
				Integer voltageL2, int currentL2, //
				Integer voltageL3, int currentL3) {
			if (activePowerSum == null) {
				return new ActivePowerPerPhase(//
						activePowerSum, //
						voltageL1, currentL1, //
						voltageL2, currentL2, //
						voltageL3, currentL3, //
						null, null, null);
			}

			var pL1 = voltageL1 != null ? voltageL1 / 1000 * currentL1 : 0;
			var pL2 = voltageL2 != null ? voltageL2 / 1000 * currentL2 : 0;
			var pL3 = voltageL3 != null ? voltageL3 / 1000 * currentL3 : 0;
			var pSum = pL1 + pL2 + pL3;
			var factor = activePowerSum / (float) pSum; // distribute power to match sum

			return new ActivePowerPerPhase(//
					activePowerSum, //
					voltageL1, currentL1, //
					voltageL2, currentL2, //
					voltageL3, currentL3, //
					round(pL1 * factor), round(pL2 * factor), round(pL3 * factor));
		}
	}

	/**
	 * Sets the associated channels depending on the dip-switches.
	 *
	 * @param dipSwitch1 Block one with eight dip-switches
	 * @param dipSwitch2 Block two with eight dip-switches
	 */
	private void checkDipSwitchSettings(String dipSwitch1, String dipSwitch2) {
		final var keba = this.parent;

		dipSwitch1 = hexStringToBinaryString(dipSwitch1);
		dipSwitch2 = hexStringToBinaryString(dipSwitch2);

		this.parent.channel(KebaUdp.ChannelId.DIP_SWITCH_1).setNextValue(dipSwitch1);
		this.parent.channel(KebaUdp.ChannelId.DIP_SWITCH_2).setNextValue(dipSwitch2);

		var setState = false;
		var hasStaticIp = false;

		// Set Channel for the communication
		setState = dipSwitch1.charAt(2) == '1' == false;
		setValue(keba, KebaUdp.ChannelId.DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM, setState);

		// Is IP static or dynamic
		var staticIpSum = Integer.parseInt(dipSwitch2.substring(0, 4));
		hasStaticIp = staticIpSum > 0 == true;

		if (hasStaticIp) {
			// Set Channel for "static IP dip-switch not set"
			setState = dipSwitch2.charAt(5) == '1' == false;
			setValue(keba, KebaUdp.ChannelId.DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP, setState);

		} else {
			// Set Channel for "static IP dip-switch wrongly set"
			setState = dipSwitch2.charAt(5) == '1' == true;
			setValue(keba, KebaUdp.ChannelId.DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP, setState);
		}

		// Set Channel for "Master-Slave communication set"
		setState = dipSwitch2.charAt(4) == '1' == true;
		setValue(keba, KebaUdp.ChannelId.DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM, setState);

		// Set Channel for "installation mode set"
		setState = dipSwitch2.charAt(7) == '1' == true;
		setValue(keba, KebaUdp.ChannelId.DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION, setState);

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
		this.parent.channel(KebaUdp.ChannelId.DIP_SWITCH_MAX_HW).setNextValue(hwLimit);
	}

	/**
	 * Sets the associated channels depending on the product information.
	 *
	 * @param product Detailed product information as string
	 */
	private void checkProductInformation(String product) {
		final var keba = this.parent;
		var blocks = product.split("-");

		// e- and b-series cannot be controlled
		var series = blocks[2].charAt(6);
		setValue(keba, KebaUdp.ChannelId.PRODUCT_SERIES_IS_NOT_COMPATIBLE, series == '0' || series == '1');

		// Energy cannot be measured if there is no meter installed
		var meter = blocks[3].charAt(0);
		setValue(keba, KebaUdp.ChannelId.NO_ENERGY_METER_INSTALLED, meter == '0');
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

	private void setString(ChannelId channelId, JsonObject jMessage, String name) {
		setValue(this.parent, channelId, getAsOptionalString(jMessage, name).orElse(null));
	}

	private void setInt(ChannelId channelId, JsonObject jMessage, String name) {
		setValue(this.parent, channelId, getAsOptionalInt(jMessage, name).orElse(null));
	}

	private void setBoolean(ChannelId channelId, JsonObject jMessage, String name) {
		var enableSysOpt = getAsOptionalInt(jMessage, name);
		if (enableSysOpt.isPresent()) {
			setValue(this.parent, channelId, enableSysOpt.get() == 1);
		} else {
			setValue(this.parent, channelId, null);
		}
	}

	protected static class EnergySessionHandler {
		private CableState cableState = CableState.UNDEFINED;
		private Integer ePresOnUnplugged = null;

		protected synchronized void updateCableState(CableState cableState) {
			this.cableState = cableState;
		}

		public synchronized Integer updateFromReport3(Integer ePres) {
			switch (this.cableState) {
			case UNPLUGGED, // no cable
					PLUGGED_ON_WALLBOX, PLUGGED_ON_WALLBOX_AND_LOCKED // not plugged on EV
				-> this.ePresOnUnplugged = ePres > 0 ? ePres : null;
			case UNDEFINED, // unsure
					PLUGGED_EV_NOT_LOCKED, PLUGGED_AND_LOCKED // plugged on EV
				-> doNothing();
			}
			if (this.ePresOnUnplugged == null) {
				return ePres;
			}
			if (ePres == null) {
				return null;
			}
			if (this.ePresOnUnplugged <= ePres) {
				return null;
			}
			// reset
			this.ePresOnUnplugged = null;
			return ePres;
		}
	}

	/**
	 * Returns true or false, if the requested report answered or not and set that
	 * value to false.
	 *
	 * @param report requested Report
	 * @return boolean
	 */
	public boolean hasResultandReset(Report report) {
		return Optional.ofNullable(this.receiveReport.put(report, false)).orElse(false);
	}
}
