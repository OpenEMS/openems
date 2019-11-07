package io.openems.edge.controller.timelinecharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Project Sambia Controller TimelineCharge", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlSambiaTimelineCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Charger-IDs", description = "IDs of the Chargers.")
	String[] charger_ids();

	@AttributeDefinition(name = "Max-ApparentPower", description = "Max apparent power of the Ess when charging.")
	int allowedApparent() default 10_000;

	@AttributeDefinition(name = "Monday", description = "Sets the soc limits for monday.")
	String monday();

	@AttributeDefinition(name = "Tuesday", description = "Sets the soc limits for tuesday.")
	String tuesday();

	@AttributeDefinition(name = "Wednesday", description = "Sets the soc limits for wednesday.")
	String wednesday();

	@AttributeDefinition(name = "Thursday", description = "Sets the soc limits for thursday.")
	String thursday();

	@AttributeDefinition(name = "Friday", description = "Sets the soc limits for friday.")
	String friday();

	@AttributeDefinition(name = "Saturday", description = "Sets the soc limits for saturday.")
	String saturday();

	@AttributeDefinition(name = "Sunday", description = "Sets the soc limits for sunday.")
	String sunday();

	String webconsole_configurationFactory_nameHint() default "Controller TimelineCharge [{id}]";

}