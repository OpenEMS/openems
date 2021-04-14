package io.openems.edge.rest.remote.device.general;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Rest Remote Device ",
        description = " The Devices you wish to Communicate with. As a Master --> register your Slaves. If Slaves want "
                + "to Communicate with Master tick boolean --> isMaster")
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Unique Id of Device", description = "Id of the Device")
    String id() default "RemoteDevice0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "RestBridge Id", description = "Id of the Rest Bridge you want to communicate with.")
    String restBridgeId() default "RestBridge0";

    @AttributeDefinition(name = "Real Device Id", description = "Id of the device on Master/Slave you want to communicate with.")
    String realDeviceId() default "TemperatureSensor0";

    @AttributeDefinition(name = "Type Selection", description = "What Device Type do you want to Read/Write to"
            + "e.g. TemperatureSensor, Relays etc")
    String deviceType() default "TemperatureSensor";

    @AttributeDefinition(name = "Channel", description = "Channel of the Device you want to read; Remember Only"
            + "Uppercase is: First Letter and the one after a _ e.g. Channel ON_OFF will be OnOff")
    String deviceChannel() default "Temperature";

    @AttributeDefinition(name = "OperationType", description = "Do you want to Read or Write",
            options = {
                    @Option(label = "Read", value = "Read"),
                    @Option(label = "Write", value = "Write")
            }
    )
            String deviceMode() default "Read";

    @AttributeDefinition(name = "Unit", description = "The Unit of the Device")
            String deviceUnit() default "dC";

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Rest Device [{id}]";

}
