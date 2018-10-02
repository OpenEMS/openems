package com.ed.openems.centurio.grid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Energy Depot Centurio Grid Meter", //
		description = "Implements the Energy Depot Centurio Grid Meter component.")
@interface Config {
	String service_pid();

	String id() default "meter0";

	boolean enabled() default true;

	@AttributeDefinition(name = "EdCom-ID", description = "ID of EdCom Interface.")
	String datasource_id() default "edcom0";

	String webconsole_configurationFactory_nameHint() default "Energy Depot Centurio Meter[{id}]";
}