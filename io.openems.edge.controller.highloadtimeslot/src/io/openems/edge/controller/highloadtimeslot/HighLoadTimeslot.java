package io.openems.edge.controller.highloadtimeslot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.HighLoadTimeslot", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HighLoadTimeslot extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public static final String TIME_FORMAT = "HH:mm";
	public static final String DATE_FORMAT = "dd.MM.yyyy";

	private final Logger log = LoggerFactory.getLogger(HighLoadTimeslot.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	LocalDate startDate;
	LocalDate endDate;
	LocalTime starttime;
	LocalTime endtime;
	int chargePower;
	int dischargePower;
	int minSoc;
	int hysteresisSoc;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {

		startDate = convertDate(config.startdate());
		endDate = convertDate(config.enddate());
		starttime = convertTime(config.starttime());
		endtime = convertTime(config.endtime());
		chargePower = config.chargePower();
		dischargePower = config.dischargePower();
		minSoc = config.minSoc();
		hysteresisSoc = config.hysteresisSoc();

		super.activate(context, config.service_pid(), config.id(), config.enabled());

	}

	@Override
	public void run() {

		handle(ess, LocalDateTime.now());

	}

	private void handle(ManagedSymmetricEss ess, LocalDateTime dateTime) {

		if (isWeekend(dateTime) || !isInDateSlot(dateTime, startDate, endDate)) {
			conservationCharge(ess);
		} else if (isInTimeSlot(dateTime, starttime, endtime) && isSoCGreaterMinSoC(ess, minSoc)) {
			discharge(ess);
		} else {
			conservationCharge(ess);
		}
	}

	protected static LocalDate convertDate(String date) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
		return localDate;
	}

	protected static LocalTime convertTime(String time) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
		LocalTime localDate = LocalTime.parse(time, dateTimeFormatter);
		return localDate;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void conservationCharge(ManagedSymmetricEss ess) {
		// TODO hysteresis 
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();

		if (!socOpt.isPresent()) {
			return;
		}

		int soC = socOpt.get();

		if (soC < hysteresisSoc) {
			charge(ess);
		} else {
			conservation(ess);
		}

	}

	private void conservation(ManagedSymmetricEss ess) {
		log.info("HighLoadTimeslot.conservation()");
		try {
			ess.addPowerConstraintAndValidate("HighLoadTimeslot", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0);
		} catch (PowerException e) {
			log.error(e.getMessage());
		}
	}

	private void charge(ManagedSymmetricEss ess) {
		log.info("HighLoadTimeslot.charge()");
		try {
			ess.addPowerConstraintAndValidate("HighLoadTimeslot", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
					chargePower);
		} catch (PowerException e) {
			log.error(e.getMessage());
		}
	}

	private void discharge(ManagedSymmetricEss ess) {
		log.info("HighLoadTimeslot.discharge()");
		try {
			ess.addPowerConstraintAndValidate("HighLoadTimeslot", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
					dischargePower);
		} catch (PowerException e) {
			log.error(e.getMessage());
		}
	}

	protected static boolean isInDateSlot(LocalDateTime currentDate, LocalDate startDate, LocalDate endDate) {
		return (currentDate.toLocalDate().isAfter(startDate.minusDays(1))
				&& currentDate.toLocalDate().isBefore(endDate.plusDays(1)));
	}
	
	protected static boolean isInTimeSlot(LocalDateTime currentTime, LocalTime starttime, LocalTime endtime) {
		return currentTime.toLocalTime().isAfter(starttime) && currentTime.toLocalTime().isBefore(endtime);
	}

	protected static boolean isWeekend(LocalDateTime date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();

		return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY); 
	}

	private boolean isSoCGreaterMinSoC(ManagedSymmetricEss ess, int minSoc) {
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();

		if (!socOpt.isPresent()) {
			return false;
		}

		int soC = socOpt.get();

		return soC > minSoc;
	}

}
