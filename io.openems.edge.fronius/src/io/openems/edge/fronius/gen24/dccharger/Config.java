package io.openems.edge.fronius.gen24.dccharger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.fronius.enums.PvString;

@ObjectClassDefinition(//
		name = "ESS Fronius Gen24 DC Charger PV", //
		description = "Implements the choosen Channel of the Gen24 as PV Inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "charger0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "String Number", description = "Fronius Gen24 String to use.")
	PvString pvString() default PvString.ONE;

	String webconsole_configurationFactory_nameHint() default "ESS Fronius Gen24 hybrid DC Charger PV [{id}]";

}
