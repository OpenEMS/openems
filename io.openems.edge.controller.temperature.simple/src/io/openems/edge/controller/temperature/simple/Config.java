package io.openems.edge.controller.temperature.simple;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Controller Consolinno Temperature Simple",
        description = "This Controller opens or Closes a Relais depending on Temperature"
)

@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique id of this Controller")
    String id() default "simpleTemperatureController0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Relais Id", description = "Unique Id of Relais what you want to be controlled")
    String relaisId() default "Relais0";

    @AttributeDefinition(name = "TemperatureSensor Id", description = "Unique Id of Temperature Sensor what you want to be controlled")
    String temperatureId() default "TemperatureSensor0";

    @AttributeDefinition(name = "Wanted Temperature in dC", description = "What Temperature you want to reach. 1°C = 10")
            float wantedTemperature() default 250.f;

    @AttributeDefinition(name = "Tolerance Temperature in dC", description = "Tolerated Temperature difference to wanted Temp.")
            float toleranceTemperature() default 0;

    boolean enabled() default true;

}
