package io.openems.edge.meter.watermeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Meter Water M-Bus",
        description = "Implements a water meter communicating via M-Bus.")
@interface ConfigMbus {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "watermeter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "M-Bus bridge ID", description = "ID of the M-Bus bridge this device should use.")
    String mbusBridgeId() default "mbus0";

    @AttributeDefinition(name = "M-Bus PrimaryAddress", description = "PrimaryAddress of the M-Bus device.")
    int primaryAddress();

    @AttributeDefinition(name = "Model", description = "Choose the meter model. This selects at which record positions to " +
            "look for the data in the transmission. \"Autosearch record position\" should work for most models.",
            options = {
                    @Option(label = "Autosearch record position", value = "Autosearch"),
                    @Option(label = "Relay PadPuls M2", value = "PAD_PULS_M2"),
                    @Option(label = "Itron BM +m", value = "ITRON_BM_M"),
                    @Option(label = "Manual record position", value = "Manual"),
            })
    String model() default "Autosearch";

    @AttributeDefinition(name = "Manual record position: Volume", description = "If \"Manual record position\" is selected: Record position of metered volume in M-Bus Telegram.")
    int volAddress() default 0;

    @AttributeDefinition(name = "Manual record position: Timestamp", description = "If \"Manual record position\" is selected: Record position of timestamp in M-Bus Telegram.")
    int timeStampAddress() default 1;

    @AttributeDefinition(name = "OpenEMS Timestamp", description = "Let OpenEMS create the timestamp instead of reading it from the meter. Enable this if the meter does not have a timestamp.")
    boolean openEmsTimeStamp() default false;

    @AttributeDefinition(name="Don't poll every second", description = "Turn this on if the meter should not be polled every second.")
    boolean usePollingInterval() default false;

    @AttributeDefinition(name="Polling interval in seconds", description = "If the \"Don't poll every second\" option is turned on, this is the wait time between polling. Unit is seconds.")
    int pollingIntervalSeconds() default 600;

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Water meter M-Bus [{id}]";

}
