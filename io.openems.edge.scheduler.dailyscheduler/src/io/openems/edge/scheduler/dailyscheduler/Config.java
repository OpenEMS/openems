package io.openems.edge.scheduler.dailyscheduler;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition( //
		name = "Scheduler Daily", //
		description = "This Scheduler executes the Controller in desired Time in a DAY")

@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "scheduler0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is based on the timings described in the json")
	String controllerTimes() default "" + "[" //
			+ "  {" //
			+ "    \"time\": \"13:42:00\"," //
			+ "    \"controllers\": [" //
			+ "      \"ctrlFixActivePower0\"" //
			+ "    ]" //
			+ "  },  {" //
			+ "    \"time\": \"13:45:00\"," //
			+ "    \"controllers\": [\"\"]" //
			+ "  }" //
			+ "]";

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] alwaysRunControllers_ids() default { "ctrlDebugLog0" };

	String webconsole_configurationFactory_nameHint() default "Daily Scheduler [{id}]";

}
