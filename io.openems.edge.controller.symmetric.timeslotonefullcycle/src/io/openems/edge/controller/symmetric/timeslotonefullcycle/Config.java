package io.openems.edge.controller.symmetric.timeslotonefullcycle;

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

	@AttributeDefinition(name = " Standby Time in Minute", description = "Wait Between Charge and Discharge (Just Integer in time).")
	int standbyTime() default 1;

	@AttributeDefinition(name = " Start Time in Hour", description = "Start Charge/Discharge Hour (2021-02-08[space in between]13:35).")
	String startTime() default "2021-02-08 13:35";
	
	@AttributeDefinition(name = "Maximum Soc [%]", description = "Limit Charge ")
	int maxSoc() default 90;
	
	@AttributeDefinition(name = "Minimum Soc [%]", description = "Limit Discharge")
	int minSoc() default 5;
	
	@AttributeDefinition(name = "Power [W]", description = "Charge/discharge power")
	int power();

	String webconsole_configurationFactory_nameHint() default "Controller Timeslot One Full Cycle [{id}]";
}