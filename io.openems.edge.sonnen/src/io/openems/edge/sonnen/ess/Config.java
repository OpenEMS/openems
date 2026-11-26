package io.openems.edge.sonnen.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.sonnen.core.OperatingMode;

@ObjectClassDefinition(//
		name = "Sonnen ESS", //
		description = "sonnenBatterie energy storage system (Managed Symmetric ESS).")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Core-ID", description = "Component-ID of the Sonnen Core component")
	String core_id() default "sonnenCore0";

	@AttributeDefinition(name = "Control Mode", description = "INTERNAL: the sonnenBatterie controls itself. "
			+ "SMART: the sonnenBatterie controls itself, but OpenEMS takes over if required. "
			+ "REMOTE: OpenEMS fully controls the sonnenBatterie via 'Manual' mode.")
	ControlMode controlMode() default ControlMode.SMART;

	@AttributeDefinition(name = "Default Operating Mode", description = "Operating mode of the sonnenBatterie "
			+ "whenever it is not remote-controlled by OpenEMS.")
	OperatingMode defaultOperatingMode() default OperatingMode.SELF_CONSUMPTION;

	@AttributeDefinition(name = "Capacity", description = "Usable capacity of the battery in [Wh].")
	int capacity() default 5500;

	@AttributeDefinition(name = "Max Apparent Power", description = "Maximum apparent power of the inverter in [VA].")
	int maxApparentPower() default 3300;

	@AttributeDefinition(name = "Max Charge Power", description = "Maximum charge power in [W].")
	int maxChargePower() default 3300;

	@AttributeDefinition(name = "Max Discharge Power", description = "Maximum discharge power in [W].")
	int maxDischargePower() default 3300;

	String webconsole_configurationFactory_nameHint() default "Sonnen ESS [{id}]";
}
