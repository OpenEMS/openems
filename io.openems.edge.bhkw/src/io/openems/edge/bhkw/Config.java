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
                    @Option(label = "Vitoblock 200 EM - 6/15", value = "EM_6_15"),
                    @Option(label = "Vitoblock 200 EM - 9/20", value = "EM_9_20"),
                    @Option(label = "Vitoblock 200 EM - 20/39", value = "EM_20_39"),
                    @Option(label = "Vitoblock 200 EM - 20/39_70", value = "EM_20_39_70"),
                    @Option(label = "Vitoblock 200 EM - 50/81", value = "EM_50_81"),
                    @Option(label = "Vitoblock 200 EM - 70/115", value = "EM_70_115"),
                    @Option(label = "Vitoblock 200 EM - 100/167", value = "EM_100_167"),
                    @Option(label = "Vitoblock 200 EM - 140/207 SCR", value = "EM_140_207"),
                    @Option(label = "Vitoblock 200 EM - 199/263", value = "EM_199_263"),
                    @Option(label = "Vitoblock 200 EM - 199/293", value = "EM_199_293"),
                    @Option(label = "Vitoblock 200 EM - 238/363", value = "EM_238_363"),
                    @Option(label = "Vitoblock 200 EM - 363/498", value = "EM_363_498"),
                    @Option(label = "Vitoblock 200 EM - 401/549 SCR", value = "EM_401_549"),
                    @Option(label = "Vitoblock 200 EM - 530/660", value = "EM_530_660"),
                    @Option(label = "Vitoblock 200 BM - 36/66", value = "BM_36_66"),
                    @Option(label = "Vitoblock 200 BM - 55/88", value = "BM_55_88"),
                    @Option(label = "Vitoblock 200 BM - 190/238", value = "BM_190_238"),
                    @Option(label = "Vitoblock 200 BM - 366/437", value = "BM_366_437"),
                    @Option(label = "Not in list", value = "Null")
            })
    String bhkwType() default "Vitoblock140";

    @AttributeDefinition(name = "GaspedalId", description = "Id of the Gaspedal you previously implemented")
    String gaspedalId() default "Gaspedal0";

    @AttributeDefinition(name = "min Limit of Bhkw", description = "Minimum of your Bhkw API mA.")
    short minLimit() default 4;

    @AttributeDefinition(name = "max Limit of Bhkw", description = "Maximum of your Bhkw API mA.")
    short maxLimit() default 20;

    @AttributeDefinition(name = "Percentage Range", description = "Where is your percentage range (depending on API) starting: 0-100%(type0) or 50-100% (type 50).")
    int percentageRange() default 0;

    @AttributeDefinition(name = "Position on Board", description = "On what Position is your Bhkw connected with Consolinno Gaspedal?")
    int position() default 0;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Bhkw [{id}]";

}
