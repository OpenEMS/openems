package io.openems.edge.consolinno.simulator.communicationmaster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Simulated CommunicationMaster",
        description = "A Simulation of a CommunicationMaster Holding n DecentralHeater, giving them random Enable/Disable Allow to Heat."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Heater-Device ID", description = "Unique Id of the Simulator.")
    String id() default "SimulatedCommunicationMaster0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Relay.")
    String alias() default "";

    @AttributeDefinition(name = "Maximum Requests at once", description = "Maximum decentralHeater requests")
    int maxSize() default 3;

    boolean useHeater() default true;

    @AttributeDefinition(name = "DecentralHeater Ids", description = "Ids of decentralHeater.")
    String[] decentralHeaterIds() default {"DecentralHeater0"};

    boolean useHydraulicLineHeater() default false;

    @AttributeDefinition(name = "HydraulicLineHeater Id", description = "Id of HydraulicLineHeater to test")
    String hydraulicLineHeaterId() default "HydraulicLineHeater0";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Simulated CommunicationMaster Device [{id}]";

}