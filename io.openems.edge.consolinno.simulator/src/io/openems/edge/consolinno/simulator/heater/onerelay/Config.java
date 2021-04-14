package io.openems.edge.consolinno.simulator.heater.onerelay;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Simulator Heater OneRelay",
        description = "A Simulation of a Heater with one Relay."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Heater-Device ID", description = "Unique Id of the Simulator.")
    String id() default "SimulatedHeater0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Relay.")
    String alias() default "";

    @AttributeDefinition(name = "Relay-Unit Id", description = "Relay ID To Control.")
    String relayId() default "Relay0";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "SimulatedHeater Device [{id}]";

}