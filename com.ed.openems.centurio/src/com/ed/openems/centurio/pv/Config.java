package com.ed.openems.centurio.pv;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Energy Depot Centurio PV Meter", //
		description = "Implements the Energy Depot Centurio pv-meter component.")
@interface Config {
	String service_pid();

	String id() default "meter1";

	boolean enabled() default true;

	@AttributeDefinition(name = "EdCom-ID", description = "ID of EdCom Interface.")
	String datasource_id() default "edcom0";

	@AttributeDefinition(name = "Max Power", description = "Maximum power of PV input.")
	int maxP() default 12000;

	String webconsole_configurationFactory_nameHint() default "Energy Depot Centurio PV Meter[{id}]";
}