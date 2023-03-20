package io.openems.edge.io.hal.raspberrypi;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(//
		name = "IO RaspberryPi Hardware Abstraction Layer (HAL)", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io.openems.edge.io.hal.raspberrypi0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "GPIO Path", description = "Path to the GPIOs on the filesystem.")
	String gpioPath() default "/sys/class/gpio";
	
	@AttributeDefinition(name = "Hardware Plattform", description = "The hardware plattform in use.")
	HardwarePlattformEnum hardwarePlattform() default HardwarePlattformEnum.MODBERRY_X500_CM4;
	

	String webconsole_configurationFactory_nameHint() default "io.openems.edge.io.hal.raspberrypi [{id}]";

}