package io.openems.edge.ess.mr.gridcon;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller On Grid for MR Gridcon PCS", //
		description = "Implements a controller for an on grid MR Gridcon PCS")
public
@interface OnGridConfig {
	String id() default "ctrlGridconOngrid0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Gridcon-ID", description = "ID of Gridcon.")
	String gridcon_id() default "ess0";
	
	@AttributeDefinition(name = "Battery-A-ID", description = "ID of Battery A.")
	String bms_a_id() default "bms0";
	
	@AttributeDefinition(name = "Battery-B-ID", description = "ID of Battery B.")
	String bms_b_id() default "";
	
	@AttributeDefinition(name = "Battery-C-ID", description = "ID of Battery C.")
	String bms_c_id() default "";

	String webconsole_configurationFactory_nameHint() default "ESS MR Gridcon PCS [{id}]";
}
