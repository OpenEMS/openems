package io.openems.edge.io.phoenixcontact;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "IO PxC PLCnext", //
		description = "Provides driver for PxC PLCnext based components. Take care of GDS shared object." //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pxc_plcnext_0";

	@AttributeDefinition(name = "Alias", description = "Readable name of PLCnext device")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (= default), Consumption")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Auth-URL", description = "Defines URL to authorize PLCnext user")
	String authUrl() default "https://192.168.1.10/_pxc_api/v1.3/auth";

	@AttributeDefinition(name = "Username", description = "Credentials: username")
	String username() default "admin";

	@AttributeDefinition(name = "Password", description = "Credentials: password", type = AttributeType.PASSWORD)
	String password() default "admin";

	@AttributeDefinition(name = "Data-URL", description = "Defines base URL to pickup data from GDS")
	String dataUrl() default "https://192.168.1.10/_pxc_api/api";

	@AttributeDefinition(name = "Data instance name", description = "Instance name of OpenEMS spaces in GDS")
	String[] dataInstanceNames() default "gds_openems_0";

	String webconsole_configurationFactory_nameHint() default "PxC PLCnext device [{id}]";

}