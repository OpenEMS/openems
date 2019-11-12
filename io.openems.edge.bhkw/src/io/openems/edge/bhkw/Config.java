package io.openems.edge.bhkw;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Bhkw Vitobloc",
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
                    @Option(label = "Vitoblock 200 EM - 260/390", value = "bhkw260390"),
                    @Option(label = "Vitoblock 200 EM - 199/263", value = "bhkw199263"),
                    @Option(label = "Vitoblock 200 EM - 530/660", value = "bhkw530660"),
                    @Option(label = "Vitoblock 200 EM - 530/660 SCR", value = "bhkw530660S"),
                    @Option(label = "Vitoblock 200 EM - 140/207", value = "bhkw140207"),
                    @Option(label = "Vitoblock 200 EM - 100/167", value = "bhkw100167"),
                    @Option(label = "Vitoblock 200 EM - 401/549", value = "bhkw401549"),
                    @Option(label = "Vitoblock 200 EM - 430/580 SCR", value = "bhkw430580"),
                    @Option(label = "Vitoblock 200 EM - 9/20", value = "bhkw920"),
                    @Option(label = "Vitoblock 200 EM - 6/15", value = "bhkw615"),
                    @Option(label = "Vitoblock 200 EM - 50/81", value = "bhkw5081"),
                    @Option(label = "Vitoblock 200 EM - 20/39", value = "bhkw2039")
            })
    String relaisType() default "Vitoblock140";

    @AttributeDefinition(name = "GaspedalId", description = "Id of the Gaspedal you previously implemented")
    String gaspedalId() default "Gaspedal0";

    @AttributeDefinition(name = "min Limit of Bhkw", description = "Is your Bhkw mA starting at 0 or 4")
    short minLimit() default 4;

    @AttributeDefinition(name = "max Limit of Bhkw", description = "Mostly your Bhkw has a max mA of 20")
    short maxLimit() default 20;

    @AttributeDefinition(name = "Percentage Range", description = "Is your Bhkw range from 0-100% (type 0) or 50-100%(type 50")
    int percentageRange() default 50;

    @AttributeDefinition(name = "Position on Board", description = "On what Position is your Bhkw connected with Gaspedal?")
    int position() default 0;
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Bhkw [{id}]";

}
