package io.openems.edge.controller.io.channelsinglethreshold;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.IO.ChannelSingleThreshold", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ChannelSingleThreshold extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		AWAITING_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hystesis is active")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	protected ComponentManager componentManager;

	private final Logger log = LoggerFactory.getLogger(ChannelSingleThreshold.class);
	private final Clock clock;

	private Config config;
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	/**
	 * The current state in the State Machine.
	 */
	private State state = State.UNDEFINED;

	public ChannelSingleThreshold() {
		this(Clock.systemDefaultZone());
	}

	public ChannelSingleThreshold(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = Clock.systemDefaultZone();
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		ChannelAddress outputChannelAddress = ChannelAddress.fromString(this.config.outputChannelAddress());

		// Get output status (true or false)
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(outputChannelAddress);
		Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();

		switch (this.config.mode()) {
		case ON:
			this.setOutput(outputChannel, currentValueOpt, true);
			break;
		case OFF:
			this.setOutput(outputChannel, currentValueOpt, false);
			break;
		case AUTOMATIC:
			this.automaticMode(outputChannel, currentValueOpt);
			break;
		}
	}

	/**
	 * Automated control.
	 * 
	 * @param currentValueOpt
	 * @param outputChannel
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void automaticMode(WriteChannel<Boolean> outputChannel, Optional<Boolean> currentValueOpt)
			throws OpenemsNamedException {

		ChannelAddress inputChannelAddress = ChannelAddress.fromString(this.config.inputChannelAddress());

		// Get input value
		IntegerReadChannel inputChannel = this.componentManager.getChannel(inputChannelAddress);
		int value = inputChannel.value().getOrError();
		/*
		 * Condition applies only when the input channel is the Grid Active Power
		 * 
		 * Power value of the output device is added to the input channel value to avoid
		 * immediate switching based on threshold.
		 * 
		 * example use case: if the feed-in is more than threshold, the output device is
		 * switched on and next second feed-in reduces below threshold and immediately
		 * switches off the device.
		 */
		if ((currentValueOpt.orElse(false))) {
			value -= this.config.switchedLoadPower();
		}

		// State Machine
		switch (this.state) {
		case UNDEFINED:
			/*
			 * Starting... state is still undefined
			 */
			if (value < this.config.threshold()) {
				this.changeState(State.BELOW_THRESHOLD);
			} else {
				this.changeState(State.ABOVE_THRESHOLD);
				break;
			}
			this.setOutput(outputChannel, currentValueOpt, false ^ this.config.invert());
			break;

		case BELOW_THRESHOLD:
			/*
			 * Value is smaller than the low threshold -> always OFF
			 */
			if (value > this.config.threshold()) {
				this.changeState(State.ABOVE_THRESHOLD);
				break;
			}
			this.setOutput(outputChannel, currentValueOpt, false ^ this.config.invert());
			break;

		case ABOVE_THRESHOLD:
			/*
			 * Value is bigger than the high threshold -> always ON
			 */
			if (value <= this.config.threshold()) {
				this.changeState(State.BELOW_THRESHOLD);
				break;
			}
			this.setOutput(outputChannel, currentValueOpt, true ^ this.config.invert());
			break;
		}
	}

	/**
	 * A flag to maintain change in the state.
	 * 
	 * @param nextState the target state
	 */
	private void changeState(State nextState) {
		Duration hysteresis = Duration.ofSeconds(this.config.minimumSwitchingTime());
		if (this.state != nextState) {
			if (this.lastStateChange.plus(hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			} else {
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
			}
		} else {
			this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
		}
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 * 
	 * @param currentValueOpt2
	 * @param outputChannel2
	 *
	 * @param value            true to switch ON, false to switch ON
	 * @throws OpenemsNamedException on error
	 */
	private void setOutput(WriteChannel<Boolean> outputChannel, Optional<Boolean> currentValueOpt, boolean value)
			throws OpenemsNamedException {
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + (value ? "ON" : "OFF") + ".");
			outputChannel.setNextWriteValue(value);
		}
	}
}
