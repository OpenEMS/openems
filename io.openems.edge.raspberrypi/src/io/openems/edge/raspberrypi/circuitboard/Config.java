package io.openems.edge.raspberrypi.circuitboard;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Circuit Boards",
        description = "Consolinno Circuit Boards, connected to the Leaflet. Communicating with Sensors")
@interface Config {
    @AttributeDefinition(name = "CircuitBoardId", description = "Unique ID of SensorComponent --> BestPractice: Type Nr")
    String boardId() default "Temperature0";

    @AttributeDefinition(name = "CircuitBoardType", description = "What Circuit Board do you want to activate?")
    String boardType() default "Temperature";

    @AttributeDefinition(name = "Version Number", description = "What Version of the Circuit Board you are using")
    String versionNumber() default "1";

    @AttributeDefinition(name = "alias", description = "alternate readable Name.")
    String alias() default "";
}
