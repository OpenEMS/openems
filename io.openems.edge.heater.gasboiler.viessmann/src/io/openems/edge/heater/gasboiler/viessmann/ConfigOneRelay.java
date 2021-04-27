package io.openems.edge.heater.gasboiler.viessmann;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "GasBoiler Viessmann One Relay",
        description = "A Gasboiler provided by Viessmann, communicating via One Relay."
)
@interface ConfigOneRelay {

    String service_pid();

    @AttributeDefinition(name = "GasBoiler-Device ID", description = "Unique Id of the GasBoiler.")
    String id() default "GasBoiler0";

    @AttributeDefinition(name = "alias", description = "Human readable name of GasBoiler.")
    String alias() default "";

    @AttributeDefinition(name = "Relay ID", description = "The Unique Id of the relay you what to allocate to this device.")
    String relayId() default "relay0";

    @AttributeDefinition(name = "Maximum thermical output", description = "Max thermical Output.")
    int maxThermicalOutput() default 1750;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "GasBoiler Device One Relay [{id}]";


}
