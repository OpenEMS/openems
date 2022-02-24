package io.openems.edge.consolinno.leaflet.pwm;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbus Pwm", description = "The Implementation of PWM Module or rather one Connection to the PWM Module,"
        + " that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Pwm.")
    String id() default "Pwm0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module() default 1;

    @AttributeDefinition(name = "Position", description = "PinPosition of this Pwm.")
    int position() default 1;

    @AttributeDefinition(name = "Default Percent", description = "Writes a default Percent Value to the device on Activation.")
    boolean useDefaultPercent() default false;

    @AttributeDefinition(name = "Default Percent value", description = "Pwm Output in Percent .")
    float percent() default 100.f;

    @AttributeDefinition(name = "Inverted", description = "Tick if Pwm is supposed to be inverted == Device reacts to Low instead of High Flank..")
    boolean isInverted() default false;

    @AttributeDefinition(name = "LeafletId", description = "Unique Id of the LeafletCore, this Module is attached to.")
    String leafletId() default "LeafletCore";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Core.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Core.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "Pwm Device [{id}]";
}
