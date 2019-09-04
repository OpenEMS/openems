package io.openems.edge.controller.HeatingElementController;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Heizstab", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerHeatingElement  extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerHeatingElement.class);

	private final Clock clock;

	// private Config config;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
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

	public ControllerHeatingElement(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	public ControllerHeatingElement() {
		this(Clock.systemDefaultZone());
	}

	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 */
	private final TemporalAmount hysteresis = Duration.ofMinutes(5);
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress1;
	private ChannelAddress outputChannelAddress2;
	private ChannelAddress outputChannelAddress3;
	private int powerOfPhase = 0;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.powerOfPhase = config.powerOfPhase();
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		this.outputChannelAddress1 = ChannelAddress.fromString(config.outputChannelAddress1());
		this.outputChannelAddress2 = ChannelAddress.fromString(config.outputChannelAddres2());
		this.outputChannelAddress3 = ChannelAddress.fromString(config.outputChannelAddres3());

		super.activate(context, config.id(), config.alias(), config.enabled());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		Channel<?> inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
		int gridActivePower = TypeUtils.getAsType(OpenemsType.INTEGER, inputChannel.value().getOrError());

		// Soc = this.sum.getEssSoc().value().orElse(0);
		 int Soc = 99;
		//int heatingPower = 0;
		// this.powerOfPhase = 2000

		boolean stateChanged;

		do {
			stateChanged = false;
			if (Soc <= 92) {
				stateChanged = this.changeState(State.UNDEFINED);
			} else if (Soc > 92 && Soc <= 94) {
				stateChanged = this.changeState(State.FIRST_PHASE);
			} else if (Soc > 94 && Soc <= 96) {
				stateChanged = this.changeState(State.SECOND_PHASE);
			} else {
				stateChanged = this.changeState(State.THIRD_PHASE);
			}

			switch (this.state) {
			case UNDEFINED:
				if (gridActivePower == 0) {
					this.off(outputChannelAddress1);
					this.off(outputChannelAddress2);
					this.off(outputChannelAddress3);
				}
				break;
			case FIRST_PHASE:
				if (gridActivePower == this.powerOfPhase) {
					this.on(outputChannelAddress1);
					this.off(outputChannelAddress2);
					this.off(outputChannelAddress3);
				}
				break;
			case SECOND_PHASE:
				if (gridActivePower == this.powerOfPhase * 2) {
					this.on(outputChannelAddress2);
					this.on(outputChannelAddress1);
					this.off(outputChannelAddress3);
				}
				break;
			case THIRD_PHASE:
				if (gridActivePower == this.powerOfPhase * 3) {
					this.on(outputChannelAddress2);
					this.on(outputChannelAddress1);
					this.on(outputChannelAddress3);
				}
				break;
			}
		} while (stateChanged); // execute again if the state changed

		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
	}


	/**
	 * Switch the output ON.
	 * 
	 * @param outputChannelAddress address of the channel which must set to ON
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void on(ChannelAddress outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(true, outputChannelAddress);
	}

	/**
	 * Switch the output OFF.
	 * 
	 * @param outputChannelAddress address of the channel which must set to OFF
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void off(ChannelAddress outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(false, outputChannelAddress);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value                The boolean value which must set on the output
	 *                             channel address
	 * @param outputChannelAddress The address of the channel
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void setOutput(boolean value, ChannelAddress outputChannelAddress)
			throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(outputChannelAddress);
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + (value) + ".");
				outputChannel.setNextWriteValue(value);
			}
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set output: [" + outputChannelAddress + "] " + e.getMessage());
		}
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 * 
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		System.out.println("FROM : " + this.state + " TO : " + nextState);
		System.out.println("local date time : " + LocalDateTime.now(this.clock).toLocalTime());
		System.out.println("added time: " + this.lastStateChange.plus(this.hysteresis).toLocalTime());
		System.out.println("added_time is isAfter is now_time : "
				+ this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock)));

		if (this.state != nextState) {

			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
				System.out.println("Not awaiting fr hysteris");
				return true;
			} else {
				System.out.println("awaiting hysteris");
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
				return false;
			}
		} else {
			System.out.println("Keine hysteris");
			this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			return false;
		}
	}

}

//do {
//stateChanged = false;
//switch (this.state) {
//case UNDEFINED:
//	stateChanged = handleUndefined(production, this.outputChannelAddress1, this.outputChannelAddress2,
//			this.outputChannelAddress3);
//	break;
//case FIRST_PHASE:
//	stateChanged = handleFirstPhase(production, this.outputChannelAddress1, this.outputChannelAddress2,
//			this.outputChannelAddress3);
//	break;
//case SECOND_PHASE:
//	stateChanged = handleSecondPhase(production, this.outputChannelAddress1, this.outputChannelAddress2,
//			this.outputChannelAddress3);
//	break;
//case THIRD_PHASE:
//	stateChanged = handleThirdPhase(production, this.outputChannelAddress1, this.outputChannelAddress2,
//			this.outputChannelAddress3);
//	break;
//}
//} while (stateChanged); // execute again if the state changed
//
//// store current state in StateMachine channel
//this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);


//private Boolean handleUndefined(int production, ChannelAddress outputChannelAddress1,
//ChannelAddress outputChannelAddress2, ChannelAddress outputChannelAddress3)
//throws IllegalArgumentException, OpenemsNamedException {
//
//// If the production power is greater than the configured power
//if (production > this.powerOfPhase) {
//return this.changeState(State.FIRST_PHASE);
//} // If the state did not change - do the operation defined for undefined state
//else {
//this.off(outputChannelAddress1);
//this.off(outputChannelAddress2);
//this.off(outputChannelAddress3);
//return false;
//}
//}
//
//private boolean handleFirstPhase(int production, ChannelAddress outputChannelAddress1,
//ChannelAddress outputChannelAddress2, ChannelAddress outputChannelAddress3)
//throws IllegalArgumentException, OpenemsNamedException {
//System.out.println(production);
//Boolean stateChanged = false;
//
//if (production > this.powerOfPhase * 2) {
//stateChanged = this.changeState(State.SECOND_PHASE);
//} else if (production <= this.powerOfPhase) {
//stateChanged = this.changeState(State.UNDEFINED);
//} // if State not changed - do the operation of the first phase
//else if (!stateChanged) {
//this.on(outputChannelAddress1);
//this.off(outputChannelAddress2);
//this.off(outputChannelAddress3);
//}
//return stateChanged = false;
//
//}
//
//private boolean handleSecondPhase(int production, ChannelAddress outputChannelAddress1,
//ChannelAddress outputChannelAddress2, ChannelAddress outputChannelAddress3)
//throws IllegalArgumentException, OpenemsNamedException {
//System.out.println(production);
//Boolean stateChanged = false;
//if (production > this.powerOfPhase * 3) {
//return this.changeState(State.THIRD_PHASE);
//} else if (production <= this.powerOfPhase * 2) {
//return this.changeState(State.FIRST_PHASE);
//} else if (!stateChanged) {
//this.on(outputChannelAddress2);
//this.on(outputChannelAddress1);
//this.off(outputChannelAddress3);
//}
//return false;
//}
//
//private boolean handleThirdPhase(int production, ChannelAddress outputChannelAddress1,
//ChannelAddress outputChannelAddress2, ChannelAddress outputChannelAddress3)
//throws IllegalArgumentException, OpenemsNamedException {
//System.out.println(production);
//Boolean stateChanged = false;
//if (production < this.powerOfPhase * 3) {
//return this.changeState(State.SECOND_PHASE);
//} else if (!stateChanged) {
//this.on(outputChannelAddress2);
//this.on(outputChannelAddress1);
//this.on(outputChannelAddress3);
//}
//return false;
//}