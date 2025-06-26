package io.openems.edge.iooffgridswitch;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "IO Off Grid Switch", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "offGridSwitch0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	// input channel ------------------------
	@AttributeDefinition(name = "Digital Input Main Contactor", description = "Input channel of Main Contactor")
	String inputMainContactor() default "io0/DigitalInputM1C1";

	@AttributeDefinition(name = "Digital Input Grid Status", description = "Input channel of Grid Status")
	String inputGridStatus() default "io0/DigitalInputM1C2";

	@AttributeDefinition(name = "Digital Input Grounding Contactor", description = "Input channel of Grounding Contactor")
	String inputGroundingContactor() default "io0/DigitalInputM1C3";

	// output channel ------------------------
	@AttributeDefinition(name = "Digital Output Main Contactor", description = "Output channel of Main Contactor")
	String outputMainContactor() default "io0/DigitalOutputM1C1";

	@AttributeDefinition(name = "Digital Output Grounding Contactor", description = "Output channel of Grounding Contactor")
	String outputGroundingContactor() default "io0/DigitalOutputM1C2";

	String webconsole_configurationFactory_nameHint() default "IO Off Grid Switch [{id}]";

}