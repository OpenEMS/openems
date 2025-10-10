package io.openems.edge.chp.ecpower.manager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "CHP EC Power Manager", //
		description = "Manager for EX Power XGRI CHP. Manages control and readonly unit parts")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "chp.ecpower.manager0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "xrgiRo0-ID", description = "ID of the first XrgiRo device")
	String xrgiRo0_id() default "xrgiRo0";
	
	@AttributeDefinition(name = "xrgiRo1-ID", description = "ID of the second XrgiRo device. Leave empty for none")
	String xrgiRo1_id() default "";	
	
	@AttributeDefinition(name = "xrgiRo2-ID", description = "ID of the third XrgiRo device. Leave empty for none")
	String xrgiRo2_id() default "";
	
	@AttributeDefinition(name = "xrgiRo3-ID", description = "ID of the fourth XrgiRo device. Leave empty for none")
	String xrgiRo3_id() default "";
	
	@AttributeDefinition(name = "xrgiControl-ID", description = "ID of the Xrgi control device")
	String xrgiControl_id() default "xrgiControl0";		
	
	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Debug Mode?", description = "Enable anhanced debugging")
	boolean debugMode() default true;	


	String webconsole_configurationFactory_nameHint() default "CHP EC Power Manager [{id}]";

}