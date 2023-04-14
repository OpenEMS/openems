package io.openems.edge.scheduler.daily;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Scheduler Daily", //
		description = "This Scheduler executes the Controller in desired Time in a DAY")

@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "scheduler0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Always Run Before", description = "IDs of Controllers that should be executed _before_ other Controllers in the order of the IDs.")
	String[] alwaysRunBeforeController_ids() default {};

	@AttributeDefinition(name = "Daily Schedule", description = "Execution order of Controllers per time of day.")
	String controllerScheduleJson() default """
		[\
		  {\
		    "time": "08:00:00",\
		    "controllers": [\
		      "ctrlFixActivePower0"\
		    ]\
		  },  {\
		    "time": "13:45:00",\
		    "controllers": [""]\
		  }\
		]""";

	@AttributeDefinition(name = "Always Run After", description = "IDs of Controllers that should be executed _after_ other Controllers in the order of the IDs.")
	String[] alwaysRunAfterController_ids() default { "ctrlDebugLog0" };

	String webconsole_configurationFactory_nameHint() default "Scheduler Daily [{id}]";

}
