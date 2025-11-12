package io.openems.edge.io.phoenixcontact;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "io.openems.edge.io.phoenixcontact", //
		description = "PLCnext driver")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "plcnext_dev0";

	@AttributeDefinition(name = "Alias", description = "PxC PLCnext device")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (= default), Consumption")
	MeterType type() default MeterType.PRODUCTION;
	
	@AttributeDefinition(name = "AuthUrl", description = "AuthUrl")
	String authUrl() default "http://localhost:8888/auth";
	
	@AttributeDefinition(name = "Username", description = "Username")
	String username() default "admin";

	@AttributeDefinition(name = "Password", description = "Password")
	String password() default "admin";

	@AttributeDefinition(name = "DataUrl", description = "DataUrl")
	String dataUrl() default "http://localhost:8080/plcnext";

	String webconsole_configurationFactory_nameHint() default "PxC PLCnext device [{id}]";

}