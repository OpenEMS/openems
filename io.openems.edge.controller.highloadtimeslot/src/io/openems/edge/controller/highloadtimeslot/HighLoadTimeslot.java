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

	private final Logger log = LoggerFactory.getLogger(HighLoadTimeslot.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess0;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess1;

	LocalDate startDate;
	LocalDate endDate;
	LocalTime starttime;
	LocalTime endtime;
	int chargePower;
	int dischargePower;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {

		startDate = convert(config.startdate());
		endDate = convert(config.enddate());
		starttime = convertTime(config.starttime());
		endtime = convertTime(config.endtime());
		chargePower = config.chargePower();
		dischargePower = config.dischargePower();

		super.activate(context, config.service_pid(), config.id(), config.enabled());

	}

	@Override
	public void run() {
		
		handle(ess0);
		handle(ess1);
		
	}
	
	private void handle(ManagedSymmetricEss ess) {
		LocalDateTime aktualDate = LocalDateTime.now();
		
		if (isWeekend(aktualDate) || !isInDateSlot(aktualDate, startDate, endDate)) {
			conservationCharge(ess);
		} else if (aktualDate.toLocalTime().isAfter(starttime)) {
			if (aktualDate.toLocalTime().isBefore(endtime)) {
				
				if (isSoCGreater3(ess) == true) {
					discharge(ess);
					
				} else {
					conservationCharge(ess);
				}
			} else {
				conservationCharge(ess);
			}
		} else {
			conservationCharge(ess);
		}
		
	}
	private LocalDate convert(String startdate2) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate localDate = LocalDate.parse(startdate2, dateTimeFormatter);
		return localDate;
	}

	private LocalTime convertTime(String time) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		LocalTime localDate = LocalTime.parse(time, dateTimeFormatter);
		return localDate;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void conservationCharge(ManagedSymmetricEss ess) {
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();

		if (!socOpt.isPresent()) {
			return;
		}

		int soC = socOpt.get();

		if (soC < 95) {
			charge(ess);
		} else {
			conservation(ess);
		}

	}

	private void conservation(ManagedSymmetricEss ess) {
		System.out.println("CONSERVATION");
		try {
			ess.addPowerConstraintAndValidate("HighLoadTimeslot", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0);
		} catch (PowerException e) {
			log.error(e.getMessage());
		}
	}

	private void charge(ManagedSymmetricEss ess) {
		System.out.println("CHARGE");
		try {
			ess.addPowerConstraintAndValidate("HighLoadTimeslot", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
					chargePower);
		} catch (PowerException e) {
			log.error(e.getMessage());
		}
	}

	private void discharge(ManagedSymmetricEss ess) {
		System.out.println("DISCHARGE");
		try {
			ess.addPowerConstraintAndValidate("HighLoadTimeslot", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
					dischargePower);
		} catch (PowerException e) {
			log.error(e.getMessage());
		}
	}


	protected static boolean isInDateSlot(LocalDateTime currentDate, LocalDate startDate, LocalDate endDate) {
		if (currentDate.toLocalDate().isAfter(startDate.minusDays(1)) && currentDate.toLocalDate().isBefore(endDate.plusDays(1))) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isWeekend(LocalDateTime date) {
		LocalDateTime aktualDate = LocalDateTime.now();
		DayOfWeek dayOfWeek = aktualDate.getDayOfWeek();
		
		if(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
			return true;
		}else {
			return false;
		}
	}

	private boolean isSoCGreater3(ManagedSymmetricEss ess) {
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();

		if (!socOpt.isPresent()) {
			return false;
		}

		int soC = socOpt.get();
		if (soC > 3) {
			return true;
		}
		return false;
	}

}
