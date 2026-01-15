package io.openems.edge.phoenixcontact.plcnext.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Phoenix Contact PLCnext compatible ESS", //
		description = "Provides a compatible ESS component for Phoenix Contact PLCnext platform. Take care of corresponding PLCnext component library." //
)
public @interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Readable name of PLCnext device")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Base-URL", description = "Defines base URL for PLCnext authorization and data access")
	String baseUrl() default "https://192.168.1.10/_pxc_api";

	@AttributeDefinition(name = "Username", description = "Credentials: username")
	String username() default "admin";

	@AttributeDefinition(name = "Password", description = "Credentials: password", type = AttributeType.PASSWORD)
	String password() default "admin";

	@AttributeDefinition(name = "Data instance name", description = "Instance name of OpenEMS spaces in GDS")
	String dataInstanceName() default "PLCnextEss";

	String webconsole_configurationFactory_nameHint() default "Phoenix Contact PLCnext BESS [{id}]";
}
