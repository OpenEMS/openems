package io.openems.edge.consolinno.leaflet.core.pwm.configurator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbus Pwm Configurator", description = "Configurator for the Frequency of a Modbus Pwm Module.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Pwm Frequency Configurator.")
    String id() default "PwmFrequencyConfigurator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Pwm Module Module Number", description = "Module Number for the Pwm Module that has to be configured.")
    int moduleNumber();

    @AttributeDefinition(name = "Frequency", description = "Frequency of the Pwm Module.")
    int frequency() default 24;

    String webconsole_configurationFactory_nameHint() default "{id}";
}