package io.openems.edge.controller.ess.standby;

import java.time.DayOfWeek;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller ESS Standby", //
		description = "Puts a energy storage system in standby mode while regularly checking the functionality once per week.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEssStandby0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Startdate", description = "for example: 30.12.1998")
	String startDate();

	@AttributeDefinition(name = "Enddate", description = "for example: 31.12.1998")
	String endDate();

	@AttributeDefinition(name = "Day of week", description = "On which weekday should the regular check run?")
	DayOfWeek dayOfWeek();

	String webconsole_configurationFactory_nameHint() default "Controller ESS Standby [{id}]";

}