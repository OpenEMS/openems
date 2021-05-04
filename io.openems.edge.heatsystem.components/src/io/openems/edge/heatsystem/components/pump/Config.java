package io.openems.edge.heatsystem.components.pump;

import io.openems.edge.heatsystem.components.ConfigurationType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;


@ObjectClassDefinition(
        name = "Hydraulic Pump Pwm/Relay",
        description = "A Pump mainly used for the Passing Station and Controller"
)
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Pump Name", description = "Unique Id of the Pump.")
    String id() default "Pump0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Configuration Type", description = "Do you want to Configure and Control the Pump via Devices or Channels")
    ConfigurationType configType() default ConfigurationType.CHANNEL;

    @AttributeDefinition(name = "Pump Type", description = "What Kind of Pump is it?",
    options = {
            @Option(label = "Relay", value = "Relay"),
            @Option(label = "Pwm", value = "Pwm"),
            @Option(label = "Both", value = "Both")
    })
    String pump_Type() default "Both";

    @AttributeDefinition(name =  "BooleanChannel or Id of (Relay) Device", description = "Either the BooleanChannel or the Relay Device.")
    String pump_Relays() default "Relay0/WriteOnOff";

    @AttributeDefinition(name = "PWM Id/ PwmChannel", description = "Either the WriteChannel or the Pwm Device")
    String pump_Pwm() default "PwmDevice0/WritePowerLevel";

    boolean disableOnActivation() default true;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Pump [{id}]";
}
