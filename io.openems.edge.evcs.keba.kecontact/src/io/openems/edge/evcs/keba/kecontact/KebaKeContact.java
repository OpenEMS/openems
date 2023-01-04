package io.openems.edge.evcs.keba.kecontact;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.keba.kecontact.core.KebaKeContactCore;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Keba.KeContact", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class KebaKeContact extends AbstractManagedEvcsComponent
		implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler, ModbusSlave {

	public static final int UDP_PORT = 7090;

	private final Logger log = LoggerFactory.getLogger(KebaKeContact.class);
	private final ReadWorker readWorker = new ReadWorker(this);
	private final ReadHandler readHandler = new ReadHandler(this);
	private Boolean lastConnectionLostState = false;

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	private KebaKeContactCore kebaKeContactCore = null;

	/**
	 * Constructor.
	 */
	public KebaKeContact() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				KebaChannelId.values() //
		);
	}

	private InetAddress ip = null;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.channel(KebaChannelId.ALIAS).setNextValue(config.alias());

		this.ip = InetAddress.getByName(config.ip().trim());

		this.config = config;
		this._setPowerPrecision(0.23);
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());

		/*
		 * subscribe on replies to report queries
		 */
		this.kebaKeContactCore.onReceive((ip, message) -> {
			if (ip.equals(this.ip)) { // same IP -> handle message
				this.readHandler.accept(message);
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(false);
			}
		});

		// start queryWorker
		this.readWorker.activate(this.id() + "query");

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.readWorker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			// Clear channels if the connection to the Charging Station has been lost
			Channel<Boolean> connectionLostChannel = this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED);
			var connectionLost = connectionLostChannel.value().orElse(this.lastConnectionLostState);
			if (connectionLost != this.lastConnectionLostState) {
				if (connectionLost) {
					this.resetChannelValues();
				}
				this.lastConnectionLostState = connectionLost;
			}
			break;
		}
	}

	/**
	 * Send UDP message to KEBA KeContact. Returns true if sent successfully
	 *
	 * @param s Message to send
	 * @return true if sent
	 */
	protected boolean send(String s) {
		var raw = s.getBytes();
		var packet = new DatagramPacket(raw, raw.length, this.ip, KebaKeContact.UDP_PORT);
		try (DatagramSocket datagrammSocket = new DatagramSocket()) {
			datagrammSocket.send(packet);
			return true;
		} catch (SocketException e) {
			this.logError(this.log, "Unable to open UDP socket for sending [" + s + "] to [" + this.ip.getHostAddress()
					+ "]: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log,
					"Unable to send [" + s + "] UDP message to [" + this.ip.getHostAddress() + "]: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Triggers an immediate execution of query reports.
	 */
	protected void triggerQuery() {
		this.readWorker.triggerNextRun();
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.channel(KebaChannelId.CURR_USER).value().asString() + "|" + this.getStatus().getName();
	}

	/**
	 * Logs are displayed if the debug mode is configured.
	 *
	 * @param log    the {@link Logger}
	 * @param string Text to display
	 */
	protected void logInfoInDebugmode(Logger log, String string) {
		if (this.config.debugMode()) {
			this.logInfo(log, string);
		}
	}

	public ReadWorker getReadWorker() {
		return this.readWorker;
	}

	public ReadHandler getReadHandler() {
		return this.readHandler;
	}

	/**
	 * Resets all channel values except the Communication_Failed channel.
	 */
	private void resetChannelValues() {
		for (KebaChannelId c : KebaChannelId.values()) {
			if (c != KebaChannelId.ALIAS) {
				Channel<?> channel = this.channel(c);
				channel.setNextValue(null);
			}
		}
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {

		var phases = this.getPhasesAsInt();
		var current = Math.round((power * 1000) / phases / 230f);

		/*
		 * Limits the charging value because KEBA knows only values between 6000 and
		 * 63000
		 */
		current = Math.min(current, 63_000);

		if (current < 6000) {
			current = 0;
		}
		return this.send("currtime " + current + " 1");
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		if (!this.config.useDisplay()) {
			return false;
		}
		if (text.length() > 23) {
			text = text.substring(0, 23);
		}
		text = text.replace(" ", "$"); // $ == blank

		return this.send("display 0 0 0 0 " + text);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT / 1000f) * Evcs.DEFAULT_VOLTAGE
				* Phases.THREE_PHASE.getValue();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {

		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Evcs.getModbusSlaveNatureTable(accessMode), //
				ManagedEvcs.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	private ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {

		return ModbusSlaveNatureTable.of(KebaKeContact.class, accessMode, 300) //

				.channel(0, KebaChannelId.PRODUCT, ModbusType.STRING16)
				.channel(16, KebaChannelId.SERIAL, ModbusType.STRING16)
				.channel(32, KebaChannelId.FIRMWARE, ModbusType.STRING16)
				.channel(48, KebaChannelId.COM_MODULE, ModbusType.STRING16)
				.channel(64, KebaChannelId.STATUS_KEBA, ModbusType.UINT16)
				.channel(65, KebaChannelId.ERROR_1, ModbusType.UINT16)
				.channel(66, KebaChannelId.ERROR_2, ModbusType.UINT16)
				.channel(67, KebaChannelId.PLUG, ModbusType.UINT16)
				.channel(68, KebaChannelId.ENABLE_SYS, ModbusType.UINT16)
				.channel(69, KebaChannelId.ENABLE_USER, ModbusType.UINT16)
				.channel(70, KebaChannelId.MAX_CURR_PERCENT, ModbusType.UINT16)
				.channel(71, KebaChannelId.CURR_USER, ModbusType.UINT16)
				.channel(72, KebaChannelId.CURR_FAILSAFE, ModbusType.UINT16)
				.channel(73, KebaChannelId.TIMEOUT_FAILSAFE, ModbusType.UINT16)
				.channel(74, KebaChannelId.CURR_TIMER, ModbusType.UINT16)
				.channel(75, KebaChannelId.TIMEOUT_CT, ModbusType.UINT16).uint16Reserved(76)
				.channel(77, KebaChannelId.OUTPUT, ModbusType.UINT16)
				.channel(78, KebaChannelId.INPUT, ModbusType.UINT16)

				// Report 3
				.channel(79, KebaChannelId.VOLTAGE_L1, ModbusType.UINT16)
				.channel(80, KebaChannelId.VOLTAGE_L2, ModbusType.UINT16)
				.channel(81, KebaChannelId.VOLTAGE_L3, ModbusType.UINT16)
				.channel(82, KebaChannelId.CURRENT_L1, ModbusType.UINT16)
				.channel(83, KebaChannelId.CURRENT_L2, ModbusType.UINT16)
				.channel(84, KebaChannelId.CURRENT_L3, ModbusType.UINT16)
				.channel(85, KebaChannelId.ACTUAL_POWER, ModbusType.UINT16)
				.channel(86, KebaChannelId.COS_PHI, ModbusType.UINT16).uint16Reserved(87)
				.channel(88, KebaChannelId.ENERGY_TOTAL, ModbusType.UINT16).build();
	}
}
