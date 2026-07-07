package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCore.UDP_PORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.evcs.keba.udp.EvcsKebaUdpImpl;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;
import io.openems.edge.evse.chargepoint.keba.udp.EvseKebaUdpImpl;

/**
 * Common Channels and methods for {@link EvseKebaUdpImpl} and
 * {@link EvcsKebaUdpImpl}.
 */
public interface KebaUdp extends Keba {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		COMMUNICATION_FAILED(Doc.of(Level.FAULT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Communication to wallbox Failed "//
						+ "| Keine Verbindung zur Ladestation "//
						+ "| Bitte überprüfen Sie die Kommunikationsverbindung zu der Ladestation")), //

		// REPORT 1
		PRODUCT(Doc.of(OpenemsType.STRING)//
				.text("Model name (variant)")), //
		SERIAL(Doc.of(OpenemsType.STRING)), //
		COM_MODULE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Communication module is installed")),
		BACKEND(Doc.of(OpenemsType.BOOLEAN)//
				.text("Backend communication is present.")),
		FIRMWARE(new StringDoc()//
				.text("Firmware version")), //
		ENABLE_USER(Doc.of(OpenemsType.BOOLEAN)//
				.text("Device is enabled")), //
		MAX_CURR_PERCENT(Doc.of(OpenemsType.INTEGER)//
				.text("Current preset value via Control pilot in 0,1% of the PWM value")), //
		CURR_USER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Current preset value of the user via UDP; Default = 63000mA")), //
		CURR_FAILSAFE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Current preset value for the Failsafe function")), //
		TIMEOUT_FAILSAFE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Communication timeout before triggering the Failsafe function")), //
		CURR_TIMER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Shows the current preset value of currtime")), //
		TIMEOUT_CT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Shows the remaining time until the current value is accepted")), //
		SETENERGY(Doc.of(OpenemsType.INTEGER)//
				.text("Energy value in 0.1 Wh defined by the last setenergy command")), //
		OUTPUT(Doc.of(OpenemsType.INTEGER)//
				.text("Show the setting of the UDP command output")), //
		DIP_SWITCH_1(Doc.of(OpenemsType.STRING)//
				.text("The first eight dip switch settings as binary")),
		DIP_SWITCH_2(Doc.of(OpenemsType.STRING)//
				.text("The second eight dip switch settings as binary")),

		// REPORT 2
		ENABLE_SYS(Doc.of(OpenemsType.BOOLEAN)//
				.text("Charging state can be enabled")), //
		INPUT(Doc.of(OpenemsType.BOOLEAN)//
				.unit(Unit.ON_OFF)//
				.text("State of the input X1; For further information concerning the input X1, see the \"installation manual\"")), //
		ERROR_1(Doc.of(OpenemsType.INTEGER)//
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		ERROR_2(Doc.of(OpenemsType.INTEGER)//
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		AUTH_ON(Doc.of(OpenemsType.BOOLEAN)//
				.text("Authorization function is activated/deactivated")),
		AUTH_REQ(Doc.of(OpenemsType.BOOLEAN)//
				.text("Authorization via RFID card is required")),

		// REPORT 3
		CHARGINGSTATION_STATE_ERROR(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)),
		DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM(Doc.of(Level.FAULT)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("Dip-Switch 1.3. for communication must be on")),
		DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP(Doc.of(Level.FAULT)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("A static ip is configured. The Dip-Switch 2.6. must be on")), //
		DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP(Doc.of(OpenemsType.BOOLEAN)//
				.text("A dynamic ip is configured. Either the Dip-Switch 2.6. must be off or a static ip has to be configured")), //
		DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM(Doc.of(Level.INFO)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("Master-Slave communication is configured. If this is a normal KEBA that should be not controlled by a KEBA x-series, Dip-Switch 2.5. should be off")), //
		DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("Installation mode is configured. If the installation has finished, Dip-Switch 2.8. should be off")), //
		DIP_SWITCH_MAX_HW(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("The raw maximum limit configured by the dip switches")),

