package io.openems.edge.consolinno.aio;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Consolinno Modbus Aio", description = "Aio module that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Aio module.")
    String id() default "Aio0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module();

    @AttributeDefinition(name = "Position", description = "Pinposition of this Aio.")
    int position();

    @AttributeDefinition(name = "Type", description = "If this Module is supposed to be 20mA or 10V",
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
    String type();

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Configurator.")
    int modbusUnitId();

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Configurator.")
    String modbusBridgeId();

    String webconsole_configurationFactory_nameHint() default "Aio [{id}]";
}
