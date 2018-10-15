package com.ed.openems.centurio.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KACO Centurio Ess", //
		description = "Implements the Energy Depot Centurio ess component.")
@interface Config {
	String service_pid();

	String id() default "ess0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Read-Only mode", description = "Enables Read-Only mode")
	boolean readonly() default false;
	
	@AttributeDefinition(name = "EdCom-ID", description = "ID of EdCom Interface.")
	String datasource_id() default "edcom0";
	
	@AttributeDefinition(name = "max Power", description = "The maximum power (W) of the Domus Battery")
	int maxP() default 4000;

	String webconsole_configurationFactory_nameHint() default "KACO Centurio ess [{id}]";
}
