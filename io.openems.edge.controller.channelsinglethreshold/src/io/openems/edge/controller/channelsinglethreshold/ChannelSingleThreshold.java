package io.openems.edge.controller.channelsinglethreshold;

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
		name = "Controller.ChannelSingleThreshold", //
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
		switch (this.config.mode()) {
		case ON:
			this.setOutput(true);
			break;
		case OFF:
			this.setOutput(false);
			break;
		case AUTOMATIC:
			this.automaticMode();
			break;
		}
	}

	/**
	 * Automated control.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void automaticMode() throws OpenemsNamedException {
		
		ChannelAddress inputChannelAddress = ChannelAddress.fromString(this.config.inputChannelAddress());
		ChannelAddress outputChannelAddress = ChannelAddress.fromString(this.config.outputChannelAddress());

		// Get input value
		IntegerReadChannel inputChannel = this.componentManager.getChannel(inputChannelAddress);
		int value = inputChannel.value().getOrError();
		
		// Get output status (true or false)
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(outputChannelAddress);
		Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
		
		if((currentValueOpt.isPresent() ^ this.config.invert()) == true) {
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
			}
			this.setOutput(false);
			break;

		case BELOW_THRESHOLD:
			/*
			 * Value is smaller than the low threshold -> always OFF
			 */
			if (value >= this.config.threshold()) {
				this.changeState(State.ABOVE_THRESHOLD);
			}
			this.setOutput(false);
			break;

		case ABOVE_THRESHOLD:
			/*
			 * Value is bigger than the high threshold -> always ON
			 */
			if (value <= this.config.threshold()) {
				this.changeState(State.BELOW_THRESHOLD);
			}
			this.setOutput(true);
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
	 * @param value true to switch ON, false to switch ON; is inverted if
	 *              'invertOutput' config is set
	 * @throws OpenemsNamedException on error
	 */
	private void setOutput(boolean value) throws OpenemsNamedException {
		ChannelAddress outputChannelAddress = ChannelAddress.fromString(this.config.outputChannelAddress());
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(outputChannelAddress);
		Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != (value ^ this.config.invert())) {
			this.logInfo(this.log, "Set output [" + outputChannel.address() + "] "
					+ (value ^ this.config.invert() ? "ON" : "OFF") + ".");
			outputChannel.setNextWriteValue(value ^ this.config.invert());
		}
	}
}
