package io.openems.edge.evcc.loadpoint;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(name = "Loadpoint consumption meter (evcc-API)", description = "Provides loadpoint consumption data using the evcc-API.")
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

	@AttributeDefinition(name = "Loadpoint Index", description = "Index of the loadpoint in EVCC's response, e.g. 0 for the first")
	int loadpointIndex() default 0;

	String webconsole_configurationFactory_nameHint() default "Loadpoint consumption evcc [{id}]";
}
