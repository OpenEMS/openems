package io.openems.edge.controller.ess.onefullcycle;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.OneFullCycle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EssOneFullCycle extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssOneFullCycle.class);
	private final Clock clock;
	private final TemporalAmount hysteresis = Duration.ofMinutes(30);

	private int power;
	private String essId;
	private State state = State.UNDEFINED;
	private CycleOrder cycleOrder = CycleOrder.UNDEFINED;

	/**
	 * Length of hysteresis. States are not changed quicker than this.
	 */
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	@Reference
	protected ComponentManager componentManager;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		CYCLE_ORDER(Doc.of(CycleOrder.values()) //
				.text("First Charge or First Discharge")), //
		AWAITING_HYSTERESIS(Doc.of(State.values()) //
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

	public EssOneFullCycle(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	public EssOneFullCycle() {
		this(Clock.systemDefaultZone());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.power = Math.abs(config.power());
		this.essId = config.ess_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);

		ManagedSymmetricEss ess = this.componentManager.getComponent(essId);

		if (this.state == State.FINISHED) {
			return;
		}

		try {
			this.initializeEnums(ess);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to initalize Enums: " + e.getMessage());
			return;
		}

		// get max charge/discharge power
		int maxDischargePower = ess.getPower().getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		int maxChargePower = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);

		this.applyPower(ess, maxChargePower, maxDischargePower);
	}

	private void initializeEnums(ManagedSymmetricEss ess) throws InvalidValueException {
		/*
		 * set Cycle-Order
		 */
		if (this.cycleOrder.isUndefined()) {
			int soc = ess.getSoc().value().getOrError();
			if (soc < 50) {
				this.cycleOrder = CycleOrder.START_WITH_DISCHARGE;
			} else {
				this.cycleOrder = CycleOrder.START_WITH_CHARGE;
			}
			this.channel(ChannelId.CYCLE_ORDER).setNextValue(this.cycleOrder);
		}

		/*
		 * set initial State
		 */
		if (this.state == State.UNDEFINED) {
			switch (this.cycleOrder) {
			case START_WITH_CHARGE:
				this.state = State.FIRST_CHARGE;
				break;
			case START_WITH_DISCHARGE:
			case UNDEFINED:
				this.state = State.FIRST_DISCHARGE;
				break;
			}
		}
	}

	private void applyPower(ManagedSymmetricEss ess, int maxChargePower, int maxDischargePower)
			throws OpenemsNamedException {
		switch (this.state) {
		case FIRST_CHARGE: {
			/*
			 * Charge till full
			 */
			if (maxChargePower == 0) {
				switch (this.cycleOrder) {
				case START_WITH_CHARGE:
					this.changeState(State.FIRST_DISCHARGE);
					break;
				case START_WITH_DISCHARGE:
				case UNDEFINED:
					this.changeState(State.SECOND_DISCHARGE);
					break;
				}

			}
			int power = Math.max(maxChargePower, this.power * -1);
			this.logInfo(this.log, "FIRST_CHARGE with [" + power + " W]");
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(power);
			return;
		}
		case FIRST_DISCHARGE: {
			/*
			 * Discharge till empty
			 */
			if (maxDischargePower == 0) {
				switch (this.cycleOrder) {
				case START_WITH_CHARGE:
					this.changeState(State.SECOND_CHARGE);
					break;
				case START_WITH_DISCHARGE:
				case UNDEFINED:
					this.changeState(State.FIRST_CHARGE);
				}

			}
			int power = Math.min(maxDischargePower, this.power);
			this.logInfo(this.log, "FIRST_DISCHARGE with [" + power + " W]");
			ess.getSetActivePowerGreaterOrEquals().setNextWriteValue(power);
			return;
		}
		case SECOND_CHARGE: {
			/*
			 * Charge till full
			 */
			if (maxChargePower == 0) {
				this.changeState(State.FINISHED);
			}
			int power = Math.max(maxChargePower, this.power * -1);
			this.logInfo(this.log, "SECOND_CHARGE with [" + power + " W]");
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(power);
			return;
		}
		case SECOND_DISCHARGE: {
			/*
			 * Discharge till empty
			 */
			if (maxDischargePower == 0) {
				this.changeState(State.FINISHED);
			}
			int power = Math.min(maxDischargePower, this.power);
			this.logInfo(this.log, "SECOND_DISCHARGE with [" + power + " W]");
			ess.getSetActivePowerGreaterOrEquals().setNextWriteValue(power);
			return;
		}
		case FINISHED:
		default:
			/*
			 * Nothing to do
			 */
			return;
		}
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 * 
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
				return true;
			} else {
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
				this.logInfo(this.log,
						"Awaiting hysteresis for changing from [" + this.state + "] to [" + nextState + "]");
				return false;
			}
		} else {
			this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			return false;
		}
	}
}
