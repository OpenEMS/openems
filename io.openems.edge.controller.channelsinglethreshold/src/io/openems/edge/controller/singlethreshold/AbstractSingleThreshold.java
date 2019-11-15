package io.openems.edge.controller.singlethreshold;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

public abstract class AbstractSingleThreshold extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractSingleThreshold.class);

	private final Clock clock;
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	@Reference
	protected ComponentManager componentManager;

	protected abstract ComponentManager getComponentManager();

	protected ChannelAddress inputChannelAddress;
	protected ChannelAddress outputChannelAddress;
	protected int threshold = 0;
	protected boolean invertOutput = false;
	protected TemporalAmount hysteresis;

	protected State state = State.UNDEFINED;
	protected Mode mode;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(Mode.values()) //
				.initialValue(Mode.AUTOMATIC) //
				.text("Configured Mode")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public AbstractSingleThreshold() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = Clock.systemDefaultZone();
	}

	public AbstractSingleThreshold(Clock clock, String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values()//
		);
		this.clock = clock;
	}

	public void run() throws IllegalArgumentException, OpenemsNamedException {

		boolean modeChanged;

		do {
			modeChanged = false;
			switch (this.mode) {
			case ON:
				this.on();
				modeChanged = this.changeMode(Mode.ON);
				break;
			case OFF:
				this.off();
				modeChanged = this.changeMode(Mode.OFF);
				break;
			case AUTOMATIC:
				this.automaticMode();
				modeChanged = this.changeMode(Mode.AUTOMATIC);
				break;
			}
		} while (modeChanged);

		this.channel(ChannelId.MODE).setNextValue(this.mode);
	}

	private void automaticMode() throws IllegalArgumentException, OpenemsNamedException {
		/*
		 * Check if all parameters are available
		 */
		int value;
		try {
			Channel<?> inputChannel = this.getComponentManager().getChannel(this.inputChannelAddress);
			value = TypeUtils.getAsType(OpenemsType.INTEGER, inputChannel.value().getOrError());
		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
			return;
		}

		/*
		 * State Machine
		 */
		switch (this.state) {
		case UNDEFINED:

			if (value < this.threshold) {
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.state = State.BELOW_THRESHOLD;
			} else {
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.state = State.ABOVE_THRESHOLD;
			}

			this.off();
			break;

		case BELOW_THRESHOLD:
			/*
			 * Value is smaller than the low threshold -> always OFF
			 */
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				if (value >= this.threshold) {
					this.lastStateChange = LocalDateTime.now(this.clock);
					this.state = State.ABOVE_THRESHOLD;
					break;
				}
			}

			this.off();
			break;

		case ABOVE_THRESHOLD:
			/*
			 * Value is bigger than the high threshold -> always OFF
			 */
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				if (value <= this.threshold) {
					this.lastStateChange = LocalDateTime.now(this.clock);
					this.state = State.BELOW_THRESHOLD;
				}
			}

			this.on();
			break;
		}
	}

	/**
	 * Switch the output ON.
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void on() throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(true);
	}

	/**
	 * Switch the output OFF.
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void off() throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(false);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value true to switch ON, false to switch ON; is inverted if
	 *              'invertOutput' config is set
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void setOutput(boolean value) throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.getComponentManager().getChannel(this.outputChannelAddress);
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != (value ^ this.invertOutput)) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] "
						+ (value ^ this.invertOutput ? "ON" : "OFF") + ".");
				outputChannel.setNextWriteValue(value ^ invertOutput);
			}
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set output: [" + this.outputChannelAddress + "] " + e.getMessage());
		}
	}

	/**
	 * A flag to maintain change in the mode.
	 * 
	 * @param nextMode the target mode
	 * @return Flag that the mode is changed or not
	 */
	private boolean changeMode(Mode nextMode) {
		if (this.mode != nextMode) {
			this.mode = nextMode;
			return true;
		} else {
			return false;
		}
	}

}
