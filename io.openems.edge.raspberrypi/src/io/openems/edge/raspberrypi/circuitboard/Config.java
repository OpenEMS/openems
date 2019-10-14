package io.openems.edge.raspberrypi.circuitboard;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Circuit Boards",
        description = "Consolinno Circuit Boards, connected to the Leaflet. Communicating with Sensors")
@interface Config {
    @AttributeDefinition(name = "CircuitBoardId", description = "Unique ID of SensorComponent")
    String boardId() default "Temperature0";

    @AttributeDefinition(name = "CircuitBoardType", description = "What kind Circuit Board do you want to activate?")
    String boardType() default "Temperature";

    @AttributeDefinition(name = "Version Number", description = "What Version of the Circuit Board you are using")
    String versionNumber() default "1";

    @AttributeDefinition(name = "ADC Frequencies", description = "ADC Frequency, if more than 1 ADC is on CircuitBoard seperate via ';'")
    String adcFrequency() default "500_000;500_000";

    @AttributeDefinition(name = "Dip Switches", description = "What Switches are enabled on the Circuit Board, just type in the Numbers like 025")
    String dipSwitches() default "01";

    @AttributeDefinition(name = "alias", description = "alternate readable Name.")
    String alias() default "";



    String webconsole_configurationFactory_nameHint() default "Circuitboard [{id}]";
}
