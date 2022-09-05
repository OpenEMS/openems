package io.openems.edge.ess.byd.container.watchdog;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ESS FENECON BYD Container Watchdog-Controller", //
		description = "TODO")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlBydContainerWatchdog0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "Component-ID of the BYD Container Component")
	String ess_id() default "ess0";

	String webconsole_configurationFactory_nameHint() default "ESS FENECON BYD Container Watchdog-Controller [{id}]";

}
