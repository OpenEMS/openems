package io.openems.edge.bhkw;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno Gaspedal",
        description = "Depending on VersionId you can activate up to X Devices per Gaspedal"
)

@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Bhkw Name", description = "Name of the Bhkw")
    String id() default "Bhkw0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Bhkw")
    String alias() default "";

    @AttributeDefinition(name = "Bhkw Type", description = "What Bhkw Type do you want to use.",
            options = {
                    @Option(label = "Vitoblock140", value = "bhkw140"),
                    @Option(label = "Vitoblock80", value = "bhkw80")
            })
    String relaisType() default "Vitoblock140";

    @AttributeDefinition(name = "GaspedalId", description = "Id of the Gaspedal you previously implemented")
    String gaspedalId() default "Gaspedal0";

    @AttributeDefinition(name = "Range of min Limit", description = "Is your Bhkw mA starting at 0 or 4")
    short minLimit() default 4;

    @AttributeDefinition(name = "Percentage Range", description = "Is your Bhkw range from 0-100% (type 0) or 50-100%(type 50")
    int percentageRange() default 50;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Bhkw [{id}]";

}
