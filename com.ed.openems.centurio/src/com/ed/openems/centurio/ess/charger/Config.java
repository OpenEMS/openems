package com.ed.openems.centurio.ess.charger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KACO blueplanet 10.0 TL3 hybrid DC Charger", //
		description = "Implements the KACO blueplanet 10.0 TL3 hybrid DC Charger.")
@interface Config {
	String service_pid();

	String id() default "charger0";

	boolean enabled() default true;

	@AttributeDefinition(name = "bpCom-ID", description = "ID of bpCom Interface.")
	String datasource_id() default "bpcom0";

	@AttributeDefinition(name = "Maximum Ever Actual Power", description = "This is automatically updated.")
	int maxActualPower();

	String webconsole_configurationFactory_nameHint() default "KACO blueplanet 10.0 TL3 hybrid DC Charger [{id}]";
}
