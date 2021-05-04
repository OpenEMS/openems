package io.openems.edge.heatsystem.components.valve;

import io.openems.edge.heatsystem.components.ConfigurationType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Valve Two Relays",
        description = "A valve controlled by 2 relays."
)
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Valve Name", description = "Unique Id of the Valve.")
    String id() default "Valve0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Configuration Type", description = "Select either Control by Channel or by Devicetype (Relay only supported).")
    ConfigurationType configurationType() default ConfigurationType.CHANNEL;

    @AttributeDefinition(name = "Closing Channel or Device", description = "What channel to write True/False if Valve should close OR Device. Depends on configurationType.")
    String close() default "Relays0/WriteOnOff";

    @AttributeDefinition(name = "Opening Channel or Device", description = "What channel to write True/False if Valve should close OR Device. Depends on configurationType.")
    String open() default "Relays1/WriteOnOff";

    @AttributeDefinition(name = "Valve Time", description = "The time needed to Open and Close the valve (t in seconds).")
    int valve_Time() default 30;

    @AttributeDefinition(name = "Should Close on Activation", description = "Should the Valve Close completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldCloseOnActivation() default true;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Valve Two Relays [{id}]";
}
