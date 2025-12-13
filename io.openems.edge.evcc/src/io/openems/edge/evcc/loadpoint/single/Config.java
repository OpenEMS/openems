package io.openems.edge.evcc.loadpoint.single;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SinglePhase;

@ObjectClassDefinition(name = "Loadpoint single-phase consumption meter (evcc-API)", description = "Provides single-phase loadpoint consumption data using the evcc-API.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "loadpoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "evcc API URL", description = "URL to fetch loadpoint data from EVCC. Example: http://localhost:7070/api/state")
	String apiUrl() default "http://localhost:7070/api/state";

	@AttributeDefinition(name = "Loadpoint Title", description = "Title of the loadpoint in EVCC (e.g. 'Carport', 'Garage', 'Heatpump'). Used as primary reference. Leave empty to use index only.")
	String loadpointTitle() default "";

	@AttributeDefinition(name = "Loadpoint Index", description = "Index of the loadpoint in EVCC's response, e.g. 0 for the first. Used as fallback if title is not found or empty.")
	int loadpointIndex() default 0;

	@AttributeDefinition(name = "Phase", description = "Which phase is the meter connected?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "Minimum charging power", description = "Minimum charging power in W (e.g. 6A × 230V = 1380W)")
	int minChargingPowerW() default 1380;

	@AttributeDefinition(name = "Maximum charging power", description = "Maximum charging power in W (e.g. 32A × 230V = 7360W)")
	int maxChargingPowerW() default 7360;

	String webconsole_configurationFactory_nameHint() default "Loadpoint consumption evcc (single-phase) [{id}]";
}
