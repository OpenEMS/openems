package io.openems.edge.ess.mr.gridcon.ongrid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "MR Gridcon ESS On Grid", //
		description = "ESS MR Gridcon PCS on grid variant" //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Gridcon-ID", description = "ID of Gridcon.")
	String gridcon_id() default "gridcon0";

	@AttributeDefinition(name = "Battery-A-ID", description = "ID of Battery A.")
	String bms_a_id() default "bms0";

	@AttributeDefinition(name = "Battery-B-ID", description = "ID of Battery B.")
	String bms_b_id() default "";

	@AttributeDefinition(name = "Battery-C-ID", description = "ID of Battery C.")
	String bms_c_id() default "";

	@AttributeDefinition(name = "Enable IPU 1", description = "IPU 1 is enabled")
	boolean enableIpu1() default true;

	@AttributeDefinition(name = "Enable IPU 2", description = "IPU 2 is enabled")
	boolean enableIpu2() default false;

	@AttributeDefinition(name = "Enable IPU 3", description = "IPU 3 is enabled")
	boolean enableIpu3() default false;

	@AttributeDefinition(name = "Offset Current", description = "An offset current is put on the rack with the highest cell voltage")
	float offsetCurrent() default 0;

	// @AttributeDefinition(name = "Parameter Set", description = "Parameter Set")
	// ParameterSet parameterSet() default ParameterSet.SET_1;

	@AttributeDefinition(name = "Output Gridcon Hard Reset", description = "Output for hard reset for gridcon")
	String outputHardReset() default "io0/DigitalOutputM1C2";

	String webconsole_configurationFactory_nameHint() default "MR Gridcon ESS On Grid [{id}]";

}