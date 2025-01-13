package io.openems.edge.io.gpio;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.io.gpio.hardware.HardwareType;

@ObjectClassDefinition(//
		name = "IO GPIO", //
		description = "General purpose Input/Output provider for digital IOs")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "GPIO Path", description = "Path to the GPIOs on the filesystem.")
	String gpioPath() default "/sys/class";

	@AttributeDefinition(name = "Hardware Type", description = "The hardware type in use.")
	HardwareType hardwareType() default HardwareType.MODBERRY_X500_M40804_WB;

	String webconsole_configurationFactory_nameHint() default "IO GPIO [{id}]";
}