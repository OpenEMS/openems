package io.openems.edge.controller.symmetric.timeslotonefullcycle;

import java.time.DayOfWeek;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Timeslot One Full Cycle", //
		description = "Completes one full cycle for an Ess.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlTimeSlotOneFullCycle0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "CycleOrder [Charge/Discharge]", description = "Charge/discharge CycleOrder for Operation")
	CycleOrder cycleorder() default CycleOrder.START_WITH_DISCHARGE;

	@AttributeDefinition(name = " Is Fixed Date Enabled", description = " If yes Day Of Week will take into account otherwise, any day of month can be select")
	boolean isFixedDayTimeEnabled() default false;

	@AttributeDefinition(name = "Choose Day In Month ", description = "Choosen Day In Month [ Ex. Monday of each Month]")
	DayOfWeek dayOfWeek() default DayOfWeek.MONDAY;

	@AttributeDefinition(name = "Choose Day of Month ", description = "Choosen Day of Month [ Ex. First..last day of Month]")
	int dayOfMonth() default 1;

	@AttributeDefinition(name = " Start Time in Hour", description = "Start Charge/Discharge Hour (Just Integer in time).")
	int hour() default 8;

//	@AttributeDefinition(name = " ", description = " ")
//	String anyDateTime() default "[{ \"year\" :2019, \"month\" : 9 , \"day\" : 19, \"hour\" : 12,\"minute\" : 06}]";

	@AttributeDefinition(name = "Power [W]", description = "Charge/discharge power")
	int power();

	String webconsole_configurationFactory_nameHint() default "Controller Timeslot One Full Cycle [{id}]";
}