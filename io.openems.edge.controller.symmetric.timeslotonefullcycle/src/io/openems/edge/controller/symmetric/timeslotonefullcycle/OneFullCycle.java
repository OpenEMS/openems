package io.openems.edge.controller.symmetric.timeslotonefullcycle;

import static java.time.temporal.TemporalAdjusters.firstInMonth;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Calendar;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Timeslot.OneFullCycle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OneFullCycle extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(OneFullCycle.class);
	private final Clock clock;
	private final TemporalAmount hysteresis = Duration.ofMinutes(1);
	private Config config;
	private boolean fullCycleActivated = false;
//	private boolean fullCycleFinished = false;

	private CycleOrder cycleOrder = CycleOrder.UNDEFINED;
	private State state = State.UNDEFINED;
//	private int year, month, day, hour, minute;

	/**
	 * Length of hysteresis. States are not changed quicker than this.
	 */
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

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

	public OneFullCycle(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	public OneFullCycle() {
		this(Clock.systemDefaultZone());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.cycleOrder = config.cycleorder();
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.initializeTime();
		if (!this.fullCycleActivated) {
			return;
		}
		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);

		if (this.state == State.FINISHED) {
			this.log.info("State == FINISHED");
			return;
		}
		try {
			this.initializeEnums(ess);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to initalize Enums: " + e.getMessage());
			return;
		}
		// get max charge/discharge power
		int maxDischargePower = this.ess.getPower().getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		int maxChargePower = this.ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		this.applyPower(ess, maxChargePower, maxDischargePower);
	}

	private void initializeTime() throws OpenemsNamedException {
		Calendar nowCalendar = Calendar.getInstance();
		LocalDate nowDate = LocalDate.now();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, this.config.hour());
		if (this.config.isFixedDayTimeEnabled()) {
			LocalDate firstDay = nowDate.with(firstInMonth(this.config.dayOfWeek()));
			if ((nowDate == firstDay) && (nowCalendar.after(calendar))) {
				this.fullCycleActivated = true;
				return;
			}
		} else {
			LocalDate dayOfMonth = nowDate.withDayOfMonth(this.config.dayOfMonth());
			if ((nowDate == dayOfMonth) && (nowCalendar.after(calendar))) {
				this.fullCycleActivated = true;
				return;
			}
		}
//		else {
//			this.jsonTime(this.config.anyDateTime());
//			LocalDate setDate = LocalDate.of(this.year, this.month, this.day);
//			ChronoLocalDate setNextDate = LocalDate.of(this.year, this.month, this.day + 1);
//			LocalTime setTime = LocalTime.of(this.hour, this.minute);
//			LocalTime nowTime = LocalTime.now();
//			if (nowDate.isEqual(setDate) && nowTime.isAfter(setTime) && nowDate.isBefore(setNextDate)
//					&& this.fullCycleFinished) {
//				this.fullCycleActivated = true;
//				return;
//			}
//		}

		this.fullCycleActivated = false;
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

//	private void jsonTime(String anyDateTime) throws OpenemsNamedException {
//		try {
//			JsonArray time = JsonUtils.getAsJsonArray(JsonUtils.parse(anyDateTime));
//			for (JsonElement element : time) {
//				this.year = JsonUtils.getAsInt(element, "year");
//				this.month = JsonUtils.getAsInt(element, "month");
//				this.day = JsonUtils.getAsInt(element, "day");
//				this.hour = JsonUtils.getAsInt(element, "hour");
//				this.minute = JsonUtils.getAsInt(element, "minute");
//			}
//		} catch (NullPointerException e) {
//			throw new OpenemsException("Unable to find values [" + anyDateTime + "] " + e.getMessage());
//		}
//	}

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
					this.changeState(State.FINISHED);
					break;
				}

			}
			int power = Math.max(maxChargePower, this.config.power() * -1);
			this.logInfo(this.log, "FIRST_CHARGE with [" + power + " W]");
			ess.getSetActivePowerLessOrEqualsChannel().setNextWriteValue(power);
			this.channel(ChannelId.STATE_MACHINE).setNextValue(State.FIRST_CHARGE);
			return;
		}
		case FIRST_DISCHARGE: {
			/*
			 * Discharge till empty
			 */
			if (maxDischargePower == 0) {
				switch (this.cycleOrder) {
				case START_WITH_CHARGE:
					this.changeState(State.FINISHED);
					break;
				case START_WITH_DISCHARGE:
				case UNDEFINED:
					this.changeState(State.FIRST_CHARGE);
				}

			}
			int power = Math.min(maxDischargePower, this.config.power());
			this.logInfo(this.log, "FIRST_DISCHARGE with [" + power + " W]");
			ess.getSetActivePowerGreaterOrEqualsChannel().setNextWriteValue(power);
			this.channel(ChannelId.STATE_MACHINE).setNextValue(State.FIRST_DISCHARGE);
			return;
		}
		case SECOND_CHARGE: {
			/*
			 * Charge till full
			 */
			if (maxChargePower == 0) {
				this.changeState(State.FINISHED);
			}
			int power = Math.max(maxChargePower, this.config.power() * -1);
			this.logInfo(this.log, "SECOND_CHARGE with [" + power + " W]");
			ess.getSetActivePowerLessOrEqualsChannel().setNextWriteValue(power);
			this.channel(ChannelId.STATE_MACHINE).setNextValue(State.SECOND_CHARGE);
			return;
		}
		case SECOND_DISCHARGE: {
			/*
			 * Discharge till empty
			 */
			if (maxDischargePower == 0) {
				this.changeState(State.FINISHED);
			}
			int power = Math.min(maxDischargePower, this.config.power());
			this.logInfo(this.log, "SECOND_DISCHARGE with [" + power + " W]");
			ess.getSetActivePowerGreaterOrEqualsChannel().setNextWriteValue(power);
			this.channel(ChannelId.STATE_MACHINE).setNextValue(State.SECOND_DISCHARGE);
			return;
		}
		case FINISHED:
		default:
			/*
			 * Nothing to do
			 */
			ess.getSetActivePowerGreaterOrEqualsChannel().deactivate();
//			this.fullCycleFinished = true;
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
