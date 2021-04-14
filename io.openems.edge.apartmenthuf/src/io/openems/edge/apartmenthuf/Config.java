package io.openems.edge.apartmenthuf;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Consolinno Apartment HuF",
        description = "A module to map Modbus calls to OpenEMS channels for a Consolinno Apartment HuF."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "ApartmentHuF-Device ID", description = "Unique Id of the Apartment HuF.")
    String id() default "ApartmentHuF0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of the Apartment HuF.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component. Minimum 6, maximum 63.", min = "6", max = "63")
    int modbusUnitId() default 6;

    @AttributeDefinition(name = "Temperature sensor calibration", description = "Calibration value for the PT1000 temperature sensor.")
    int tempCal() default 70;

    // Debug options.
    @AttributeDefinition(name = "Debug", description = "Debug.")
    boolean debug() default false;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Consolinno Apartment HuF Device [{id}]";

}