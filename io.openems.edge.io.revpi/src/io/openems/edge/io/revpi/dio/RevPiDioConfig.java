package io.openems.edge.io.revpi.dio;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "IO RevolutionPi Digital IO Board", //
		description = "Implements the access to the Kunbus RevolutionPi Digital IO enhancement hardware")
@interface RevPiDioConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "dio0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read DataOut Initially", description = "Init Outputs with state from hardware initially")
	boolean initOutputFromHardware() default true;

	@AttributeDefinition(name = "Digital Input Name Prefix", description = "Channel name prefix configered within Pictory without channel number, e.g. DigitalInput_.")
	String prefixDigitalIn() default "DigitalInput_";

	@AttributeDefinition(name = "Digital Output Name Prefix", description = "Channel name prefix configered within Pictory without channel number, e.g. DigitalOutput_.")
	String prefixDigitalOut() default "DigitalOutput_";

	@AttributeDefinition(name = "First Digital Input Index", description = "Offset if multiple Digital IO Modules are used (First input index of this IO Module)")
	int firstInputIndex() default 1;

	@AttributeDefinition(name = "First Digital Output Index", description = "Offset if multiple Digital IO Modules are used (First output index of this IO Module)")
	int firstOutputIndex() default 1;

	@AttributeDefinition(name = "Input Used", description = "0 (not used) or 1 (used) for !all 14! Input channels separated by | that OpenEms is allowed to access, e.g. 1|1|1|0|1|1|1|1|1|1|0|1|1|1|1|1")
	String inputUsed() default "1|1|1|1|1|1|1|1|1|1|1|1|1|1|1|1";

	@AttributeDefinition(name = "Output Used", description = "0 (not used) or 1 (used) for for !all 14! Output channels separated by | that OpenEms is allowed to access, e.g. 1|1|1|0|1|1|1|1|1|1|0|1|1|1|1|1")
	String outputUsed() default "1|1|1|1|1|1|1|1|1|1|1|1|1|1|1|1";

	String webconsole_configurationFactory_nameHint() default "IO RevolutionPi Digital IO Board [{id}]";
}