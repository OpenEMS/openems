package io.openems.edge.ess.mr.gridcon.controller.fundamentalfrequency;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.ess.mr.gridcon.enums.FundamentalFrequencyMode;

@ObjectClassDefinition(//
		name = "MR Gridcon Controller Set Fundamental Frequency Mode", //
		description = "MR Gridcon PCS: Sets the fundamental frequency mode for the gridcon.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlSetFundamentalFrequencyMode0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Gridcon-ID", description = "ID of gridcon device.")
	String gridcon_id() default "gridcon0";

	@AttributeDefinition(name = "Fundamental Frequency mode", description = "Fundamental Frequency Mode for Gridon")
	FundamentalFrequencyMode fundamentalFrequencyMode() default FundamentalFrequencyMode.DISABLED;

	@AttributeDefinition(name = "Cos Phi Set Point 1, positive=inductive, negative=capacitive", description = "Cos Phi Set Point 1, positive=inductive, negative=capacitive", max = "1", min = "-1")
	float cosPhiSetPoint1() default 0.9f;

	@AttributeDefinition(name = "Cos Phi Set Point 2, positive=inductive, negative=capacitive", description = "Cos Phi Set Point 2, positive=inductive, negative=capacitive", max = "1", min = "-1")
	float cosPhiSetPoint2() default 0.95f;

	String webconsole_configurationFactory_nameHint() default "MR Gridcon Controller Set Fundamental Frequency Mode [{id}]";
}