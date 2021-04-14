package io.openems.edge.pump.grundfos;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Pump Grundfos", //
        description = "This module maps GENIbus calls to OpenEMS channels for a Grundfos pump. IMPORTANT: This module "
                + "requires \"Bridge GeniBus\" to be active. It won't start if that is not the case!")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "Pump0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "GENIbus-Bridge Id", description = "The Unique Id of the GENIbus-Bridge you want to allocate to this device.")
    String genibusBridgeId() default "genibus0";

    @AttributeDefinition(name = "PumpAddress", description = "Address of the Pump.")
    int pumpAddress() default 231;

    @AttributeDefinition(name = "Flash LED", description = "Continuous flashing of center LED, useful for pump identification.")
    boolean pumpWink() default false;

    @AttributeDefinition(name = "Broadcast", description = "Use this when you don't know the address of the pump. "
            + "This will send a broadcast signal to which any connected pump will respond that is not already communicating "
            + "via GENIbus. If there is more than one pump that could respond, which pump it is will be random. The "
            + "address of the responding pump will be printed to the log, as well as some information about it. The "
            + "broadcast mode should not be used to send commands to the pump.")
    boolean broadcast() default false;

    @AttributeDefinition(name = "Change address", description = "Change the GENIbus address of the pump. Won't work when "
            + "\"broadcast\" is active. You need to restart the pump module and enter the new address after using this, "
            + "so the new address is correctly saved in the config of Apache Felix.")
    boolean changeAddress() default false;

    @AttributeDefinition(name = "New address", description = "A value between 32 and 231 (inclusive).")
    int newAddress() default 231;

    @AttributeDefinition(name = "Multipump system setup", description = "Configure this pump as part of a multipump "
            + "system. After the setup was successful, disable this option. Pump operation is not possible in setup mode.")
    boolean mpSetup() default false;

    @AttributeDefinition(name = "Multipump system end", description = "End the participation in a multipump system.")
    boolean mpEnd() default false;

    @AttributeDefinition(name = "Multipump master", description = "Configure this pump as the multipump master.")
    boolean mpMaster() default false;

    @AttributeDefinition(name = "Multipump master address", description = "For a pump that is not the master, enter the "
            + "address of the master pump.")
    int mpMasterAddress() default 231;

    @AttributeDefinition(name = "Multipump mode", description = "How the multiple pumps work together.")
    TpModeSetting tpMode() default TpModeSetting.TIME_ALTERNATING;

    //@AttributeDefinition(name = "PumpType", description = "Denotation of the Pump.")
    //String pumpType() default "Magna3";

    @AttributeDefinition(name = "Is Magna3?", description = "Is this a Magna3 pump?")
    boolean isMagna3() default false;

    String webconsole_configurationFactory_nameHint()default"Pump Grundfos [{id}]";
}