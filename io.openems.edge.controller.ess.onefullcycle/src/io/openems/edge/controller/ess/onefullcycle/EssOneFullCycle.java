package io.openems.edge.controller.ess.onefullcycle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;

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
	private final TemporalAmount hysteresis = Duration.ofMinutes(30);

	private State state = State.UNDEFINED;
	private CycleOrder cycleOrder = CycleOrder.UNDEFINED;

	/**
	 * Length of hysteresis. States are not changed quicker than this.
	 */
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	/**
	 * Keeps the currently applied power to have it available in {@link #debugLog()}
	 * method.
	 */
	private Integer currentPower;

	@Reference
	protected ComponentManager componentManager;

	private Config config;

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

	public EssOneFullCycle() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Modified
	void modified(Config config) throws OpenemsNamedException {
		this.config = config;
	}

	@Override
	public void run() throws OpenemsNamedException {
		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);

		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

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
			int soc = ess.getSoc().getOrError();
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
		final Integer power;

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
			power = Math.max(maxChargePower, this.config.power() * -1);
			ess.setActivePowerLessOrEquals(power);
			break;
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
			power = Math.min(maxDischargePower, this.config.power());
			ess.setActivePowerGreaterOrEquals(power);
			break;
		}
		case SECOND_CHARGE: {
			/*
			 * Charge till full
			 */
			if (maxChargePower == 0) {
				this.changeState(State.FINISHED);
			}
			power = Math.max(maxChargePower, this.config.power() * -1);
			ess.setActivePowerLessOrEquals(power);
			break;
		}
		case SECOND_DISCHARGE: {
			/*
			 * Discharge till empty
			 */
			if (maxDischargePower == 0) {
				this.changeState(State.FINISHED);
			}
			power = Math.min(maxDischargePower, this.config.power());
			ess.setActivePowerGreaterOrEquals(power);
			break;
		}
		case FINISHED:
		default:
			/*
			 * Nothing to do
			 */
			power = 0;
			break;
		}

		this.currentPower = power;
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 * 
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			LocalDateTime now = LocalDateTime.now(this.componentManager.getClock());
			if (this.lastStateChange.plus(this.hysteresis).isBefore(now)) {
				this.state = nextState;
				this.lastStateChange = now;
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

	@Override
	public String debugLog() {
		return "State:" + this.state + ((this.currentPower != null) ? "|L:" + this.currentPower + " W" : "");
	}
}
