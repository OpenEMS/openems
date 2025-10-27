package io.openems.edge.controller.chp.costoptimization;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller CHP cost optimization ", //
		description = "This is a Controller for CHP (Combined Heat and Power Unit, German: BHKW - Blockheizkraftwerk). The Controller is used to signal CHP turn ON if total consumption costs exeed a configured value")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlChpCostOptimization0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Debug mode", description = "Enhanced debug mode")
	boolean debugMode() default true;

	@AttributeDefinition(name = "Maximum Cost [€/MWh]", description = "CHP is started if grid cosumption exceeds this price value")
	int priceThreshold() default 100;

	@AttributeDefinition(name = "Maximum CHP power [W]", description = "Defines the over all rated electrical output power of the CHP system in watts.")
	int maxActivePower() default 10000;

	@AttributeDefinition(name = "Start Hysteresis [s]", description = "Minimum time (in seconds) to wait before allowing a new start after a shutdown.")
	int startHyteresis() default 3600;

	@AttributeDefinition(name = "Run Hysteresis [s]", description = "Minimum run time (in seconds) before the CHP is allowed to stop.")
	int runHyteresis() default 3600;

	@AttributeDefinition(name = "Preparation Hysteresis [s]", description = "Before CHP is started heating system might need to be prepared, i.e. shut down other heating systems to lower temperatures. \n In order to do that future energy costs are calculated")
	int preparationHyteresis() default 3600;
	
	@AttributeDefinition(name = "Min. buffer tank temperature [°C]", description = "Minimum buffer tank temperature. If below that value CHP will be started forcefully")
	int minBufferTankTemperature() default 60;
	
	@AttributeDefinition(name = "Threshold buffer tank temperature [°C]", description = "Threshold buffer tank temperature. CPHs won´t start above that value")
	int thresholdBufferTankTemperature() default 70;
	
	@AttributeDefinition(name = "Max. buffer tank temperature [°C]", description = "Maximum buffer tank temperature before chp(s) will be stopped")
	int maxBufferTankTemperature() default 75;	

	@AttributeDefinition(name = "Min. grid power", description = "Min. power from grid to activate this controller")
	int minGridPower() default 40000;		
	
	@AttributeDefinition(name = "Grid meter ID", description = "Id of grid meter")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "CHP id", description = "Id of chp device")
	String chp_id() default "chp0";

	String webconsole_configurationFactory_nameHint() default "Controller CHP cost optimization [{id}]";

}