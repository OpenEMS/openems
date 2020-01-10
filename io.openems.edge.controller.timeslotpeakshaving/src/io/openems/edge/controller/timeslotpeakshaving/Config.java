package io.openems.edge.controller.timeslotpeakshaving;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Timeslot Peakshaving", //
		description = "This controller Peak shaves during the high threshold configured time-slot, and charges the battery outside the timeslot.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlTimeslotPeakshaving0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess();
	
	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Startdate", description = "for example: 30.12.1998")
	String startDate();

	@AttributeDefinition(name = "Enddate", description = "for example: 31.12.1998")
	String endDate();

	@AttributeDefinition(name = "Daily Starttime", description = "for example: 06:46")
	String startTime();

	@AttributeDefinition(name = "Daily Endtime", description = "for example: 17:05")
	String endTime();
	
	@AttributeDefinition(name = "Slow start Time", description = "The time to slow charging of the battery within timeslot and not within highthreshold time")
	String slowStartTime();

	@AttributeDefinition(name = "Which days?", description = "On which days should the algorithm run?")
	WeekdayFilter weekdayFilter() default WeekdayFilter.EVERDAY;
	
	@AttributeDefinition(name = "Peak-Shaving power", description = "Grid purchase power above this value is considered a peak and shaved to this value.")
	int peakShavingPower();

	@AttributeDefinition(name = "Recharge power", description = "If grid purchase power is below this value battery is recharged.")
	int rechargePower();	

	@AttributeDefinition(name = "Charge Power", description = "Charge power per ess in Watt and neg. values for example: -10000")
	int chargePower();

	@AttributeDefinition(name = "Hysteresis SoC", description = "Controller charges the ess until it is full, charging is started again when hysteresis soc is reached")
	int hysteresisSoc();

	String webconsole_configurationFactory_nameHint() default "Controller Timeslot Peakshaving [{id}]";
}