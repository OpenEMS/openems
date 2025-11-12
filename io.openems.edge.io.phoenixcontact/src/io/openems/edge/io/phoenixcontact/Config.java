package io.openems.edge.io.phoenixcontact;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "PxC PLCnext driver", //
		description = "Implements a driver for PLCnext platform of Phoenix Contact" //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pxc_plcnext_dev0";

	@AttributeDefinition(name = "Alias", description = "Readable name of PLCnext device")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (= default), Consumption")
	MeterType type() default MeterType.PRODUCTION;
	
	@AttributeDefinition(name = "Auth-URL", description = "Defines URL to authorize PLCnext user")
	String authUrl() default "http://localhost:8888/auth";
	
	@AttributeDefinition(name = "Username", description = "Credentials: username")
	String username() default "admin";

	@AttributeDefinition(name = "Password", description = "Credentials: password")
	String password() default "admin";

	@AttributeDefinition(name = "Data-URL", description = "Defines base URL to pickup data from GDS")
	String dataUrl() default "http://localhost:8080/plcnext";
	
	@AttributeDefinition(name = "Data instance name", description = "Instance name of OpenEMS spaces in GDS")
	String dataInstanceName() default "gds_openems0";

	String webconsole_configurationFactory_nameHint() default "PxC PLCnext device [{id}]";

}