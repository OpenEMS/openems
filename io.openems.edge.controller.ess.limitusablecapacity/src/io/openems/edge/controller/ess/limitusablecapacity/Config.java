package io.openems.edge.controller.ess.limitusablecapacity;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller io.openems.edge.controller.ess.limitusablecapacity", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlio.openems.edge.controller.ess.limitusablecapacity0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();
	
	@AttributeDefinition(name = "", description = "")
	int stopDischargeSoc() default 10;
	
	@AttributeDefinition(name = "", description = "")
	int allowDischargeSoc() default 12;
	
	@AttributeDefinition(name = "", description = "")
	int forceChargeSoc() default 8;
	
	@AttributeDefinition(name = "", description = "")
	int stopChargeSoc() default 90;
	
	@AttributeDefinition(name = "", description = "")
	int allowChargeSoc() default 85;
	
	String webconsole_configurationFactory_nameHint() default "Controller io.openems.edge.controller.ess.limitusablecapacity [{id}]";

}