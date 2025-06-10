package io.openems.edge.controller.asymmetric.balancingcosphi;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Balancing Cos-Phi Asymmetric", //
		description = "Keeps the Grid meter on a defined Cos-Phi.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlBalancingCosPhi0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Cos-Phi", description = "Cosinus Phi (e.g. '1' or '0.95').")
	double cosPhi() default ControllerAsymmetricBalancingCosPhi.DEFAULT_COS_PHI;

	@AttributeDefinition(name = "Inductive/Capacitive", description = "Inductive or Capacitive cos phi.")
	CosPhiDirection direction() default CosPhiDirection.CAPACITIVE;

	String webconsole_configurationFactory_nameHint() default "Controller Balancing Cos-Phi Asymmetric [{id}]";
}