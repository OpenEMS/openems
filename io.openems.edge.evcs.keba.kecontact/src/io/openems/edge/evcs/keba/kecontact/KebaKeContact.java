package io.openems.edge.evcs.keba.kecontact;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.keba.kecontact.core.KebaKeContactCore;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Keba.KeContact", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class KebaKeContact extends AbstractOpenemsComponent
		implements Evcs, OpenemsComponent, EventHandler, ModbusSlave {

	public final static int UDP_PORT = 7090;

	private final Logger log = LoggerFactory.getLogger(KebaKeContact.class);
	private final ReadWorker readWorker = new ReadWorker(this);
	private final ReadHandler readHandler = new ReadHandler(this);
	private final WriteHandler writeHandler = new WriteHandler(this);
	private Boolean lastConnectionLostState = false;

	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	private KebaKeContactCore kebaKeContactCore = null;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * Report 1
		 */
		PRODUCT(new Doc().type(OpenemsType.STRING).text("Model name (variant)")), //
		SERIAL(new Doc().type(OpenemsType.STRING).text("Serial number")), //
		FIRMWARE(new Doc().type(OpenemsType.STRING).text("Firmware version")), //
		COM_MODULE(new Doc().type(OpenemsType.STRING).text("Communication module is installed; KeContact P30 only")),
		/*
		 * Report 2
		 */
		STATUS(new Doc().type(OpenemsType.INTEGER).text("Current state of the charging station")
				.options(Status.values())),
		ERROR_1(new Doc().type(OpenemsType.INTEGER)
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		ERROR_2(new Doc().type(OpenemsType.INTEGER)
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		PLUG(new Doc().type(OpenemsType.INTEGER).options(Plug.values())),
		ENABLE_SYS(new Doc().type(OpenemsType.BOOLEAN)
				.text("Enable state for charging (contains Enable input, RFID, UDP,..)")), //
		ENABLE_USER(new Doc().type(OpenemsType.BOOLEAN).text("Enable condition via UDP")), //
		MAX_CURR(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)
				.text("Current preset value via Control pilot")), //
		MAX_CURR_PERCENT(new Doc().type(OpenemsType.INTEGER)
				.text("Current preset value via Control pilot in 0,1% of the PWM value")), //
		CURR_USER(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)
				.text("Current preset value of the user via UDP; Default = 63000mA")), //
		CURR_FAILSAFE(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)
				.text("Current preset value for the Failsafe function")), //
		TIMEOUT_FAILSAFE(new Doc().type(OpenemsType.INTEGER).unit(Unit.SECONDS)
				.text("Communication timeout before triggering the Failsafe function")), //
		CURR_TIMER(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)
				.text("Shows the current preset value of currtime")), //
		TIMEOUT_CT(new Doc().type(OpenemsType.INTEGER).unit(Unit.SECONDS)
				.text("Shows the remaining time until the current value is accepted")), //
		ENERGY_LIMIT(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).text("Shows the set energy limit")), //
		// TODO: 0.1 Wh
		OUTPUT(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).text("State of the output X2")), //
		INPUT(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).text(
				"State of the potential free Enable input X1. When using the input, please pay attention to the information in the installation manual.")), //
		/*
		 * Report 3
		 */
		VOLTAGE_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT).text("Voltage on L1")), //
		VOLTAGE_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT).text("Voltage on L2")), //
		VOLTAGE_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT).text("Voltage on L3")), //
		CURRENT_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current on L1")), //
		CURRENT_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current on L2")), //
		CURRENT_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current on L3")), //
		ACTUAL_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIWATT).text("Total real power")), //
		COS_PHI(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT).text("Power factor")), //
		// TODO: 0.1 %
		ENERGY_SESSION(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).text(
				"Power consumption of the current loading session. Reset with new loading session (Status = NOT_READY_FOR_CHARGING)")), //
		// TODO: 0.1 Wh
		ENERGY_TOTAL(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).text(
				"Total power consumption (persistent) without current loading session. Is summed up after each completed charging session")), //

		PHASES(new Doc().type(OpenemsType.INTEGER).text("Count of ladders, the car is louding with")), //

		/*
		 * Write Channels
		 */
		SET_ENABLED(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)
				.text("Disabled is indicated with a blue flashing LED. "
						+ "ATTENTION: Some electric vehicles (EVs) do not yet meet the standard requirements "
						+ "and disabling can lead to an error in the charging station.")), //

		/*
		 * Fail State Channels
		 */
		ChargingStation_COMMUNICATION_FAILED(new Doc().level(Level.FAULT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public KebaKeContact() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	private InetAddress ip = null;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.enabled());

		this.ip = Inet4Address.getByName(config.ip());

		/*
		 * subscribe on replies to report queries
		 */
		this.kebaKeContactCore.onReceive((ip, message) -> {
			if (ip.equals(this.ip)) { // same IP -> handle message
				this.readHandler.accept(message);
			}
		});

		// start queryWorker
		this.readWorker.activate(this.id() + "query");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.readWorker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			// Clear channels if the connection to the Charging Station has been lost
			Channel<Boolean> connectionLostChannel = this.channel(ChannelId.ChargingStation_COMMUNICATION_FAILED);
			Boolean connectionLost = connectionLostChannel.value().orElse(lastConnectionLostState);
			if (connectionLost != lastConnectionLostState) {
				if (connectionLost) {
					resetChannelValues();
				}
				lastConnectionLostState = connectionLost;
			}

			// handle writes
			this.writeHandler.run();
			break;
		}
	}

	/**
	 * Send UDP message to KEBA KeContact. Returns true if sent successfully
	 *
	 * @param s
	 * @return
	 */
	protected boolean send(String s) {
		byte[] raw = s.getBytes();
		DatagramPacket packet = new DatagramPacket(raw, raw.length, ip, KebaKeContact.UDP_PORT);
		DatagramSocket dSocket = null;
		try {
			dSocket = new DatagramSocket();
			this.logInfo(this.log, "Sending message to KEBA KeContact [" + s + "]");
			dSocket.send(packet);
			return true;
		} catch (SocketException e) {
			this.logError(this.log, "Unable to open UDP socket for sending [" + s + "] to [" + ip.getHostAddress()
					+ "]: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log,
					"Unable to send [" + s + "] UDP message to [" + ip.getHostAddress() + "]: " + e.getMessage());
		} finally {
			if (dSocket != null) {
				dSocket.close();
			}
		}
		return false;
	}

	/**
	 * Triggers an immediate execution of query reports
	 */
	protected void triggerQuery() {
		this.readWorker.triggerNextRun();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.channel(KebaKeContact.ChannelId.CURR_USER).value().asString();
	}

	public ReadWorker getReadWorker() {
		return readWorker;
	}

	public ReadHandler getReadHandler() {
		return readHandler;
	}

	private void resetChannelValues() {
		for (ChannelId c : ChannelId.values()) {
			if (c != ChannelId.ChargingStation_COMMUNICATION_FAILED) {
				Channel<?> channel = this.channel(c);
				channel.setNextValue(null);
			}
		}

	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		
		return new ModbusSlaveTable(
				OpenemsComponent.getModbusSlaveNatureTable(), 
				Evcs.getModbusSlaveNatureTable(),
				this.getModbusSlaveNatureTable()
				
		);
	}
	
	private ModbusSlaveNatureTable getModbusSlaveNatureTable() {
		
		return ModbusSlaveNatureTable.of(KebaKeContact.class, 300) //
		
				
		.channel(0, ChannelId.PRODUCT, ModbusType.STRING16)
		.channel(16, ChannelId.SERIAL, ModbusType.STRING16)
		.channel(32, ChannelId.FIRMWARE, ModbusType.STRING16)
		.channel(48, ChannelId.COM_MODULE, ModbusType.STRING16)
		.channel(64, ChannelId.STATUS, ModbusType.UINT16)
		.channel(65, ChannelId.ERROR_1, ModbusType.UINT16)
		.channel(66, ChannelId.ERROR_2, ModbusType.UINT16)
		.channel(67, ChannelId.PLUG, ModbusType.UINT16)
		.channel(68, ChannelId.ENABLE_SYS, ModbusType.UINT16)
		.channel(69, ChannelId.ENABLE_USER, ModbusType.UINT16)
		.channel(70, ChannelId.MAX_CURR, ModbusType.UINT16)
		.channel(71, ChannelId.MAX_CURR_PERCENT, ModbusType.UINT16)
		.channel(72, ChannelId.CURR_USER, ModbusType.UINT16)
		.channel(73, ChannelId.CURR_FAILSAFE, ModbusType.UINT16)
		.channel(74, ChannelId.TIMEOUT_FAILSAFE, ModbusType.UINT16)
		.channel(75, ChannelId.CURR_TIMER, ModbusType.UINT16)
		.channel(76, ChannelId.TIMEOUT_CT, ModbusType.UINT16)
		.channel(77, ChannelId.ENERGY_LIMIT, ModbusType.UINT16)
		.channel(78, ChannelId.OUTPUT, ModbusType.UINT16)
		.channel(79, ChannelId.INPUT, ModbusType.UINT16)
		 
		//Report 3
		.channel(80, ChannelId.VOLTAGE_L1, ModbusType.UINT16)
		.channel(81, ChannelId.VOLTAGE_L2, ModbusType.UINT16)
		.channel(82, ChannelId.VOLTAGE_L3, ModbusType.UINT16)
		.channel(83, ChannelId.CURRENT_L1, ModbusType.UINT16)
		.channel(84, ChannelId.CURRENT_L2, ModbusType.UINT16)
		.channel(85, ChannelId.CURRENT_L3, ModbusType.UINT16)
		.channel(86, ChannelId.ACTUAL_POWER, ModbusType.UINT16)
		.channel(87, ChannelId.COS_PHI, ModbusType.UINT16)
		.channel(88, ChannelId.ENERGY_SESSION, ModbusType.UINT16)
		.channel(89, ChannelId.ENERGY_TOTAL, ModbusType.UINT16)
		.channel(90, ChannelId.PHASES, ModbusType.UINT16)
		.channel(91, ChannelId.SET_ENABLED, ModbusType.UINT16)
		.channel(92, ChannelId.ChargingStation_COMMUNICATION_FAILED, ModbusType.UINT16)
		.build();
	}
}
