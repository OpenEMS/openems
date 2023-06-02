package io.openems.edge.controller.io.heatpump.sgready;

import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Io.HeatPump.SgReady", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class HeatPumpImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, HeatPump, EventHandler, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(HeatPumpImpl.class);

	/*
	 * Status definitions for each state. Are responsible for the time calculation
	 * activation of that state and storing their meta data.
	 */
	private final StatusDefinition lockState = new StatusDefinition(this, Status.LOCK,
			HeatPump.ChannelId.LOCK_STATE_TIME);
	private final StatusDefinition regularState = new StatusDefinition(this, Status.REGULAR,
			HeatPump.ChannelId.REGULAR_STATE_TIME);
	private final StatusDefinition recommState = new StatusDefinition(this, Status.RECOMMENDATION,
			HeatPump.ChannelId.RECOMMENDATION_STATE_TIME);
	private final StatusDefinition forceOnState = new StatusDefinition(this, Status.FORCE_ON,
			HeatPump.ChannelId.FORCE_ON_STATE_TIME);

	@Reference
	protected Sum sum;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config = null;
	protected Status activeState = Status.UNDEFINED;
	protected Instant lastStateChange = Instant.MIN;

	public HeatPumpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				HeatPump.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			// Updates the time channel depending if the state is active or not.
			this.lockState.updateActiveTime();
			this.regularState.updateActiveTime();
			this.recommState.updateActiveTime();
			this.forceOnState.updateActiveTime();
			break;
		}
	}

	@Override
	public void run() throws OpenemsNamedException {

		// Handle Mode AUTOMATIC and MANUAL
		switch (this.config.mode()) {
		case AUTOMATIC:
			this.modeAutomatic();
			break;

		case MANUAL:
			this.modeManual();
			break;
		}
	}

	/**
	 * Automatic mode.
	 *
	 * <p>
	 * Sets the digital outputs and the state depending on the surplus or grid-buy
	 * power.
	 *
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void modeAutomatic() throws IllegalArgumentException, OpenemsNamedException {

		// Detect if hysteresis is active, depending on the minimum switching time
		if (this.lastStateChange.plusSeconds(this.config.minimumSwitchingTime())
				.isAfter(Instant.now(this.componentManager.getClock()))) {
			this._setAwaitingHysteresis(true);
			return;
		}
		this._setAwaitingHysteresis(false);

		// Values to calculate the surplus/grid-buy power
		var gridActivePower = this.getGridActivePowerOrZero();
		var soc = this.getEssSocOrZero();
		var essDischargePower = this.getEssDischargePowerOrZero();

		// We are only interested in discharging, not charging
		essDischargePower = essDischargePower < 0 ? 0 : essDischargePower;

		// Calculate power used by the heat pump
		var heatPumpPower = this.recommState.isActive() ? this.config.automaticRecommendationSurplusPower() : 0;
		heatPumpPower = this.forceOnState.isActive() ? this.config.automaticForceOnSurplusPower() : heatPumpPower;

		// Calculate surplus power
		long surplusPower = gridActivePower * -1 - essDischargePower + heatPumpPower;

		// Check conditions for lock mode (Lock mode is not depending on the
		// essDischarge Power)
		if (this.config.automaticLockCtrlEnabled() && gridActivePower > this.config.automaticLockGridBuyPower()
				&& soc < this.config.automaticLockSoc()) {
			this.lockState.switchOn();
			return;
		}

		// Check conditions for force on mode
		if (this.config.automaticForceOnCtrlEnabled() && surplusPower > this.config.automaticForceOnSurplusPower()
				&& soc >= this.config.automaticForceOnSoc()) {
			this.forceOnState.switchOn();
			return;
		}

		// Check conditions for recommendation mode
		if (this.config.automaticRecommendationCtrlEnabled()
				&& surplusPower > this.config.automaticRecommendationSurplusPower()) {
			this.recommState.switchOn();
			return;
		}

		// No conditions fulfilled
		this.regularState.switchOn();

	}

	/**
	 * Manual mode.
	 *
	 * <p>
	 * Sets the digital outputs and the state depending on a fix user input.
	 *
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void modeManual() throws IllegalArgumentException, OpenemsNamedException {
		var state = this.config.manualState();
		switch (state) {
		case FORCE_ON:
			this.forceOnState.switchOn();
			break;
		case LOCK:
			this.lockState.switchOn();
			break;
		case RECOMMENDATION:
			this.recommState.switchOn();
			break;
		case REGULAR:
			this.regularState.switchOn();
			break;
		case UNDEFINED:
			this.regularState.switchOn();
			break;
		}
	}

	private int getEssDischargePowerOrZero() {
		return this.getChannelValueOrZeroAndSetStateChannel(this.sum.getEssDischargePowerChannel(),
				this.getEssDischargePowerNotPresentChannel());
	}

	private int getEssSocOrZero() {
		return this.getChannelValueOrZeroAndSetStateChannel(this.sum.getEssSocChannel(),
				this.getStateOfChargeNotPresentChannel());
	}

	private int getGridActivePowerOrZero() {
		return this.getChannelValueOrZeroAndSetStateChannel(this.sum.getGridActivePowerChannel(),
				this.getGridActivePowerNotPresentChannel());
	}

	/**
	 * Get the IntegerReadChannel value or 0 if not present - Sets also the
	 * according state channel depending on the channel.
	 *
	 * @param channel      Channel that value should be read.
	 * @param stateChannel Referring StateChannel that will be set if the value is
	 *                     not present.
	 * @return Current channel value as int.
	 */
	private int getChannelValueOrZeroAndSetStateChannel(IntegerReadChannel channel, StateChannel stateChannel) {
		var channelOptional = channel.getNextValue().asOptional();

		if (channelOptional.isPresent()) {
			stateChannel.setNextValue(false);
			return channelOptional.get();
		}
		stateChannel.setNextValue(true);
		return 0;
	}

	/**
	 * Helper method to set the two booleans for the two outputs.
	 *
	 * @param output1 Value that should be set on output 1.
	 * @param output2 Value that should be set on output 2.
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	protected void setOutputs(boolean output1, boolean output2) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(ChannelAddress.fromString(this.config.outputChannel1()), output1);
		this.setOutput(ChannelAddress.fromString(this.config.outputChannel2()), output2);
	}

	/**
	 * Switch an output if it was not switched before.
	 *
	 * @param channelAddress The address of the channel.
	 * @param value          Boolean that should be set on the output.
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	protected void setOutput(ChannelAddress channelAddress, boolean value)
			throws IllegalArgumentException, OpenemsNamedException {

		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(channelAddress);
		var currentValueOpt = outputChannel.value().asOptional();

		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			this.logDebug(this.log, "Set output [" + outputChannel.address() + "] " + value + ".");
			outputChannel.setNextWriteValue(value);
		}
	}

	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	/**
	 * Change of a state.
	 *
	 * <p>
	 * Sets the digital outputs, the currently active status and the lastStateChange
	 * time set point.
	 *
	 * @param status New active status
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	public void changeState(Status status) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutputs(status.getOutput1(), status.getOutput2());
		this._setStatus(status);
		this.activeState = status;
		this.lastStateChange = Instant.now(this.componentManager.getClock());
	}
}
