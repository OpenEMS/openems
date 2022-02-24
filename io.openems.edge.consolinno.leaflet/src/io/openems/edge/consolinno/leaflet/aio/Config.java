package io.openems.edge.consolinno.leaflet.aio;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbus Aio", description = "Implementation of the Aio module that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Aio module.")
    String id() default "Aio0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module() default 5;

    @AttributeDefinition(name = "Position", description = "PinPosition of this Aio.")
    int position() default 1;

    @AttributeDefinition(name = "debugValue", description = "If set to true, write the Value below to the AIO Module to check if it's working.")
    boolean debugValue() default false;

    @AttributeDefinition(name = "Value", description = "Output Value for the specified Configuration.")
    int value() default 1000;

    @AttributeDefinition(name = "Type", description = "What should the AIO do at this specific Connection",
            options = {
                    @Option(label = "10V OUT", value = "10V_out"),
                    @Option(label = "10V IN", value = "10V_in"),
                    @Option(label = "0-20mA OUT", value = "0-20mA_out"),
                    @Option(label = "0-20mA IN", value = "0-20mA_in"),
                    @Option(label = "4-20mA OUT", value = "4-20mA_out"),
                    @Option(label = "4-20mA IN", value = "4-20mA_in"),
                    @Option(label = "Temperature IN", value = "Temperature_in"),
                    @Option(label = "Digital IN", value = "Digital_in"),

            })
    String type() default "10V_out";

    @AttributeDefinition(name = "LeafletId", description = "Unique Id of the LeafletCore, this Module is attached to.")
    String leafletId() default "LeafletCore";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Configurator.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Configurator.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "Aio Device [{id}]";
}
