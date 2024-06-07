package io.openems.edge.evcs.keba.kecontact;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.keba.kecontact.core.EvcsKebaKeContactCore;
import io.openems.edge.evcs.keba.kecontact.state.PhaseSwitchHandler;
import io.openems.edge.evcs.keba.kecontact.state.State;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Keba.KeContact", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsKebaKeContactImpl extends AbstractManagedEvcsComponent
		implements EvcsKebaKeContact, ManagedEvcs, Evcs, OpenemsComponent, EventHandler, ModbusSlave {

	public final Logger log = LoggerFactory.getLogger(EvcsKebaKeContactImpl.class);
	private final ReadWorker readWorker = new ReadWorker(this);
	private final ReadHandler readHandler = new ReadHandler(this);
	private PhaseSwitchHandler phaseSwitchHandler;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	private EvcsKebaKeContactCore kebaKeContactCore = null;

	protected Config config;

	private Boolean lastConnectionLostState = false;
	private InetAddress ip = null;

	public EvcsKebaKeContactImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsKebaKeContact.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.ip = InetAddress.getByName(config.ip().trim());

		this.config = config;
		this._setPowerPrecision(0.23);
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());

		this.phaseSwitchHandler = new PhaseSwitchHandler(this);

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
	public boolean send(String s) {
		if (s.startsWith("x2") && !this.isPhaseSwitchSourceSet()) {
			this.log.warn("Phase switch source not set to UDP control. Setting now...");
			if (!this.send("x2src 4")) {
				this.log.error("Failed to set phase switch source to UDP control.");
				return false;
			}
		}

		var raw = s.getBytes();
		var packet = new DatagramPacket(raw, raw.length, this.ip, EvcsKebaKeContactImpl.UDP_PORT);
		try (DatagramSocket datagrammSocket = new DatagramSocket()) {
			datagrammSocket.send(packet);
			return true;

		} catch (IOException e) {
			this.logError(this.log, "Failed to send UDP packet: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Triggers an immediate execution of query reports.
	 */
	protected void triggerQuery() {
		this.readWorker.triggerNextRun();
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.channel(EvcsKebaKeContact.ChannelId.CURR_USER).value().asString() + "|"
				+ this.getStatus().getName();
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
		for (var c : EvcsKebaKeContact.ChannelId.values()) {
			Channel<?> channel = this.channel(c);
			channel.setNextValue(null);
		}
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.getEvcsPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	/**
	 * Checks whether the phase switch configuration is active. This method queries
	 * the current configuration to determine if the phase switch has been enabled.
	 * 
	 * @return true if the phase switch is active, false otherwise.
	 */
	public boolean phaseSwitchActive() {
		return this.config.phaseSwitchActive();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		this.log.debug("Applying charge power limit: [Power: " + power + "W]");

		this.phaseSwitchHandler.applyPower(power); // Verwenden des PhaseSwitchHandlers

		return true;
	}

	public int getMinHwCurrent() {
		return this.config.minHwCurrent();
	}

	public PhaseSwitchHandler getPhaseSwitchHandler() {
		return this.phaseSwitchHandler;
	}

	public State getSwitchToOnePhaseState() {
		return this.phaseSwitchHandler.getSwitchToOnePhaseState();
	}

	public State getRunningOnePhaseState() {
		return this.phaseSwitchHandler.getRunningOnePhaseState();
	}

	public State getRunningThreePhaseState() {
		return this.phaseSwitchHandler.getRunningThreePhaseState();
	}

	public State getSwitchToThreePhaseState() {
		return this.phaseSwitchHandler.getSwitchToThreePhaseState();
	}

	public int getMinPowerThreePhase() {
		return this.config.minHwCurrent();
	}

	public int calculateCurrent(int power, int phases) {
		var current = Math.round((power * 1000) / phases / 230f);
		/*
		 * Limits the charging value because KEBA knows only values between 6000 and
		 * 63000
		 */
		current = Math.min(current, 63_000);

		if (current < 6000) {
			current = 0;
		}

		return current;
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

	private boolean isPhaseSwitchSourceSet() {
		IntegerReadChannel channel = getX2PhaseSwitchSourceChannel();

		// Use the channel to read the value directly as an Integer.
		// orElse(0) is safely used here because the method call is type-safe and
		// expects an Integer.
		Integer phaseSwitchSource = channel.value().orElse(0);

		return phaseSwitchSource == 4;
	}

}
