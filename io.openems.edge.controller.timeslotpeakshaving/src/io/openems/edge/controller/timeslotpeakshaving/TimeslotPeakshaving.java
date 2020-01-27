package io.openems.edge.controller.timeslotpeakshaving;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.TimeslotPeakshaving", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TimeslotPeakshaving extends AbstractOpenemsComponent implements Controller, OpenemsComponent {
	
	public static final String TIME_FORMAT = "HH:mm";
	public static final String DATE_FORMAT = "dd.MM.yyyy";

	@Reference
	protected ComponentManager componentManager;
	
	@Reference
	protected Power power;
	
	private Config config;
	
	private PidFilter pidFilter;

	private final Logger log = LoggerFactory.getLogger(TimeslotPeakshaving.class);
	private final Clock clock;

	private LocalDate startDate;
	private LocalDate endDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private LocalTime slowStartTime;
	private int peakShavingPower;
	private int rechargePower;
	private int chargePower;
	private int hysteresisSoc;
	//private WeekdayFilter weekdayDayFilter;
	private int forceChargeMinutes;
	
	boolean isInTimeslot = false;
	boolean isInBeforeTimeslot = false;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_STATE(Doc.of(ChargeState.values()) //
				.text("Current State of charging")), //
		PEAKSHAVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		RECHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)), //
		PID_OUTPUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTUAL_POWER_CHARGING(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT));
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	public TimeslotPeakshaving() {
		this(Clock.systemDefaultZone());
	}

	protected TimeslotPeakshaving(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	
	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {		
		this.startDate = convertDate(config.startDate());
		this.endDate = convertDate(config.endDate());
		this.startTime = convertTime(config.startTime());
		this.endTime = convertTime(config.endTime());
		this.slowStartTime = convertTime(config.slowStartTime());
		this.peakShavingPower = config.peakShavingPower();
		this.channel(ChannelId.PEAKSHAVE_POWER).setNextValue(this.peakShavingPower);
		this.rechargePower = config.rechargePower();
		this.channel(ChannelId.RECHARGE_POWER).setNextValue(this.rechargePower);
		this.chargePower = config.chargePower();
		this.channel(ChannelId.CHARGE_POWER).setNextValue(this.chargePower);
		this.hysteresisSoc = config.hysteresisSoc();
//		this.weekdayDayFilter = config.weekdayFilter();		
		this.forceChargeMinutes = calculateTimeforForceCharge(this.slowStartTime,  this.startTime);
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.pidFilter = this.power.buildPidFilter();
	}

	private int calculateTimeforForceCharge(LocalTime slowStartTime, LocalTime startTime) {
		int forceChargeTime = (int) ChronoUnit.MINUTES.between(slowStartTime, startTime);
		if (forceChargeTime > 0) {
			return forceChargeTime;
		} else {
			return forceChargeTime + 1440; // 1440 - total minutes in a day
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private ChargeState chargeState = ChargeState.NORMAL;
	
	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

		int power = getPower(ess, meter);
		
		this.applyPower(ess, power);		
		this.channel(ChannelId.CHARGE_STATE).setNextValue(this.chargeState);		
	}
	
	/**
	 * Applies the power on the Ess
	 * 
	 * @param ess managedSymmetricEss where the power needs to be set
	 * @param pidOuput the power to be set on ess
	 * @throws OpenemsNamedException 
	 */
	private void applyPower(ManagedSymmetricEss ess, int activePower) throws OpenemsNamedException {
		/* 
		 * if the controller is in the timeslot and just before the time slot
		 * set the active power
		 */
		if(isInTimeslot || isInBeforeTimeslot) {	
			ess.getSetActivePowerEquals().setNextWriteValue(activePower);
			ess.getSetReactivePowerEquals().setNextWriteValue(0);
			this.channel(ChannelId.ACTUAL_POWER_CHARGING).setNextValue(activePower);
			isInTimeslot = false;
			isInBeforeTimeslot = false;
		}else {
			this.channel(ChannelId.ACTUAL_POWER_CHARGING).setNextValue(null);
		}
	}
	
	/**
	 * Gets the current ActivePower.
	 * 
	 * @return
	 */
	private int getPower(ManagedSymmetricEss ess, SymmetricMeter meter) {		
		LocalDateTime now = LocalDateTime.now(this.clock);
		int activePower = this.peakShave(ess, meter);
		if (this.isHighLoadTimeslot(now)) {
			/*
			 * We are in a High-Load period -> peak shave and discharge/ charge
			 */
			// hysterisis soc within highloadtimeslot
			if (ess.getSoc().value().orElse(0) >= this.hysteresisSoc) {
				this.logInfo(log, "SoC [" + ess.getSoc().value().orElse(0) + " >= " + this.hysteresisSoc
						+ "]. Switch to Charge-Normal state.");
				this.chargeState = ChargeState.HYSTERESIS;
				return 0;
			}			
			this.channel(ChannelId.PID_OUTPUT).setNextValue(activePower);
			this.chargeState = ChargeState.NORMAL;
			isInTimeslot = true;
			this.logInfo(log, "Within High-Load timeslot. charge with [" + activePower + "]");			
			return activePower;			
		} else if (this.isHighLoadTimeslot(now.plusMinutes(this.forceChargeMinutes))) {
			/*
			 * We are soon going to be in High-Load period -> activate FORCE_CHARGE mode
			 */
			this.logInfo(log, " We are soon going to be in High-Load period ");	
			this.channel(ChannelId.PID_OUTPUT).setNextValue(0);
			isInBeforeTimeslot = true;
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
			this.logInfo(log, "Outside High-Load timeslot. Charge with [" + this.chargePower + "]");
			int minPower = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
			if (minPower >= 0) {
				this.logInfo(log, "Min-Power [" + minPower + " >= 0]. Switch to Charge-Hystereses state.");
				// activate Charge-hysteresis if no charge power (i.e. >= 0) is allowed
				this.chargeState = ChargeState.HYSTERESIS;
			}
			return activePower;
		case HYSTERESIS:
			/*
			 * block charging till configured hysteresisSoc
			 */
			this.logInfo(log, "Outside High-Load timeslot. Charge-Hysteresis-Mode: Block charging.");
			if (ess.getSoc().value().orElse(0) <= this.hysteresisSoc) {
				this.logInfo(log, "SoC [" + ess.getSoc().value().orElse(0) + " <= " + this.hysteresisSoc
						+ "]. Switch to Charge-Normal state.");
				this.chargeState = ChargeState.NORMAL;
			}
			return 0;
		case FORCE_CHARGE:
			/*
			 * force full charging just before the high-load timeslot starts
			 */
			this.logInfo(log, "Just before High-Load timeslot. Charge with [" + this.chargePower + "]");
			return this.chargePower;
		}
		// we should never come here...
		return 0;
	}

	private int peakShave(ManagedSymmetricEss ess, SymmetricMeter meter) {
		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = ess.getGridMode().value().asEnum();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		if (gridMode != GridMode.ON_GRID) {
			return 0;
		}

		// Calculate 'real' grid-power (without current ESS charge/discharge)
		int gridPower = meter.getActivePower().value().orElse(0) /* current buy-from/sell-to grid */
				+ ess.getActivePower().value().orElse(0) /* current charge/discharge Ess */;

		int calculatedPower;
		if (gridPower >= this.peakShavingPower) {	
			/*
			 * Peak-Shaving
			 */
			calculatedPower = gridPower -= this.peakShavingPower;

		} else if (gridPower <= this.rechargePower) {
			/*
			 * Recharge
			 */
			calculatedPower = gridPower -= this.rechargePower;

		} else {
			/*
			 * Do nothing
			 */
			calculatedPower = 0;
		}

		/*
		 * Apply PID filter
		 */
		int minPower = this.power.getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		int maxPower = this.power.getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		this.pidFilter.setLimits(minPower, maxPower);
		int pidOutput = (int) this.pidFilter.applyPidFilter(ess.getActivePower().value().orElse(0), calculatedPower);
		return pidOutput;
	}

	/**
	 * Is the current time in a high-load timeslot?
	 * 
	 * @return
	 */
	private boolean isHighLoadTimeslot(LocalDateTime dateTime) {
		if(!isConfiguredActiveDay(this.config, dateTime)) {
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
	
	private static boolean isConfiguredActiveDay(Config config, LocalDateTime dateTime) {

		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		boolean configuredDay = false;

		switch (day) {
		case Calendar.SUNDAY:
			if (config.sunday()) {
				configuredDay = true;
			}
			break;
		case Calendar.MONDAY:
			if (config.monday()) {
				configuredDay = true;
			}
			break;
		case Calendar.TUESDAY:
			if (config.tuesday()) {
				configuredDay = true;
			}
			break;
		case Calendar.WEDNESDAY:
			if (config.wednesday()) {
				configuredDay = true;
			}
			break;
		case Calendar.THURSDAY:
			if (config.thursday()) {
				configuredDay = true;
			}
			break;
		case Calendar.FRIDAY:
			if (config.friday()) {
				configuredDay = true;
			}
			break;
		case Calendar.SATURDAY:
			if (config.saturday()) {
				configuredDay = true;
			}
			break;
		}
		return configuredDay;
	}
	
	/**
	 * Is 'dateTime' within the ActiveWeekdayFilter?
	 * 
	 * @param activeDayFilter
	 * @param dateTime
	 * @return
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
		LocalDate date = dateTime.toLocalDate();
		return !(date.isBefore(startDate) || date.isAfter(endDate));
	}

	/**
	 * Is the time of 'dateTime' within startTime and endTime?
	 * 
	 * @param startTime
	 * @param endTime
	 * @param dateTime
	 * @return
	 */
	protected static boolean isActiveTime(LocalTime startTime, LocalTime endTime, LocalDateTime dateTime) {
		LocalTime time = dateTime.toLocalTime();
		return !(time.isBefore(startTime) || time.isAfter(endTime));
	}

	/**
	 * Is 'dateTime' a Saturday or Sunday?
	 * 
	 * @param dateTime
	 * @return
	 */
	protected static boolean isWeekend(LocalDateTime dateTime) {
		DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
		return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
	}
	
	/**
	 * Converts a string to a LocalDate.
	 * 
	 * @param date
	 * @return
	 */
	protected static LocalDate convertDate(String date) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
		return localDate;
	}
	
	/**
	 * Converts a string to a LocalTime.
	 * 
	 * @param time
	 * @return
	 */
	protected static LocalTime convertTime(String time) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
		LocalTime localDate = LocalTime.parse(time, dateTimeFormatter);
		return localDate;
	}

}