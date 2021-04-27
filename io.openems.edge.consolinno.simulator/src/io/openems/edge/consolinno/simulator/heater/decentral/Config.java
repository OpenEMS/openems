package io.openems.edge.consolinno.simulator.heater.decentral;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Simulated Heater Decentral",
        description = "A Simulation of Multiple DecentralHeater. This is to check if the CommunicationMaster can Handle multiple HeatRequests."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Heater-Device ID", description = "Unique Id of the Simulator.")
    String id() default "SimulatedHeaterDecentral0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Relay.")
    String alias() default "";

    @AttributeDefinition(name = "ChannelToControlForCheck", description = "If Any Channel Given -> Write true/false if Decentral is enabled")
    String[] channelToWrite() default {"Relays9/OnOff", "Relays10/OnOff", "Relays11/OnOff", "Relays12/OnOff", "Relays13/OnOff", "Relays14/OnOff"};


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Simulated Heater Decentral [{id}]";

}