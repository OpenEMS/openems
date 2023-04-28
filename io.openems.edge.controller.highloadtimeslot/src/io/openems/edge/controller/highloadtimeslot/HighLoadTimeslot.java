package io.openems.edge.controller.highloadtimeslot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.HighLoadTimeslot", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class HighLoadTimeslot extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	/**
	 * This many minutes before the high-load timeslot force charging is activated.
	 */
	private static final int FORCE_CHARGE_MINUTES = 30;

	@Reference
	protected ComponentManager componentManager;

	private final Logger log = LoggerFactory.getLogger(HighLoadTimeslot.class);

	private String essId;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private int chargePower;
	private int dischargePower;
	private int hysteresisSoc;
	private WeekdayFilter weekdayDayFilter;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// TODO implement State_Machine channel
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public HighLoadTimeslot() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.essId = config.ess();
		this.startDate = DateUtils.parseLocalDateOrError(config.startDate(), DateUtils.DMY_FORMATTER);
		this.endDate = DateUtils.parseLocalDateOrError(config.endDate(), DateUtils.DMY_FORMATTER);
		// TODO switch format to {@link DateTimeFormatter#ISO_LOCAL_TIME}
		this.startTime = DateUtils.parseLocalTimeOrError(config.startTime(), DateUtils.TIME_FORMATTER);
		this.endTime = DateUtils.parseLocalTimeOrError(config.endTime(), DateUtils.TIME_FORMATTER);
		this.chargePower = config.chargePower();
		this.dischargePower = config.dischargePower();
		this.hysteresisSoc = config.hysteresisSoc();
		this.weekdayDayFilter = config.weekdayFilter();

		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.essId);

		var power = this.getPower(ess);
		this.applyPower(ess, power);
	}

	private ChargeState chargeState = ChargeState.NORMAL;

	/**
	 * Gets the current ActivePower.
	 *
	 * @param ess the {@link ManagedSymmetricEss}
	 * @return the active power value
	 */
	private int getPower(ManagedSymmetricEss ess) {
		var now = LocalDateTime.now(this.componentManager.getClock());
		if (this.isHighLoadTimeslot(now)) {
			/*
			 * We are in a High-Load period -> discharge
			 */
			// reset charge state
			this.chargeState = ChargeState.NORMAL;
			this.logInfo(this.log, "Within High-Load timeslot. Discharge with [" + this.dischargePower + "]");
			return this.dischargePower;
		}
		if (this.isHighLoadTimeslot(now.plusMinutes(FORCE_CHARGE_MINUTES))) {
			/*
			 * We are soon going to be in High-Load period -> activate FORCE_CHARGE mode
			 */
			this.chargeState = ChargeState.FORCE_CHARGE;
		}
		/*
		 * We are in a Charge period
		 */
		switch (this.chargeState) {
		case NORMAL:
			/*
			 * charge with configured charge-power
			 */
			this.logInfo(this.log, "Outside High-Load timeslot. Charge with [" + this.chargePower + "]");
			var minPower = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
			if (minPower >= 0) {
				this.logInfo(this.log, "Min-Power [" + minPower + " >= 0]. Switch to Charge-Hystereses state.");
				// activate Charge-hysteresis if no charge power (i.e. >= 0) is allowed
				this.chargeState = ChargeState.HYSTERESIS;
			}
			return this.chargePower;

		case HYSTERESIS:
			/*
			 * block charging till configured hysteresisSoc
			 */
			this.logInfo(this.log, "Outside High-Load timeslot. Charge-Hysteresis-Mode: Block charging.");
			if (ess.getSoc().orElse(0) <= this.hysteresisSoc) {
				this.logInfo(this.log, "SoC [" + ess.getSoc().orElse(0) + " <= " + this.hysteresisSoc
						+ "]. Switch to Charge-Normal state.");
				this.chargeState = ChargeState.NORMAL;
			}
			return 0;

		case FORCE_CHARGE:
			/*
			 * force full charging just before the high-load timeslot starts
			 */
			this.logInfo(this.log, "Just before High-Load timeslot. Charge with [" + this.chargePower + "]");
			return this.chargePower;
		}
		// we should never come here...
		return 0;
	}

	/**
	 * Is the current time in a high-load timeslot?.
	 *
	 * @param dateTime the current {@link LocalDateTime}
	 * @return true on yes
	 */
	private boolean isHighLoadTimeslot(LocalDateTime dateTime) {
		if (!isActiveWeekday(this.weekdayDayFilter, dateTime)) {
			return false;
		}
		if (!isActiveDate(this.startDate, this.endDate, dateTime)) {
			return false;
		}
		if (!isActiveTime(this.startTime, this.endTime, dateTime)) {
			return false;
		}
		// all tests passed
		return true;
	}

	/**
	 * Is 'dateTime' within the ActiveWeekdayFilter?.
	 *
	 * @param activeDayFilter the {@link WeekdayFilter}
	 * @param dateTime        the current {@link LocalDateTime}
	 * @return true on yes
	 */
	protected static boolean isActiveWeekday(WeekdayFilter activeDayFilter, LocalDateTime dateTime) {
		switch (activeDayFilter) {
		case EVERDAY:
			return true;
		case ONLY_WEEKDAYS:
			return !isWeekend(dateTime);
		case ONLY_WEEKEND:
			return isWeekend(dateTime);
		}
		// should never happen
		return false;
	}

	protected static boolean isActiveDate(LocalDate startDate, LocalDate endDate, LocalDateTime dateTime) {
		var date = dateTime.toLocalDate();
		return !(date.isBefore(startDate) || date.isAfter(endDate));
	}

	/**
	 * Is the time of 'dateTime' within startTime and endTime?.
	 *
	 * @param startTime the configured start time
	 * @param endTime   the configured end time
	 * @param dateTime  the current {@link LocalDateTime}
	 * @return true on yes
	 */
	protected static boolean isActiveTime(LocalTime startTime, LocalTime endTime, LocalDateTime dateTime) {
		var time = dateTime.toLocalTime();
		return !(time.isBefore(startTime) || time.isAfter(endTime));
	}

	/**
	 * Is 'dateTime' a Saturday or Sunday?.
	 *
	 * @param dateTime the current {@link LocalDateTime}
	 * @return true on yes
	 */
	protected static boolean isWeekend(LocalDateTime dateTime) {
		var dayOfWeek = dateTime.getDayOfWeek();
		return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
	}

	/**
	 * Applies the power constraint on the Ess.
	 *
	 * @param ess         the {@link ManagedSymmetricEss}
	 * @param activePower the active power set-point
	 * @throws OpenemsException on error
	 */
	private void applyPower(ManagedSymmetricEss ess, int activePower) throws OpenemsException {
		// adjust value so that it fits into Min/MaxActivePower
		var calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
				activePower);
		if (calculatedPower != activePower) {
			this.logInfo(this.log, "- Applying [" + calculatedPower + " W] instead of [" + activePower + "] W");
		}

		// set result
		ess.addPowerConstraintAndValidate("HighLoadTimeslot P", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
				calculatedPower); //
		ess.addPowerConstraintAndValidate("HighLoadTimeslot Q", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0);
	}

}
