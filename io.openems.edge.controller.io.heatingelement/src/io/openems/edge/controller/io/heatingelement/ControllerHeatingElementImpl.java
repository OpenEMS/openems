package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.IO.HeatingElement", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerHeatingElementImpl extends AbstractOpenemsComponent
		implements ControllerHeatingElement, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerHeatingElementImpl.class);

	private final PhaseDef phase1;
	private final PhaseDef phase2;
	private final PhaseDef phase3;

	private Config config;

	/**
	 * Holds the minimum time the phases should be switch on in [Ws].
	 */
	private long minimumTotalPhaseTime;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	public ControllerHeatingElementImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerHeatingElement.ChannelId.values() //
		);
		this.phase1 = new PhaseDef(this, Phase.L1);
		this.phase2 = new PhaseDef(this, Phase.L2);
		this.phase3 = new PhaseDef(this, Phase.L3);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Modified
	void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) {
		this.config = config;
		this.minimumTotalPhaseTime = calculateMinimumTotalPhaseTime(config);
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Handle Mode AUTOMATIC, MANUAL_OFF or MANUAL_ON
		switch (this.config.mode()) {
		case AUTOMATIC:
			this.modeAutomatic();
			break;

		case MANUAL_OFF:
			this.modeManualOff();
			break;

		case MANUAL_ON:
			this.modeManualOn();
			break;
		}

		// Calculate Phase Time
		var phase1Time = (int) this.phase1.getTotalDuration().getSeconds();
		var phase2Time = (int) this.phase2.getTotalDuration().getSeconds();
		var phase3Time = (int) this.phase3.getTotalDuration().getSeconds();
		var totalPhaseTime = phase1Time + phase2Time + phase3Time;

		// Update Channels
		this.channel(ControllerHeatingElement.ChannelId.PHASE1_TIME).setNextValue(phase1Time);
		this.channel(ControllerHeatingElement.ChannelId.PHASE2_TIME).setNextValue(phase2Time);
		this.channel(ControllerHeatingElement.ChannelId.PHASE3_TIME).setNextValue(phase3Time);
		this.channel(ControllerHeatingElement.ChannelId.TOTAL_PHASE_TIME).setNextValue(totalPhaseTime);

		this.channel(ControllerHeatingElement.ChannelId.LEVEL1_TIME).setNextValue(phase1Time - phase2Time);
		this.channel(ControllerHeatingElement.ChannelId.LEVEL2_TIME).setNextValue(phase2Time - phase3Time);
		this.channel(ControllerHeatingElement.ChannelId.LEVEL3_TIME).setNextValue(phase3Time);
	}

	/**
	 * Handle Mode "Manual On".
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void modeManualOn() throws IllegalArgumentException, OpenemsNamedException {
		this.applyLevel(this.config.defaultLevel());
	}

	/**
	 * Handle Mode "Manual Off".
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void modeManualOff() throws IllegalArgumentException, OpenemsNamedException {
		this.applyLevel(Level.LEVEL_0);
	}

	/**
	 * Handle Mode "Automatic".
	 *
	 * @throws IllegalArgumentException on error.
	 * @throws OpenemsNamedException    on error.
	 */
	protected void modeAutomatic() throws IllegalArgumentException, OpenemsNamedException {
		// Get the input channel addresses
		IntegerReadChannel gridActivePowerChannel = this.sum.channel(Sum.ChannelId.GRID_ACTIVE_POWER);
		int gridActivePower = gridActivePowerChannel.value().getOrError();
		IntegerReadChannel essDischargePowerChannel = this.sum.getEssDischargePowerChannel();
		int essDischargePower = essDischargePowerChannel.value().orElse(0 /* if there is no storage */);
		if (essDischargePower < 0) { // we are only interested in discharging, not charging
			essDischargePower = 0;
		}

		long excessPower;
		if (gridActivePower > 0) {
			excessPower = 0;
		} else {
			excessPower = gridActivePower * -1 - essDischargePower
					+ this.currentLevel.getValue() * this.config.powerPerPhase();
		}

		// Calculate Level from excessPower
		Level targetLevel;
		if (excessPower >= this.config.powerPerPhase() * 3) {
			targetLevel = Level.LEVEL_3;
		} else if (excessPower >= this.config.powerPerPhase() * 2) {
			targetLevel = Level.LEVEL_2;
		} else if (excessPower >= this.config.powerPerPhase()) {
			targetLevel = Level.LEVEL_1;
		} else {
			targetLevel = Level.LEVEL_0;
		}

		// Do we need to force-heat?
		var now = LocalTime.now(this.componentManager.getClock());
		var configuredEndTime = LocalTime.parse(this.config.endTime());
		var latestForceChargeStartTime = this.calculateLatestForceHeatingStartTime();
		if (now.isAfter(latestForceChargeStartTime) && now.isBefore(configuredEndTime)) {
			if (targetLevel.getValue() < this.config.defaultLevel().getValue()) {
				targetLevel = this.config.defaultLevel(); // force-heat with configured default level
			}
		}

		// Apply Hysteresis
		targetLevel = this.applyHysteresis(targetLevel);

		// Update Channel
		this.channel(ControllerHeatingElement.ChannelId.FORCE_START_AT_SECONDS_OF_DAY)
				.setNextValue(latestForceChargeStartTime.toSecondOfDay());

		// Apply Level
		this.applyLevel(targetLevel);

	}

	/**
	 * Calculates the minimum total phase time the user demands in [s].
	 *
	 * <ul>
	 * <li>in {@link WorkMode#TIME}:
	 * <ul>
	 * <li>default level {@link Level#LEVEL_0}: return 0
	 * <li>default level {@link Level#LEVEL_1}: return configured time
	 * <li>default level {@link Level#LEVEL_2}: return configured time * 2
	 * <li>default level {@link Level#LEVEL_3}: return configured time * 3
	 * </ul>
	 * <li>in {@link WorkMode#NONE}: always return 0
	 * </ul>
	 *
	 * @return the minimum total phase time [s]
	 */
	private static long calculateMinimumTotalPhaseTime(Config config) {
		switch (config.workMode()) {
		case TIME:
			switch (config.defaultLevel()) {
			case LEVEL_0:
				return 0;
			case LEVEL_1:
				return config.minTime() * 3600;
			case LEVEL_2:
				return config.minTime() * 3600 * 2;
			case LEVEL_3:
				return config.minTime() * 3600 * 3;
			case UNDEFINED:
				return 0;
			}
		case NONE:
			return 0;
		}
		assert true;
		return 0;
	}

	/**
	 * Calculates the time from when force-heating needs to start latest.
	 *
	 * @return the time, or {@link LocalTime#MAX} if no force-heating is required
	 */
	private LocalTime calculateLatestForceHeatingStartTime() {
		var totalPhaseTime = this.phase1.getTotalDuration().getSeconds() //
				+ this.phase2.getTotalDuration().getSeconds() //
				+ this.phase3.getTotalDuration().getSeconds(); // [s]
		var remainingTotalPhaseTime = this.minimumTotalPhaseTime - totalPhaseTime; // [s]
		if (remainingTotalPhaseTime < 0) {
			return LocalTime.MAX;
		}
		var endTime = LocalTime.parse(this.config.endTime());
		switch (this.config.defaultLevel()) {
		case LEVEL_0:
		case UNDEFINED:
		case LEVEL_1:
			// keep value
			break;
		case LEVEL_2:
			remainingTotalPhaseTime /= 2;
			break;
		case LEVEL_3:
			remainingTotalPhaseTime /= 3;
			break;
		}
		return endTime.minusSeconds(remainingTotalPhaseTime);
	}

	/**
	 * Switch on Phases according to selected {@link Level}.
	 *
	 * @param level the target Level
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	public void applyLevel(Level level) throws IllegalArgumentException, OpenemsNamedException {
		// Update Channel
		this.channel(ControllerHeatingElement.ChannelId.LEVEL).setNextValue(level);

		// Set phases accordingly
		switch (level) {
		case UNDEFINED:
		case LEVEL_0:
			this.phase1.switchOff();
			this.phase2.switchOff();
			this.phase3.switchOff();
			break;
		case LEVEL_1:
			this.phase1.switchOn();
			this.phase2.switchOff();
			this.phase3.switchOff();
			break;
		case LEVEL_2:
			this.phase1.switchOn();
			this.phase2.switchOn();
			this.phase3.switchOff();
			break;
		case LEVEL_3:
			this.phase1.switchOn();
			this.phase2.switchOn();
			this.phase3.switchOn();
			break;
		}
	}

	/**
	 * Applies the {@link #HYSTERESIS} to avoid too quick changes of Levels.
	 *
	 * @param targetLevel the target {@link Level}
	 * @return the targetLevel if no hysteresis needs to be applied; the
	 *         currentLevel if hysteresis is to be applied
	 */
	private Level applyHysteresis(Level targetLevel) {
		if (this.currentLevel != targetLevel) {
			var now = LocalDateTime.now(this.componentManager.getClock());
			var hysteresis = Duration.ofSeconds(this.config.minimumSwitchingTime());
			if (this.lastLevelChange.plus(hysteresis).isBefore(now)) {
				// no hysteresis applied
				this.currentLevel = targetLevel;
				this.lastLevelChange = now;
				this.channel(ControllerHeatingElement.ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			} else {
				// wait for hysteresis
				this.channel(ControllerHeatingElement.ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
			}
		} else {
			// Level was not changed
			this.channel(ControllerHeatingElement.ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
		}
		return this.currentLevel;
	}

	private Level currentLevel = Level.UNDEFINED;
	private LocalDateTime lastLevelChange = LocalDateTime.MIN;

	/**
	 * Gets the configured Power-per-Phase in [W].
	 *
	 * @return power per phase
	 */
	protected int getPowerPerPhase() {
		return this.config.powerPerPhase();
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value                The boolean value which must set on the output
	 *                             channel address.
	 * @param outputChannelAddress The address of the channel.
	 * @throws OpenemsNamedException    on error.
	 * @throws IllegalArgumentException on error.
	 */
	protected void setOutput(Phase phase, boolean value) throws IllegalArgumentException, OpenemsNamedException {
		var channelAddress = this.getChannelAddressForPhase(phase);
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(channelAddress);
		var currentValueOpt = outputChannel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + value + ".");
			outputChannel.setNextWriteValue(value);
		}
	}

	/**
	 * Gets the Output ChannelAddress for a given Phase.
	 *
	 * @param phase the Phase
	 * @return the Output ChannelAddress
	 * @throws OpenemsNamedException on error
	 */
	private ChannelAddress getChannelAddressForPhase(Phase phase) throws OpenemsNamedException {
		switch (phase) {
		case L1:
			return ChannelAddress.fromString(this.config.outputChannelPhaseL1());
		case L2:
			return ChannelAddress.fromString(this.config.outputChannelPhaseL2());
		case L3:
			return ChannelAddress.fromString(this.config.outputChannelPhaseL3());
		}
		assert true; // can never happen
		return null;
	}
}