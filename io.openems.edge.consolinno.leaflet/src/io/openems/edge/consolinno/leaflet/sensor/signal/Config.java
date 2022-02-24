package io.openems.edge.consolinno.leaflet.sensor.signal;

import io.openems.edge.consolinno.leaflet.sensor.signal.api.SignalType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbus Signal Sensor", description = "Signal Sensor, connected to the TemperatureModule, that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Signal Sensor.")
    String id() default "SignalSensor0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module() default 1;

    @AttributeDefinition(name = "Position", description = "PinPosition of this sensor.")
    int position() default 1;

    @AttributeDefinition(name = "SignalType", description = "Is the Signal an Error/Status")
    SignalType signalType() default SignalType.STATUS;


    @AttributeDefinition(name = "Inverted Logic", description = "A SignalSensor uses an inverted Logic (SignalActive==true) when the Analogue Signal is Missing!")
    boolean inverted() default false;

    @AttributeDefinition(name = "LeafletId", description = "Unique Id of the LeafletCore, this Module is attached to.")
    String leafletId() default "LeafletCore";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Core.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Core.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "SignalSensor [{id}]";
}
