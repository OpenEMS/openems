package io.openems.edge.evcs.vw.weconnect;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "io.openems.edge.evcs.vw.weconnect", //
		description = "Implements a connector to a VW electronic car and sets it as virtual EVCS")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io.openems.edge.evcs.vw.weconnect0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Type", description = "Type of WeConnect account. Possible values: VW,id,skoda,seat,vwv2,audi,go")
	String type() default "VW";
	
	@AttributeDefinition(name = "Interval", description = "Request interval")
	int interval() default 1;
	
	@AttributeDefinition(name = "User", description = "E-mail address of user")
	String user() default "waldtrautwagner@googlemail.com";
	
	@AttributeDefinition(name = "Password", description = "password of user")
	String password() default "D7skata!1";
	
	String webconsole_configurationFactory_nameHint() default "io.openems.edge.evcs.vw.weconnect [{id}]";
}