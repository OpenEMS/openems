package io.openems.edge.utility.virtualchannel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Virtual Channel", description = "OpenEmsComponent with VirtualChannel. "
        + "On Runtime create new Channel/variables to use however you want..")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id of the VirtualChannelComponent.")
    String id() default "VirtualChannels";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Component.")
    String alias() default "";

    int numberOfLongChannel() default 0;
    int numberOfDoubleChannel() default 0;
    int numberOfBooleanChannel() default 0;
    int numberOfStringChannel() default 0;

    String[] optionalLongChannelIds() default {};

    String[] optionalDoubleChannelIds() default {};

    String[] optionalBooleanChannelIds() default {};

    String[] optionalStringChannelIds() default {};

    String[] optionalLongChannelValues() default {};

    String[] optionalDoubleChannelValues() default {};

    String[] optionalBooleanChannelValues() default {};

    String[] optionalStringChannelValues() default {};


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Virtual Channel {id}";
}
