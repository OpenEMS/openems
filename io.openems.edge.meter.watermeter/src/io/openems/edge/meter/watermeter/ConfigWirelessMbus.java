package io.openems.edge.meter.watermeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Meter Water Wireless M-Bus",
        description = "Implements a water meter communicating via Wireless M-Bus.")
@interface ConfigWirelessMbus {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "watermeter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Wireless M-Bus bridge ID", description = "ID of the Wireless M-Bus bridge this device should use.")
    String wmbusBridgeId() default "wmbus0";

    @AttributeDefinition(name = "Wireless M-Bus radio address", description = "Radio address of the WM-Bus device.")
    String radioAddress() default "";

    @AttributeDefinition(name = "Model", description = "Choose the meter model. This selects at which record positions to " +
            "look for the data in the transmission. \"Autosearch record position\" should work for most models.",
            options = {
                    @Option(label = "Autosearch record position", value = "Autosearch"),
                    @Option(label = "Relay PadPuls M2W Channel 1", value = "Relay PadPuls M2W Channel 1"),
                    @Option(label = "Relay PadPuls M2W Channel 2", value = "Relay PadPuls M2W Channel 2"),
                    @Option(label = "Engelmann Waterstar M", value = "Engelmann Waterstar M"),
                    @Option(label = "Manual record position", value = "Manual"),
            })
    String model() default "Autosearch";

    @AttributeDefinition(name = "Manual record position: Volume", description = "If \"Manual record position\" is selected: Record position of metered volume in WM-Bus Telegram.")
    int volAddress() default 0;

    @AttributeDefinition(name = "Manual record position: Timestamp", description = "If \"Manual record position\" is selected: Record position of timestamp in WM-Bus Telegram.")
    int timeStampAddress() default 1;

    @AttributeDefinition(name = "OpenEMS Timestamp", description = "Let OpenEMS create the timestamp instead of reading it from the meter. Enable this if the meter does not have a timestamp.")
    boolean openEmsTimeStamp() default false;

    @AttributeDefinition(name = "Decryption key", description = "Decryption key of the WM-Bus telegram. Leave empty if not encrypted.", type = AttributeType.PASSWORD)
    String key() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Water meter Wireless M-Bus [{id}]";

}