package io.openems.edge.controller.channelthreshold;

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
		name = "Controller.ChannelThreshold", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ChannelThresholdImpl extends AbstractOpenemsComponent
		implements ChannelThreshold, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ChannelThresholdImpl.class);

	@Reference
	private ComponentManager componentManager;

	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress;
	private int lowThreshold = 0;
	private int highThreshold = 0;
	private int hysteresis = 0;
	private boolean invertOutput = false;

	/** The current state in the State Machine. */
	private State state = State.UNDEFINED;
	/** Should the hysteresis be applied on passing high threshold?. */
	private boolean applyHighHysteresis = true;
	/** Should the hysteresis be applied on passing low threshold?. */
	private boolean applyLowHysteresis = true;

	public ChannelThresholdImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelThreshold.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		/*
		 * parse config
		 */
		this.lowThreshold = config.lowThreshold();
		this.highThreshold = config.highThreshold();
		this.hysteresis = config.hysteresis();
		this.invertOutput = config.invert();
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());

		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		/*
		 * Check if all parameters are available
		 */
		int value;
		try {
			Channel<?> inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
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
			if (value < this.lowThreshold) {
				this.state = State.BELOW_LOW;
			} else if (value > this.highThreshold) {
				this.state = State.ABOVE_HIGH;
			} else {
				this.state = State.BETWEEN_LOW_AND_HIGH;
			}
			break;

		case BELOW_LOW:
			/*
			 * Value is smaller than the low threshold -> always OFF
			 */
			if (value >= this.lowThreshold) {
				this.state = State.PASS_LOW_COMING_FROM_BELOW;
				break;
			}

			this.off();
			break;

		case PASS_LOW_COMING_FROM_BELOW:
			/*
			 * Value just passed the low threshold coming from below -> turn ON
			 */
			this.on();
			this.applyLowHysteresis = true;
			this.state = State.BETWEEN_LOW_AND_HIGH;
			break;

		case BETWEEN_LOW_AND_HIGH:
			/*
			 * Value is between low and high threshold -> always ON
			 */
			// evaluate if hysteresis is necessary
			if (value >= this.lowThreshold + this.hysteresis) {
				this.applyLowHysteresis = false; // do not apply low hysteresis anymore
			}
			if (value <= this.highThreshold - this.hysteresis) {
				this.applyHighHysteresis = false; // do not apply high hysteresis anymore
			}

			/*
			 * Check LOW threshold
			 */
			if (this.applyLowHysteresis) {
				if (value <= this.lowThreshold - this.hysteresis) {
					// pass low with hysteresis
					this.state = State.PASS_LOW_COMING_FROM_ABOVE;
					break;
				}
			} else {
				if (value <= this.lowThreshold) {
					// pass low, not applying hysteresis
					this.state = State.PASS_LOW_COMING_FROM_ABOVE;
					break;
				}
			}

			/*
			 * Check HIGH threshold
			 */
			if (this.applyHighHysteresis) {
				if (value >= this.highThreshold + this.hysteresis) {
					// pass high with hysteresis
					this.state = State.PASS_HIGH_COMING_FROM_BELOW;
					break;
				}
			} else {
				if (value >= this.highThreshold) {
					// pass high, not applying hysteresis
					this.state = State.PASS_HIGH_COMING_FROM_BELOW;
					break;
				}
			}

			// Default: not switching the State -> always ON
			this.on();
			break;

		case PASS_HIGH_COMING_FROM_BELOW:
			/*
			 * Value just passed the high threshold coming from below -> turn OFF
			 */
			this.off();
			this.state = State.ABOVE_HIGH;
			break;

		case PASS_LOW_COMING_FROM_ABOVE:
			/*
			 * Value just passed the low threshold from above -> turn OFF
			 */
			this.off();
			this.state = State.BELOW_LOW;
			break;

		case PASS_HIGH_COMING_FROM_ABOVE:
			/*
			 * Value just passed the high threshold coming from above -> turn ON
			 */
			this.on();
			this.applyHighHysteresis = true;
			this.state = State.BETWEEN_LOW_AND_HIGH;
			break;

		case ABOVE_HIGH:
			/*
			 * Value is bigger than the high threshold -> always OFF
			 */
			if (value <= this.highThreshold) {
				this.state = State.PASS_HIGH_COMING_FROM_ABOVE;
			}

			this.off();
			break;
		}
	}

	/**
	 * Switch the output ON.
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void on() throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(true);
	}

	/**
	 * Switch the output OFF.
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
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
			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(this.outputChannelAddress);
			var currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != (value ^ this.invertOutput)) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] "
						+ (value ^ this.invertOutput ? "ON" : "OFF") + ".");
				outputChannel.setNextWriteValue(value ^ this.invertOutput);
			}
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set output: [" + this.outputChannelAddress + "] " + e.getMessage());
		}
	}
}
