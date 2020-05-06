package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

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
public class ControllerHeatingElement extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerHeatingElement.class);

	private final PhaseDef phase1;
	private final PhaseDef phase2;
	private final PhaseDef phase3;

	private Config config;

	/**
	 * Holds the minimum energy the phases should be switch on in [Ws].
	 */
	private long minimumEnergy;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	public ControllerHeatingElement() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				MyChannelId.values() //
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

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) {
		this.config = config;
		this.minimumEnergy = calculateMinimumEnergy(config);
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
		int phase1Time = (int) this.phase1.getTotalDuration().getSeconds();
		int phase2Time = (int) this.phase2.getTotalDuration().getSeconds();
		int phase3Time = (int) this.phase3.getTotalDuration().getSeconds();
		int totalPhaseTime = phase1Time + phase2Time + phase3Time;

		// Calculate Phase Energy
		int phase1Energy = this.phase1.getTotalEnergy();
		int phase2Energy = this.phase2.getTotalEnergy();
		int phase3Energy = this.phase3.getTotalEnergy();
		int totalPhaseEnergy = phase1Energy + phase2Energy + phase3Energy;

		// Update Channels
		this.channel(MyChannelId.PHASE1_TIME).setNextValue(phase1Time);
		this.channel(MyChannelId.PHASE2_TIME).setNextValue(phase2Time);
		this.channel(MyChannelId.PHASE3_TIME).setNextValue(phase3Time);
		this.channel(MyChannelId.TOTAL_PHASE_TIME).setNextValue(totalPhaseTime);

		this.channel(MyChannelId.PHASE1_ENERGY).setNextValue(phase1Energy);
		this.channel(MyChannelId.PHASE2_ENERGY).setNextValue(phase2Energy);
		this.channel(MyChannelId.PHASE3_ENERGY).setNextValue(phase3Energy);
		this.channel(MyChannelId.TOTAL_ENERGY).setNextValue(totalPhaseEnergy);

		this.channel(MyChannelId.LEVEL1_TIME).setNextValue(phase1Time - phase2Time);
		this.channel(MyChannelId.LEVEL2_TIME).setNextValue(phase2Time - phase3Time);
		this.channel(MyChannelId.LEVEL3_TIME).setNextValue(phase3Time);

		this.channel(MyChannelId.LEVEL1_ENERGY).setNextValue(phase1Energy - phase2Energy);
		this.channel(MyChannelId.LEVEL2_ENERGY).setNextValue((phase2Energy - phase3Energy) * 2);
		this.channel(MyChannelId.LEVEL3_ENERGY).setNextValue(phase3Energy * 3);
	}

	/**
	 * Handle Mode "Manual On".
	 * 
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void modeManualOn() throws IllegalArgumentException, OpenemsNamedException {
		this.phase1.switchOn();
		this.phase2.switchOn();
		this.phase3.switchOn();
	}

	/**
	 * Handle Mode "Manual Off".
	 * 
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void modeManualOff() throws IllegalArgumentException, OpenemsNamedException {
		this.phase1.switchOff();
		this.phase2.switchOff();
		this.phase3.switchOff();
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
			excessPower = (gridActivePower * -1) - essDischargePower
					+ (this.currentLevel.getValue() * this.config.powerPerPhase());
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
		LocalTime now = LocalTime.now(this.componentManager.getClock());
		LocalTime configuredEndTime = LocalTime.parse(this.config.endTime());
		LocalTime latestForceChargeStartTime = this.calculateLatestForceHeatingStartTime();
		if (now.isAfter(latestForceChargeStartTime) && now.isBefore(configuredEndTime)) {
			if (targetLevel.getValue() < this.config.defaultLevel().getValue()) {
				targetLevel = this.config.defaultLevel(); // force-heat with configured default level
			}
		}

		// Apply Hysteresis
		targetLevel = this.applyHysteresis(targetLevel);

		// Update Channels
		this.channel(MyChannelId.LEVEL).setNextValue(targetLevel);
		this.channel(MyChannelId.FORCE_START_AT_SECONDS_OF_DAY)
				.setNextValue(latestForceChargeStartTime.toSecondOfDay());

		// Apply Level
		this.applyLevel(targetLevel);

	}

	/**
	 * Calculates the minimum energy the user demands in [Ws].
	 * 
	 * <ul>
	 * <li>in {@link WorkMode#KWH}: kWh x 1000 * 3600
	 * <li>in {@link WorkMode#TIME}: minTime [h] x 3600 x defaultLevel-Phases x
	 * power-per-phase [W]
	 * <li>in {@link WorkMode#NONE}: always return 0
	 * </ul>
	 * 
	 * @return the minimum demanded energy in [Ws]
	 */
	private static long calculateMinimumEnergy(Config config) {
		switch (config.workMode()) {
		case TIME:
			return config.minTime() * 3600 * config.defaultLevel().getValue() * config.powerPerPhase();
		case KWH:
			return config.minKwh() * 1000 * 3600;
		case NONE:
			return 0;
		}
		assert (true);
		return 0;
	}

	/**
	 * Calculates the time from when force-heating needs to start latest.
	 * 
	 * @return the time, or {@link LocalTime#MAX} if no force-heating is required
	 */
	private LocalTime calculateLatestForceHeatingStartTime() {
		long totalPhaseTime = this.phase1.getTotalDuration().getSeconds() //
				+ this.phase2.getTotalDuration().getSeconds() //
				+ this.phase3.getTotalDuration().getSeconds(); // [s]
		long totalEnergy = (totalPhaseTime /* [s] */ * this.getPowerPerPhase() /* [W] */); // [Ws]
		long remainingEnergy = this.minimumEnergy - totalEnergy; // [Ws]
		if (remainingEnergy < 0) {
			return LocalTime.MAX;
		} else {
			long remainingTime = remainingEnergy / (config.defaultLevel().getValue() * config.powerPerPhase()); // [s]
			LocalTime endTime = LocalTime.parse(this.config.endTime());
			return endTime.minusSeconds(remainingTime);
		}
	}

	/**
	 * Switch on Phases according to selected {@link Level}.
	 * 
	 * @param level the target Level
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	public void applyLevel(Level level) throws IllegalArgumentException, OpenemsNamedException {
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
			LocalDateTime now = LocalDateTime.now(this.componentManager.getClock());
			Duration hysteresis = Duration.ofSeconds(this.config.minimumSwitchingTime());
			if (this.lastLevelChange.plus(hysteresis).isBefore(now)) {
				// no hysteresis applied
				this.currentLevel = targetLevel;
				this.lastLevelChange = now;
				this.channel(MyChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			} else {
				// wait for hysteresis
				this.channel(MyChannelId.AWAITING_HYSTERESIS).setNextValue(true);
			}
		} else {
			// Level was not changed
			this.channel(MyChannelId.AWAITING_HYSTERESIS).setNextValue(false);
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
		ChannelAddress channelAddress = this.getChannelAddressForPhase(phase);
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(channelAddress);
		Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + (value) + ".");
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
		assert (true); // can never happen
		return null;
	}
}