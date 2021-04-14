package io.openems.edge.heater.biomassheater.gilles;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "MassHeaterWoodChips Gilles",
        description = "A Massheater by Gilles, using Woodchips. Communicating via Modbus."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "MassHeater-Device ID", description = "Unique Id of the MassHeater.")
    String id() default "WoodChipHeater0";

    @AttributeDefinition(name = "alias", description = "Human readable name of MassHeater.")
    String alias() default "";

    @AttributeDefinition(name = "MassHeater Type", description = "Type for identification.",
            options = {
                    @Option(label = "Placeholder", value = "Placeholder"),
                    @Option(label = "Not in List", value = "Not in List")
            })
    String massHeaterType() default "Not in List";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modBusBridgeId() default "modbus0";

    @AttributeDefinition(name = "Maximum thermical output", description = "Max thermical Output if device not in List")
    int maxThermicalOutput() default 0;

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modBusUnitId() default 0;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "MassHeater - Device [{id}]";

}