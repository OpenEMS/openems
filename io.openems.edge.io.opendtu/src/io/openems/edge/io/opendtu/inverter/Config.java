package io.openems.edge.io.opendtu.inverter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(name = "Hoymiles Inverter", description = "Configures Hoymiles Inverter via openDTU. If relative and absolute limits are -1 no limits are set via OpenEMS")
@interface Config {

    @AttributeDefinition(name = "ID", description = "Component unique identifier")
    String id() default "io0";

    @AttributeDefinition(name = "Alias", description = "Readable name; defaults to ID")
    String alias() default "";

    @AttributeDefinition(name = "Username", description = "openDTU username")
    String username() default "";

    @AttributeDefinition(name = "Password", description = "openDTU password", type = AttributeType.PASSWORD)
    String password() default "";

    @AttributeDefinition(name = "Enabled", description = "Enable this component?")
    boolean enabled() default true;

    @AttributeDefinition(name = "SN Phase L1", description = "Inverter serial for Phase L1")
    String serialNumberL1() default "";

    @AttributeDefinition(name = "SN Phase L2", description = "Inverter serial for Phase L2")
    String serialNumberL2() default "";

    @AttributeDefinition(name = "SN Phase L3", description = "Inverter serial for Phase L3")
    String serialNumberL3() default "";

    @AttributeDefinition(name = "IP", description = "openDTU IP address")
    String ip();

    @AttributeDefinition(name = "Rel. Power Limit (%)", description = "Initial % power limit/inverter")
    int relativeLimit() default -1;

    @AttributeDefinition(name = "Abs. Power Limit (W)", description = "Initial power limit/inverter in Watts")
    int absoluteLimit() default -1;

    @AttributeDefinition(name = "Threshold", description = "Power change threshold")
    int threshold() default 100;

    @AttributeDefinition(name = "Delay (s)", description = "Delay for setting power limit")
    int delay() default 30;

    @AttributeDefinition(name = "Meter Type", description = "DTU measurement type")
    MeterType type() default MeterType.PRODUCTION;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode?")
    boolean debugMode() default false;

    String webconsole_configurationFactory_nameHint() default "IO openDTU Device [{id}]";
}