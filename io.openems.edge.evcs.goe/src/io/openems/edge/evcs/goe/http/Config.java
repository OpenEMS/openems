package io.openems.edge.evcs.goe.http;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVCS go-e Gemini Http", //
		description = "Implementation for the go-e Gemini charging station using Http")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "192.168.1.130";

	@AttributeDefinition(name = "Minimum hardware current", description = "Minimum charging current of the Charger in mA.", required = true)
	int minHwCurrent() default 6_000;

	@AttributeDefinition(name = "Maximum hardware current", description = "Maximum charging current of the Charger in mA.", required = true)
	int maxHwCurrent() default 32_000;

	@AttributeDefinition(name = "Phase rotation", description = "The way in which the phases are physically rotated.")
    PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Read only", description = "Defines that this evcs is read only.", required = true)
	boolean readOnly() default true;

	String webconsole_configurationFactory_nameHint() default "EVCS go-e Gemini [{id}]";

}