		// States
		PRODUCT_SERIES_IS_NOT_COMPATIBLE(Doc.of(Level.FAULT)//
				.text("Keba e- and b-series cannot be controlled because their software and hardware are not designed for it.")), //
		NO_ENERGY_METER_INSTALLED(Doc.of(Level.INFO)//
				.text("This keba cannot measure energy values, because there is no energy meter in it.")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Triggers an immediate execution of query reports.
	 */
	public void triggerQuery();

	/**
	 * Send UDP message to KEBA KeContact. Returns true if sent successfully.
	 * 
	 * @param parent       the parent {@link KebaUdp} component
	 * @param ip           the target {@link InetAddress}
	 * @param logVerbosity the configured {@link LogVerbosity}
	 * @param log          the parent {@link Logger}
	 * @param command      the command
	 * @return true if sent successfully
	 */
	public static boolean send(KebaUdp parent, InetAddress ip, LogVerbosity logVerbosity, Logger log, String command) {
		var raw = command.getBytes();
		var packet = new DatagramPacket(raw, raw.length, ip, UDP_PORT);
		try (var datagrammSocket = new DatagramSocket()) {
			datagrammSocket.send(packet);
			switch (logVerbosity) {
			case DEBUG_LOG -> FunctionUtils.doNothing();
			case WRITES, UDP_REPORTS -> OpenemsComponent.logInfo(parent, log, "Sent [" + command + "] successfully");
			}
			return true;

		} catch (SocketException e) {
			OpenemsComponent.logError(parent, log,
					"Unable to open UDP socket for sending [" + command + "] to [" + ip.getHostAddress() + "]: " //
							+ e.getMessage());

		} catch (IOException e) {
			OpenemsComponent.logError(parent, log,
					"Unable to send [" + command + "] UDP message to [" + ip.getHostAddress() + "]: " //
							+ e.getMessage());
		}
		return false;
	}

	/**
	 * Resets all channel values except the {@link ChannelId#COMMUNICATION_FAILED}
	 * channel.
	 * 
	 * @param parent the parent {@link KebaUdp} component
	 */
	public static void resetChannelValues(KebaUdp parent) {
		parent.channels().stream() //
				.filter(c -> c.channelId() != KebaUdp.ChannelId.COMMUNICATION_FAILED) //
				.filter(c -> !(c instanceof StateChannel)) //
				.forEach(c -> c.setNextValue(null));
	}

	/**
	 * Pre-Processes the text for "display" command.
	 * 
	 * @param text the text
	 * @return the command
	 */
	public static String preprocessDisplayTest(String text) {
		if (text == null) {
			text = "";
		}
		if (text.length() > 23) {
			text = text.substring(0, 23);
		}
		text = text.replace(" ", "$"); // $ == blank

		return "display 0 0 0 0 " + text;
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(KebaUdp.class, accessMode, 300) //
				.channel(0, KebaUdp.ChannelId.PRODUCT, ModbusType.STRING16)
				.channel(16, KebaUdp.ChannelId.SERIAL, ModbusType.STRING16)
				.channel(32, KebaUdp.ChannelId.FIRMWARE, ModbusType.STRING16)
				.channel(48, KebaUdp.ChannelId.COM_MODULE, ModbusType.STRING16)
				.channel(64, Keba.ChannelId.CHARGING_STATE, ModbusType.UINT16)
				.channel(65, KebaUdp.ChannelId.ERROR_1, ModbusType.UINT16)
				.channel(66, KebaUdp.ChannelId.ERROR_2, ModbusType.UINT16)
				.channel(67, Keba.ChannelId.CABLE_STATE, ModbusType.UINT16)
				.channel(68, KebaUdp.ChannelId.ENABLE_SYS, ModbusType.UINT16)
				.channel(69, KebaUdp.ChannelId.ENABLE_USER, ModbusType.UINT16)
				.channel(70, KebaUdp.ChannelId.MAX_CURR_PERCENT, ModbusType.UINT16)
				.channel(71, KebaUdp.ChannelId.CURR_USER, ModbusType.UINT16)
				.channel(72, KebaUdp.ChannelId.CURR_FAILSAFE, ModbusType.UINT16)
				.channel(73, KebaUdp.ChannelId.TIMEOUT_FAILSAFE, ModbusType.UINT16)
				.channel(74, KebaUdp.ChannelId.CURR_TIMER, ModbusType.UINT16)
				.channel(75, KebaUdp.ChannelId.TIMEOUT_CT, ModbusType.UINT16) //
				.uint16Reserved(76) //
				.channel(77, KebaUdp.ChannelId.OUTPUT, ModbusType.UINT16)
				.channel(78, KebaUdp.ChannelId.INPUT, ModbusType.UINT16) //
				.build();
	}
}
