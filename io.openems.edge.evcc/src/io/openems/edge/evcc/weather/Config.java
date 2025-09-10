package io.openems.edge.evcc.weather;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;

@ObjectClassDefinition(
    name = "Weather evcc",
    description = "Fetches weather-related values from evcc API."
)
@interface Config {

    @AttributeDefinition(
        name = "Component-ID",
        description = "Unique ID of this component"
    )
    String id() default "weatherEvcc0";

    @AttributeDefinition(
        name = "Alias",
        description = "Human-readable name of this component; defaults to Component-ID"
    )
    String alias() default "";

    @AttributeDefinition(
        name = "Enabled",
        description = "Is this component enabled?"
    )
    boolean enabled() default true;

    @AttributeDefinition(
        name = "API URL",
        description = "URL of the evcc API endpoint to fetch weather data"
    )
    String apiUrl() default "http://localhost:7070/api/tariff/solar";
    
	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

    String webconsole_configurationFactory_nameHint() default "Weather evcc [{id}]";
}
