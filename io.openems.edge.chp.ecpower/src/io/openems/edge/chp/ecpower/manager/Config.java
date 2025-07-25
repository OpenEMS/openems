package io.openems.edge.chp.ecpower.manager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "CHP EC Power Manager", //
		description = "Manager for EX Power XGRI CHP. Manages control and readonly unit parts")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io.openems.edge.chp.ecpower.manager0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "xrgiRo-ID", description = "ID of the XrgiRo device")
	String xrgiRo_id() default "xrgiRo0";	
	
	@AttributeDefinition(name = "xrgiControl-ID", description = "ID of the Xrgi control device")
	String xrgiControl_id() default "xrgiControl";		
	
	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Debug Mode?", description = "Enable anhanced debugging")
	boolean debugMode() default true;	


	String webconsole_configurationFactory_nameHint() default "io.openems.edge.chp.ecpower.manager [{id}]";

}