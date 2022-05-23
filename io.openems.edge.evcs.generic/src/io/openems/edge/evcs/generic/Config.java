package io.openems.edge.evcs.generic;

import io.openems.edge.evcs.api.GridVoltage;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVCS Generic", description = "Implements a Generic Charging Station.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the EVCS.")
    String id() default "evcs01";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this EVCS.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Charging Priority", description = "Tick if the EVCS should charge with a higher priority.")
    boolean priority() default false;

    @AttributeDefinition(name = "GridVoltage", description = "Voltage of the Power Grid the Evcs is connected to.")
    GridVoltage gridVoltage() default GridVoltage.V_230_HZ_50;

    @AttributeDefinition(name = "Minimum power", description = "Minimum current of the Charger in A.", required = true)
    int minCurrent() default 8;

    @AttributeDefinition(name = "Maximum power", description = "Maximum current of the Charger in A.", required = true)
    int maxCurrent() default 32;

    @AttributeDefinition(name = "Minimum Hardware power", description = "Minimum Hardware current of the Charger in A.", required = true)
    int minHwCurrent() default 8;

    @AttributeDefinition(name = "Maximum Hardware power", description = "Maximum Hardware current of the Charger in A.", required = true)
    int maxHwCurrent() default 32;

    @AttributeDefinition(name = "Phases", description = "If the Phases are physically swapped, change the order here.", required = true)
    int[] phases() default {1,2,3};

    @AttributeDefinition(name = "write Scale Factor", description = "The scale Factor of the charge Limit (default = 1A)", required = true)
    int writeScaleFactor() default 1;

    @AttributeDefinition(name = "read Scale Factor", description = "The scale Factor of the Power that is currently charging (default = 1W)", required = true)
    int readScaleFactor() default 1;

    @AttributeDefinition(name = "Status Register", description = "Number of the Status Register (leave empty if none exist).")
    int statusRegister();

    @AttributeDefinition(name = "PowerRead Register", description = "Number of the Power Read Register (leave empty if none exist).")
    int powerReadRegister();

    @AttributeDefinition(name = "L1 Register", description = "Number of the L1 Read Register.")
    int l1Register();

    @AttributeDefinition(name = "L2 Register", description = "Number of the L2 Read Register.")
    int l2Register();

    @AttributeDefinition(name = "L3 Register", description = "Number of the L3 Read Register.")
    int l3Register();

    @AttributeDefinition(name = "MaxCurrent Register", description = "Number of the Max Current (Limit) Write Register.")
    int maxCurrentRegister();

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId() default 255;

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge.")
    String modbusBridgeId() default "modbusBridge01";

    String webconsole_configurationFactory_nameHint() default "Evcs Generic [{id}]";
}
