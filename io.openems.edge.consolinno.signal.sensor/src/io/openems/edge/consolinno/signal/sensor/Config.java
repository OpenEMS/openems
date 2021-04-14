package io.openems.edge.consolinno.signal.sensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Consolinno Modbus Signal Sensor", description = "Signal Sensor that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Signal Sensor.")
    String id() default "SignalSensor0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module();

    @AttributeDefinition(name = "Position", description = "Pinposition of this sensor.")
    int position();

    @AttributeDefinition(name = "SignalType", description = "Is the Signal an Error/Status",
            options = {
                    @Option(label = "Status", value = "Status"),
                    @Option(label = "Error", value = "Error")
            })
    String signalType() default "Status";


    @AttributeDefinition(name = "Inverted Logic", description = "Usually ON signal at T. >100°C--> inverted Logic : Signal on at < 100°C")
    boolean inverted() default false;

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Configurator.")
    int modbusUnitId();

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Configurator.")
    String modbusBridgeId();

    String webconsole_configurationFactory_nameHint() default "Signalsensor [{id}]";
}
