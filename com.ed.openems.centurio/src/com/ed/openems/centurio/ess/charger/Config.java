package com.ed.openems.centurio.ess.charger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KACO CENTURIO DC Charger", //
		description = "Implements the KACO Centurio DC Charger.")
@interface Config {
	String service_pid();

	String id() default "charger0";

	boolean enabled() default true;

	@AttributeDefinition(name = "EdCom-ID", description = "ID of EdCom Interface.")
	String datasource_id() default "edcom0";

	@AttributeDefinition(name = "Maximum Ever Actual Power", description = "This is automatically updated.")
	int maxActualPower();

	String webconsole_configurationFactory_nameHint() default "KACO CENTURIO DC Charger [{id}]";
}
