package io.openems.edge.controller.ess.ripplecontrolreceiver;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Ripple Control Receiver", //
		description = "Controller to optimize energy distribution during peak hours by reducing the inverter output to 0, 30 or 60 percent.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEssRippleControlReceiver0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Input Channel 0%", description = "When receiving a signal, this channel reduces the inverter power to 0%.")
	String inputChannelAddress1() default "io1/DigitalInput2";

	@AttributeDefinition(name = "Input Channel 30%", description = "When receiving a signal, this channel reduces the inverter power to 30%.")
	String inputChannelAddress2() default "io1/DigitalInput3";

	@AttributeDefinition(name = "Input Channel 60%", description = "When receiving a signal, this channel reduces the inverter power to 60%.")
	String inputChannelAddress3() default "io1/DigitalInput4";

	String webconsole_configurationFactory_nameHint() default "Controller Ess Ripple Control Receiver [{id}]";

}