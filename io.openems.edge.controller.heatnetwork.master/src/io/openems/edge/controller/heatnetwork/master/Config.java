package io.openems.edge.controller.heatnetwork.master;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Heatnetwork Master",
        description = "A Controller getting RemoteDevices (Heating Network) to see if a Heatdemand is there."
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "HeatnetworkMaster-ID", description = "ID of HeatnetworkMaster.")
    String id() default "HeatNetworkMaster0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "GLT-Controller";

    @AttributeDefinition(name = "HeatTankRequestRemoteDevices", description = "RemoteDevices that are allocated to a HeatingTank")
    String[] requests() default {"RestRemoteDevice0"};

    @AttributeDefinition(name = "HeatTankNetworkReady Response", description = "RemoteDevices that are allocated to a HeatingTank --> Network Ready")
    String[] readyResponse() default {"RestRemoteDevice1"};

    @AttributeDefinition(name = "Heatnetwork Heater Id", description = "Allocated Controller Id")
    String allocatedController() default "ControlCenter0";

    @AttributeDefinition(name = "SetPointTemperature", description = "Temperature to be set when HeatingRequest is incoming: Unit is dC.")
            int temperatureSetPoint() default 800;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heatnetwork Master [{id}]";
}