package io.openems.edge.controller.chp.soc;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.CHP.SoC", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerChpSocImpl extends AbstractOpenemsComponent
		implements ControllerChpSoc, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerChpSocImpl.class);

	@Reference
	private ComponentManager componentManager;

	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress;
	private int lowThreshold = 0;
	private int highThreshold = 0;
	private boolean invertOutput = false;
	private Mode mode;
	/** The current state in the State Machine. */
	private State state = State.UNDEFINED;

	public ControllerChpSocImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerChpSoc.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.lowThreshold = config.lowThreshold();
		this.highThreshold = config.highThreshold();
		if (this.lowThreshold > this.highThreshold) {
			throw new OpenemsException("Low threshold Soc " + this.lowThreshold
					+ " should be less than the high threshold Soc " + this.highThreshold);
		}
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());
		this.mode = config.mode();
		this.channel(ControllerChpSoc.ChannelId.MODE).setNextValue(this.mode);
		this.invertOutput = config.invert();
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		boolean modeChanged;
		do {
			modeChanged = false;
			switch (this.mode) {
			case MANUAL_ON:
				this.setOutput(true);
				modeChanged = this.changeMode(Mode.MANUAL_ON);
				break;
			case MANUAL_OFF:
				this.setOutput(false);
				modeChanged = this.changeMode(Mode.MANUAL_OFF);
				break;
			case AUTOMATIC:
				this.automaticMode();
				modeChanged = this.changeMode(Mode.AUTOMATIC);
				break;
			}
		} while (modeChanged);

		this.channel(ControllerChpSoc.ChannelId.MODE).setNextValue(this.mode);

	}

	private void automaticMode() throws IllegalArgumentException, OpenemsNamedException {
		Channel<?> inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
		int value = TypeUtils.getAsType(OpenemsType.INTEGER, inputChannel.value().getOrError());

		boolean stateChanged;

		do {
			stateChanged = false;
			switch (this.state) {
			case UNDEFINED:
				if (value <= this.lowThreshold) {
					stateChanged = this.changeState(State.ON);
				} else if (value >= this.highThreshold) {
					stateChanged = this.changeState(State.OFF);
				} else {
					stateChanged = this.changeState(State.UNDEFINED);
				}
				break;
			case ON:
				/*
				 * If the value is larger than highThreshold signal OFF
				 */
				if (value >= this.highThreshold) {
					stateChanged = this.changeState(State.OFF);
					break;
				}
				/*
				 * If the value is larger than lowThreshold and smaller than highThreshold, do
				 * not signal anything.
				 */
				if (this.lowThreshold < value && value < this.highThreshold) {
					break; // do nothing
				}
				this.setOutput(true ^ this.invertOutput);
				break;
			case OFF:
				/*
				 * If the value is smaller than lowThreshold signal ON
				 */
				if (value <= this.lowThreshold) {
					stateChanged = this.changeState(State.ON);
					break;
				}
				/*
				 * If the value is larger than lowThreshold and smaller than highThreshold, do
				 * not signal anything.
				 */
				if (this.lowThreshold < value && value < this.highThreshold) {
					break; // do nothing
				}
				this.setOutput(false ^ this.invertOutput);
				break;
			}
		} while (stateChanged); // execute again if the state changed

		// store current state in StateMachine channel
		this.channel(ControllerChpSoc.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	/**
	 * A flag to maintain change in the state.
	 *
	 * @param nextState the target state
	 * @return Flag that the state is changed or not
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			this.state = nextState;
			return true;
		}
		return false;
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value true to switch ON, false to switch OFF;
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void setOutput(Boolean value) throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(this.outputChannelAddress);
			var currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + value + ".");
				outputChannel.setNextWriteValue(value);
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
		}
		return false;
	}
}
