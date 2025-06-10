package io.openems.edge.controller.highloadtimeslot;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller High-Load Timeslot", //
		description = "This controller discharges the storage system at a defined time with a defined load; charges within remaining time.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHighLoadTimeslot0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess();

	@AttributeDefinition(name = "Startdate", description = "for example: 30.12.1998")
	String startDate();

	@AttributeDefinition(name = "Enddate", description = "for example: 31.12.1998")
	String endDate();

	@AttributeDefinition(name = "Daily Starttime", description = "for example: 06:46")
	String startTime();

	@AttributeDefinition(name = "Daily Endtime", description = "for example: 17:05")
	String endTime();

	@AttributeDefinition(name = "Which days?", description = "On which days should the algorithm run?")
	WeekdayFilter weekdayFilter() default WeekdayFilter.EVERDAY;

	@AttributeDefinition(name = "Charge Power", description = "Charge power per ess in Watt and neg. values for example: -10000")
	int chargePower();

	@AttributeDefinition(name = "Discharge Power", description = "Charge power per ess in Watt and positive values for example: 29000")
	int dischargePower();

	@AttributeDefinition(name = "Hysteresis SoC", description = "Controller charges the ess until it is full, charging is started again when hysteresis soc is reached")
	int hysteresisSoc();

	String webconsole_configurationFactory_nameHint() default "Controller High-Load Timeslot [{id}]";
}