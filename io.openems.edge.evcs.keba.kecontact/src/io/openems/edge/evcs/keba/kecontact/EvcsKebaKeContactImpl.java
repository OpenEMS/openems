package io.openems.edge.evcs.keba.kecontact;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	private Instant lastPhaseChangeTime = Instant.MIN;
	private static final long PHASE_SWITCH_COOLDOWN_SECONDS = 310;

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
		if (s.startsWith("x2") && !isPhaseSwitchSourceSet()) {
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

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		Instant now = Instant.now(this.componentManager.getClock());
		this.log.debug("Applying charge power limit: [Power: " + power + "W] at [" + now + "]");

		if (!isPhaseSwitchSourceSet()) {
			if (!this.send("x2src 4")) {
				this.log.error("Failed to set phase switch source to UDP control.");
				return false;
			}
		}

		boolean isPhaseSwitchCooldownOver = this.lastPhaseChangeTime.plusSeconds(PHASE_SWITCH_COOLDOWN_SECONDS)
				.isBefore(now);
		this.log.debug("Phase switch cooldown over: " + isPhaseSwitchCooldownOver);

		var phasesValue = this.getPhasesAsInt();
		this.log.debug("Current charging phase configuration: " + phasesValue + " phases");

		var current = this.calculateCurrent(power, phasesValue);
		this.log.debug("Calculated current for charging: " + current + "mA");

		/*
		 * Determine if switching phases is necessary
		 */
		IntegerReadChannel maxCurrentCannel = this.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_MAX_HW);
		final var phases = this.getPhases();
		var preferredPhases = Phases.preferredPhaseBehavior(power, this.getPhases(), this.config.minHwCurrent(),
				maxCurrentCannel.value().orElse(DEFAULT_MAXIMUM_HARDWARE_CURRENT));

		this.logDebug("Preferred: " + preferredPhases);
		if (phases != preferredPhases && isPhaseSwitchCooldownOver) {
			// Send previous value before switching phases
			boolean sendPreviousSuccess = this.send("currtime " + current + " 1");
			this.log.debug("Sent previous value before phase switching. Success: " + sendPreviousSuccess);

			// Schedule the phase switch after a short delay
			this.scheduler.schedule(() -> {
				try {
					boolean switchSuccess = this.switchPhases(preferredPhases, now);
					if (!switchSuccess) {
						this.log.info("Phase switch failed. Exiting.");
						return false; // Early exit if phase switch failed
					}

					// Send command to set current
					boolean sendSuccess = this.send("currtime " + current + " 1");
					this.log.debug("Command to set current sent. Success: " + sendSuccess);
					return sendSuccess;

				} catch (Exception e) {
					this.log.error("Error during phase switch delay handling", e);
					return false;
				}
			}, 1, TimeUnit.SECONDS);

			// Return early to avoid blocking the main thread
			return true;
		}

		// TODO: Remove log.
		if (!isPhaseSwitchCooldownOver) {
			Duration timeUntilNextSwitch = Duration.between(now,
					this.lastPhaseChangeTime.plusSeconds(PHASE_SWITCH_COOLDOWN_SECONDS));
			long secondsUntilNextSwitch = timeUntilNextSwitch.getSeconds();
			this.channel(EvcsKebaKeContact.ChannelId.PHASE_SWITCH_COOLDOWN).setNextValue(secondsUntilNextSwitch);
			this.log.info("Phase switch cooldown period has not passed. Time before next switch: "
					+ secondsUntilNextSwitch + " seconds.");
		}

		// Send command to set current
		boolean sendSuccess = this.send("currtime " + current + " 1");
		this.log.debug("Command to set current sent. Success: " + sendSuccess);

		return sendSuccess;
	}

	// protected static boolean shouldSwitchToOnePhase(int power, int phases) {
	// return power < 4140 && phases == 3;
	// }

	private int calculateCurrent(int power, int phases) {
		var current = Math.round((power * 1000) / phases / 230f);
		/*
		 * Limits the charging value because KEBA knows only values between 6000 and
		 * 63000
		 */
		current = Math.min(current, 63_000);

		if (current < 6000) {
			current = 0;
		}

		// boolean shouldSwitchToThreePhases(int power, int phases) {
		// boolean shouldSwitch = power > 4140 && phases != 3;
		// this.log.debug("Should switch to 3 phases: " + shouldSwitch + " [Power: " +
		// power + "W, Current phases: "
		// + phases + "]");
		// return shouldSwitch;
		// }

		return current;
	}

	private boolean switchPhases(Phases prefferedPhases, Instant now) {

		if (prefferedPhases == Phases.TWO_PHASE) {
			// Set KEBA to two phases is not possible
			prefferedPhases = Phases.THREE_PHASE;
		}
		String command = prefferedPhases == Phases.ONE_PHASE ? "x2 0" : "x2 1";
		if (this.send(command)) {
			this.lastPhaseChangeTime = now; // Update the cooldown timer regardless of the phase switch direction
			this.log.info(
					"Switched to " + (prefferedPhases == Phases.ONE_PHASE ? "1 phase" : "3 phases") + " successfully.");
			return true;
		} else {
			this.log.warn(
					"Failed to switch to " + (prefferedPhases == Phases.ONE_PHASE ? "1 phase" : "3 phases") + ".");
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

	private boolean isPhaseSwitchSourceSet() {
		IntegerReadChannel channel = getX2PhaseSwitchSourceChannel();

		// Use the channel to read the value directly as an Integer.
		// orElse(0) is safely used here because the method call is type-safe and
		// expects an Integer.
		Integer phaseSwitchSource = channel.value().orElse(0);

		return phaseSwitchSource == 4;
	}

}
