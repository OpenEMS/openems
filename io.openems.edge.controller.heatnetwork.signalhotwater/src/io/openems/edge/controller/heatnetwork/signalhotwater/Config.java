package io.openems.edge.controller.heatnetwork.signalhotwater;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno SignalHotWater",
        description = "Controller that monitors the temperature of a water tank and sends the signal \"need hot water\"."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Heating Controller.")
    String id() default "SignalHotWater0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "Warmwasser Anforderung";

    @AttributeDefinition(name = "Upper temperature sensor", description = "The upper temperature sensor in the water tank, allocated to this controller.")
    String temperatureSensorUpperId() default "T_PS_oben";

    @AttributeDefinition(name = "Lower temperature sensor", description = "The lower temperature sensor in the water tank, allocated to this controller.")
    String temperatureSensorLowerId() default "T_PS_unten";

    @AttributeDefinition(name = "Minimum temperature upper sensor", description = "Minimum temperature of the water tank upper sensor. Unit is °C")
    int min_temp_upper() default 70;

    @AttributeDefinition(name = "Maximum temperature lower sensor", description = "Maximum temperature of the water tank lower sensor. Unit is °C")
    int max_temp_lower() default 65;

    @AttributeDefinition(name = "Delay after not too cold", description = "in seconds")
    int getHeatingDelay() default 600;

    @AttributeDefinition(name = "response timeout in seconds", description = "How long to wait for the response signal before continuing without it. Unit is seconds.")
    int response_timeout() default 10;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno SignalHotWater [{id}]";
}