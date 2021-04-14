package io.openems.edge.controller.heatnetwork.valve.staticvalve;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Controller Valve Static Position", description = "Static Valve Controller: Has a Valve and sets by Temperature:Value Mapping")
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for this Controller.")
    String id() default "StaticValveController0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Controller.")
    String alias() default "";

    @AttributeDefinition(name = "ValveId", description = "The ValveId of the Valve you want to Control")
    String valveToControl() default "Valve0";

    @AttributeDefinition(name = "temperatureToPosition", description = "Map in each entry the Temperature to the Position, "
            + "e.g. at 700dC set Valve to 20%, at 500dc to 70% etc")
    String[] temperaturePositionMap() default {"700:20"};

    @AttributeDefinition(name = "Control by", description = "Control this valve by Position or Temperature", options = {
            @Option(label = "Position", value = "Position"),
            @Option(label = "Temperature", value = "Temperature")
    })
    String controlType() default "Temperature";

    @AttributeDefinition(name = "ThermometerId", description = "The Thermometer you want to use for Temperature the ValveController reacts to")
    String thermometerId() default "TemperatureThreshold0";

    @AttributeDefinition(name = "autorun", description = "Should the Controller start automatically/sets it's own enable_signal")
    boolean autorun() default false;

    @AttributeDefinition(name = "Allow Force Open/Close", description = "Should calling Components be able to Allow Force Close/Open")
    boolean allowForcing() default false;

    @AttributeDefinition(name = "Close When No SignalEnable", description = "Should the Valve close when there is neither autorun or EnableSignal")
    boolean shouldCloseWhenNoSignal() default true;

    @AttributeDefinition(name = "Default ValvePosition on Activation", description = "If config temperatures are beneath every setPoint/no positionMap is given: set this to default")
    int defaultPosition() default 100;

    @AttributeDefinition(name = "use Fallback", description = "If Enabled Signal isn't arriving (due to errors), activate the ValveController after 10 Cycles for a Duration of 60 seconds")
    boolean useFallback() default true;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Valve Static Position [{id}]";
}
