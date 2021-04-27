package io.openems.edge.heater.gasboiler.viessmann;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "GasBoiler Viessmann",
        description = "A Gasboiler provided by Viessmann, communicating via ModbusTCP."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "GasBoiler-Device ID", description = "Unique Id of the GasBoiler.")
    String id() default "GasBoiler0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "GasBoiler Type", description = "Select used Gasboiler.",
            options = {
                    @Option(label = "VITOTRONIC_100", value = "VITOTRONIC_100"),
                    @Option(label = "Placeholder2", value = "Placeholder2"),
                    @Option(label = "Not in List", value = "Not in List")
            })
    String gasBoilerType() default "VITOTRONIC_100";

    @AttributeDefinition(name = "Maximum thermical output", description = "Max thermical Output.")
    int maxThermicalOutput() default 1750;

    @AttributeDefinition(name = "alias", description = "Human readable name of GasBoiler.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "GasBoiler Device [{id}]";

}