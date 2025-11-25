package io.openems.edge.controller.cleverpv;

import io.openems.common.types.DebugMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Clever-PV", //
		description = "This controller connects to Clever-PV")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlCleverPv0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "URL", description = "Full API URL. See https://www.clever-pv.com/anleitungen/push-api", type = AttributeType.PASSWORD)
	String url();

	@AttributeDefinition(name = "Read only", description = "Defines that this controller is read only. This permanently disables remote control via Clever-PV.", required = true)
	boolean readOnly() default true;

	@AttributeDefinition(name = "Control Mode", description = "Set the type of control mode.")
	ControlMode controlMode() default ControlMode.OFF;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "Controller Clever-PV [{id}]";
}
