package io.openems.edge.temperature.board;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Consolinno Temperature Board",
        description = "Consolinno Temperature Board, connected to the Leaflet. Communicating with Sensors")
@interface Config {
    @AttributeDefinition(name = "Temperature Board Id", description = "Unique ID of SensorComponent")
    String id() default "TemperatureBoard0";

    @AttributeDefinition(name = "Version Number", description = "What Version of the Circuit Board you are using")
    String versionNumber() default "1";

    @AttributeDefinition(name = "ADC Frequencies", description = "ADC Frequency, if more than 1 ADC is on CircuitBoard seperate via ';'")
    String adcFrequency() default "500000;500000";

    @AttributeDefinition(name = "Dip Switches", description = "What Switches are enabled on the Circuit Board, just type in the Numbers like 025")
    String dipSwitches() default "01";

    @AttributeDefinition(name = "alias", description = "alternate readable Name.")
    String alias() default "";

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Circuitboard [{id}]";
}
