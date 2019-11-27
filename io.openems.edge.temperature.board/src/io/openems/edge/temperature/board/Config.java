package io.openems.edge.temperature.board;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Consolinno Temperature Board",
        description = "Consolinno Temperature Board, connected to the Leaflet. Communicating with Sensors.")
@interface Config {

    @AttributeDefinition(name = "Temperature Board Id", description = "Unique ID of Temperature Board.")
    String id() default "TemperatureBoard0";

    @AttributeDefinition(name = "alias", description = "Human readable Name.")
    String alias() default "";

    @AttributeDefinition(name = "Version Number", description = "What Version of the Circuit Board you are using.",
    options = @Option(label = "Version 1.0", value = "1"))
    String versionNumber() default "1";

    @AttributeDefinition(name = "ADC Frequencies", description = "ADC Frequency, if more than 1 ADC is on CircuitBoard seperate via ';'.")
    String adcFrequency() default "500000;500000";

    @AttributeDefinition(name = "Dip Switches", description = "What Switches are enabled on the Circuit Board, just type in the Numbers like 25.")
    String dipSwitches() default "01";

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Circuitboard [{id}]";
}
