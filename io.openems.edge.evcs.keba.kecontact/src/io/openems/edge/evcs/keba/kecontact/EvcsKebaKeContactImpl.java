package io.openems.edge.evcs.keba.kecontact;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;

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

// Implement 4140W min for 3 phases !

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

	private final Logger log = LoggerFactory.getLogger(EvcsKebaKeContactImpl.class);
	private final ReadWorker readWorker = new ReadWorker(this);
	private final ReadHandler readHandler = new ReadHandler(this);

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
		var packet = new DatagramPacket(raw, raw.length, this.ip, EvcsKebaKeContactImpl.UDP_PORT);
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
		return this.evcsPower;
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

	private Instant lastPhaseChangeTime = Instant.MIN;

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		Instant now = Instant.now(this.componentManager.getClock());
		this.log.debug("Applying charge power limit: [Power: " + power + "W] at [" + now + "]");

		// Phase switch cooldown: 300 seconds (5 minutes) + 10 second Time to make sure
		// not to overwhelm KEBA :D
		boolean isPhaseSwitchCooldownOver = this.lastPhaseChangeTime.plusSeconds(310).isBefore(now);
		this.log.debug("Phase switch cooldown over: " + isPhaseSwitchCooldownOver);

		var phases = this.getPhasesAsInt();
		this.log.debug("Current charging phase configuration: " + phases + " phases");

		var current = this.calculateCurrent(power, phases);
		this.log.debug("Calculated current for charging: " + current + "mA");

		// Check if switching to 3 phases is possible and cooldown period has passed
		if (this.shouldSwitchToThreePhases(power, phases) && isPhaseSwitchCooldownOver) {
			this.log.info("Attempting to switch to 3 phases for power: " + power + "W");
			boolean switchSuccess = this.switchToThreePhases(now);
			if (!switchSuccess) {
				this.log.info("Phase switch to 3 phases failed. Exiting.");
				return false; // Early exit if phase switch failed
			}
		} else if (!isPhaseSwitchCooldownOver) {
			Duration timeUntilNextSwitch = Duration.between(now, this.lastPhaseChangeTime.plusSeconds(310));
			long secondsUntilNextSwitch = timeUntilNextSwitch.getSeconds();
			this.log.info("Phase switch cooldown period has not passed. Time before next switch: "
					+ secondsUntilNextSwitch + " seconds.");
		}

		boolean sendSuccess = this.send("currtime " + current + " 1");
		this.log.debug("Command to set current sent. Success: " + sendSuccess);

		return sendSuccess;
	}

	private int calculateCurrent(int power, int phases) {
		int current = Math.round((power * 1000) / (phases * 230f));
		this.log.debug("Initial calculated current: " + current + "mA for power: " + power + "W, phases: " + phases);

		// Ensure the current is within KEBA's acceptable range
		current = Math.min(Math.max(current, 6000), 63_000);
		if (current == 6000 && power > 0) {
			// Adjust current for powers just over the threshold for 1-phase charging
			current = Math.max(current, 6000);
		}
		this.log.debug("Adjusted current within KEBA's range: " + current + "mA");
		return current;
	}

	private boolean shouldSwitchToThreePhases(int power, int phases) {
		boolean shouldSwitch = power > 4140 && phases != 3;
		this.log.debug("Should switch to 3 phases: " + shouldSwitch + " [Power: " + power + "W, Current phases: "
				+ phases + "]");
		return shouldSwitch;
	}

	private boolean switchToThreePhases(Instant now) {
		if (this.send("x2 1")) {
			this.lastPhaseChangeTime = now;
			this.log.info("Switched to 3 phases successfully.");
			return true;
		} else {
			this.log.warn("Failed to switch to 3 phases.");
			return false;
		}
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
}
