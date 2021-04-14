package io.openems.edge.controller.heatnetwork.heatingcurveregulator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Heating Curve Regulator",
        description = "Automatic regulator that adjusts heating depending on outside temperature."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Heating Controller.")
    String id() default "HeatingCurveRegulator0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "Witterungsgesteuerter automatischer Heizregler";

    @AttributeDefinition(name = "TemperatureSensor", description = "The Temperaturesensor allocated to this controller")
    String temperatureSensorId() default "TemperatureSensor5";

    @AttributeDefinition(name = "Activation temperature", description = "Unit is °C. If the temperature measured by the sensor falls below this value, heating starts. The value needs to be equal or lower than the room temperature set in the next option.")
    int activation_temp() default 18;

    @AttributeDefinition(name = "Curve parameter: room temperature", description = "The desired room temperature, in °C. Needs to be higher than activation temperature.")
    int room_temp() default 20;

    @AttributeDefinition(name = "Averaging time temperature measurement", description = "Unit is minutes. When a temperature threshold is reached, "
            + "the controller will take measurements over this period of time and decide what to do based on the average. Will not wait that long if "
            + "the average of one minute has 5k difference to the threshold temperature. Needs to be equal or lower than minimum state time.")
    int measurement_time_minutes() default 10;

    @AttributeDefinition(name = "Minimum state time", description = "Unit is minutes. When the controller changes state (heating or no heating), it "
            + "will keep that state for at least this amount of time to prevent flip flopping.")
    int minimum_state_time_minutes() default 60;

    @AttributeDefinition(name = "Curve parameter: slope", description = "Slope of the heating curve.")
    double slope() default 1;

    @AttributeDefinition(name = "Curve parameter: offset", description = "Offset in the heating curve to account for losses in the heating system, in °C.")
    int offset() default 5;


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno Heating Curve Regulator [{id}]";
}