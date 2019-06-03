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

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers.  For Ex; [{ \"time\" : \"12:00:00 \" ,\"controller\" : \"ctrlLimitTotalDischarge0\" }, { \"time\": \"13:00:00\",\"controller \" :  \"ctrlLimitTotalDischarge1\" },{\"time\": \"14:00:00\" ,\"controller\": \"ctrlLimitTotalDischarge2 \" },{\"time\": \"15:00:00\" ,\"controller\": \"ctrlLimitTotalDischarge3\" }]")
	String controllers_ids_json() default "[{ \"time\" : \"12:00:00 \" ,\"controller\" : \"ctrlLimitTotalDischarge0\" }, { \"time\": \"13:00:00\",\"controller \" :  \"ctrlLimitTotalDischarge1\" },{\"time\": \"14:00:00\" ,\"controller\": \"ctrlLimitTotalDischarge2 \" },{\"time\": \"15:00:00\" ,\"controller\": \"ctrlLimitTotalDischarge3\" }]";

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] controllers_ids() default { "ctrlLimitTotalDischarge0", "ctrlLimitTotalDischarge1",
			"ctrlLimitTotalDischarge2", "ctrlLimitTotalDischarge3" };

	String webconsole_configurationFactory_nameHint() default "Daily Scheduler [{id}]";

}
