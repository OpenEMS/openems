package io.openems.edge.scheduler.dailyscheduler;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition( //
		name = "Scheduler Daily Scheduler", description = "Active Controller in desired Time In a DAY")

@interface Config {

	String id() default "dailyScheduler0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	boolean enabled() default true;

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers.  Controller execution is based on the timings described in the json")
	String controllers_ids_json() default "[{   \"time\": \"13:42:00\",   \"controller\": [\"ctrlFixActivePower0\"]  },  {   \"time\": \"13:45:00\",   \"controller\": [\"\"]  },  {   \"time\": \"13:46:00\",   \"controller\": [\"\"]  },  {   \"time\": \"13:47:00\",   \"controller\": [\"ctrlFixActivePower0\"]  } ]";

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] controllers_ids() default { "ctrlLimitTotalDischarge0", "ctrlLimitTotalDischarge1",
			"ctrlLimitTotalDischarge2", "ctrlLimitTotalDischarge3" };

	String webconsole_configurationFactory_nameHint() default "Daily Scheduler [{id}]";

}
