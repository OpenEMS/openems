package io.openems.edge.controller.chp.cost;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller CHP cost optimization", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlChpCostOptimization0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Debug mode", description = "Enhanced debug mode")
	boolean debugMode() default true;	
	
	@AttributeDefinition(name = "Maximum Cost [Ct/h]", description = "CHP is started if grid cosumption exceeds this cost value (grid-consumption * price)")
	int maxCost() default 100;	
	
	@AttributeDefinition(name = "Minimum Cost [Ct/h]", description = "CHP is not started if grid cosumption costs are below that value")
	int minCost() default 1;		
	
	@AttributeDefinition(name = "Maximum CHP power [W]", description = "Defines the rated electrical output power of the CHP system in watts.")
	int maxActivePower() default 10000;		
	
	@AttributeDefinition(name = "Start Hysteresis [s]", description = "Minimum time (in seconds) to wait before allowing a new start after a shutdown.")
	int startHyteresis() default 3600;		

	@AttributeDefinition(name = "Stop Hysteresis [s]", description = "Minimum run time (in seconds) before the CHP is allowed to stop.")
	int stopHyteresis() default 3600;		
	
	@AttributeDefinition(name = "Grid meter ID", description = "Id of grid meter")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "CHP id", description = "Id of chp device")
	String chp_id() default "chp0";

	String webconsole_configurationFactory_nameHint() default "Controller CHP cost optimization [{id}]";

}