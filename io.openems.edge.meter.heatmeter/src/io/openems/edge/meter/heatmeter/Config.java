package io.openems.edge.meter.heatmeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "HeatMeterMbus",
        description = "A HeatMeter Communicating via MBus."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "HeatMeter-Device ID", description = "Unique Id of the HeatMeter.")
    String id() default "MH-0";

    @AttributeDefinition(name = "alias", description = "Human readable name of the heat-meter.")
    String alias() default "";

    @AttributeDefinition(name = "Model", description = "Identification via model")
    HeatMeterModel model() default HeatMeterModel.ELSTER_SENSOSTAR_2;

    @AttributeDefinition(name = "MBus-Bridge Id", description = "The Unique Id of the mBus-Bridge you what to allocate to this device.")
    String mbusBridgeId() default "mbus0";

    @AttributeDefinition(name = "PrimaryAddress", description = "primary Address of the Mbus Component")
    int primaryAddress();

    @AttributeDefinition(name="Don't poll every second", description = "Turn this on if the meter should not be polled every second.")
    boolean usePollingInterval() default false;

    @AttributeDefinition(name="Polling interval in seconds", description = "If the \"Don't poll every second\" option is turned on, this is the wait time between polling. Unit is seconds.")
    int pollingIntervalSeconds() default 600;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heat-meter Device Id [{id}]";

}