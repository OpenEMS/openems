package io.openems.edge.controller.heatnetwork.controlcenter.api;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Dummy ControlCenter",
        description = "A Controller getting RemoteDevices (Heating Network) to see if a Heatdemand is there."
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "HeatnetworkMaster-ID", description = "ID of HeatnetworkMaster.")
    String id() default "ControlCenter0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "Controlcenter";

    @AttributeDefinition(name = "Relays Test", description = "Activates this Relays")
            String relay() default "Relays7";

    boolean enabled() default true;



    String webconsole_configurationFactory_nameHint() default "Controllcenter [{id}]";
}