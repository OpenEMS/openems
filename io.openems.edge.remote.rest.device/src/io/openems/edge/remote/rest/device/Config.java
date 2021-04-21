package io.openems.edge.remote.rest.device;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Rest Remote Device ",
        description = "The Devices you wish to Communicate with.")
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Unique Id of Device", description = "Id of the Device")
    String id() default "RemoteDevice0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "RestBridge Id", description = "The Corresponding RestBridge.")
    String restBridgeId() default "RestBridge0";

    @AttributeDefinition(name = "Real Device Id", description = "Id of the (remote) device you want to communicate with.")
    String realDeviceId() default "TemperatureSensor0";

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

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Rest Device [{id}]";

}
