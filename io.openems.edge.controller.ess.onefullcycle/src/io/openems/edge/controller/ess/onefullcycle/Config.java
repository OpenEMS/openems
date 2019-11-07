package io.openems.edge.controller.ess.onefullcycle;

import java.time.DayOfWeek;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Controller Ess One Full Cycle", //
        description = "Completes one full cycle for an Ess.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "ctrlOneFullCycle0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
    String ess_id();

    @AttributeDefinition(name = "CycleOrder [Charge/Discharge]", description = "Charge/discharge CycleOrder for Operation")
    CycleOrder cycleorder() default CycleOrder.START_WITH_DISCHARGE;

    @AttributeDefinition(name = " ", description = " ")
    String anyDateTime() default "[{ \"year\" :2019, \"month\" : 9 , \"day\" : 19, \"hour\" : 12,\"minute\" : 06}]";;

    @AttributeDefinition(name = "Is Any Date Time Enabled", description = " Is any date time set enabled ?.")
    boolean isAnyDateTimeEnabled() default true;

    @AttributeDefinition(name = "Choosen First Day In Month ", description = "Choosen First Day In Month [ Ex. Firs Monday of each Month]")
    DayOfWeek dayOfWeek() default DayOfWeek.MONDAY;

    @AttributeDefinition(name = " Start Time in Hour", description = "Start Charge/Discharge Hour (Just Integer in time).")
    int hour() default 8;

    @AttributeDefinition(name = " Is Fixed Date Enabled", description = " Is fix day time enabled ?.")
    boolean isFixedDayTimeEnabled() default false;

    @AttributeDefinition(name = "Power [W]", description = "Charge/discharge power")
    int power();

    String webconsole_configurationFactory_nameHint() default "Controller Ess One Full Cycle [{id}]";
}