package io.openems.edge.energy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Energy", //
		description = "")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "energy0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	// @AttributeDefinition(name = "ESS Mode: 00-01 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix00() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 01-02 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix01() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 02-03 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix02() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 03-04 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix03() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 04-05 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix04() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 05-06 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix05() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 06-07 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix06() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 07-08 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix07() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 08-09 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix08() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 09-10 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix09() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 10-11 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix10() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 11-12 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix11() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 12-13 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix12() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 13-14 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix13() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 14-15 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix14() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 15-16 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix15() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 16-17 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix16() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 17-18 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix17() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 18-19 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix18() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 19-20 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix19() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 20-21 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix20() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 21-22 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix21() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 22-23 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix22() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "ESS Mode: 23-24 Uhr", description = "")
	// EssFixActivePower.ScheduleMode essFix23() default
	// EssFixActivePower.ScheduleMode.OFF;
	//
	// @AttributeDefinition(name = "EVCS Mode: 00-01 Uhr", description = "")
	// EvcsController.ScheduleMode evcs00() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 01-02 Uhr", description = "")
	// EvcsController.ScheduleMode evcs01() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 02-03 Uhr", description = "")
	// EvcsController.ScheduleMode evcs02() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 03-04 Uhr", description = "")
	// EvcsController.ScheduleMode evcs03() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 04-05 Uhr", description = "")
	// EvcsController.ScheduleMode evcs04() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 05-06 Uhr", description = "")
	// EvcsController.ScheduleMode evcs05() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 06-07 Uhr", description = "")
	// EvcsController.ScheduleMode evcs06() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 07-08 Uhr", description = "")
	// EvcsController.ScheduleMode evcs07() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 08-09 Uhr", description = "")
	// EvcsController.ScheduleMode evcs08() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 09-10 Uhr", description = "")
	// EvcsController.ScheduleMode evcs09() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 10-11 Uhr", description = "")
	// EvcsController.ScheduleMode evcs10() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 11-12 Uhr", description = "")
	// EvcsController.ScheduleMode evcs11() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 12-13 Uhr", description = "")
	// EvcsController.ScheduleMode evcs12() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 13-14 Uhr", description = "")
	// EvcsController.ScheduleMode evcs13() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 14-15 Uhr", description = "")
	// EvcsController.ScheduleMode evcs14() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 15-16 Uhr", description = "")
	// EvcsController.ScheduleMode evcs15() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 16-17 Uhr", description = "")
	// EvcsController.ScheduleMode evcs16() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 17-18 Uhr", description = "")
	// EvcsController.ScheduleMode evcs17() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 18-19 Uhr", description = "")
	// EvcsController.ScheduleMode evcs18() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 19-20 Uhr", description = "")
	// EvcsController.ScheduleMode evcs19() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 20-21 Uhr", description = "")
	// EvcsController.ScheduleMode evcs20() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 21-22 Uhr", description = "")
	// EvcsController.ScheduleMode evcs21() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 22-23 Uhr", description = "")
	// EvcsController.ScheduleMode evcs22() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;
	//
	// @AttributeDefinition(name = "EVCS Mode: 23-24 Uhr", description = "")
	// EvcsController.ScheduleMode evcs23() default
	// EvcsController.ScheduleMode.FORCE_FAST_CHARGE;

	String webconsole_configurationFactory_nameHint() default "Energy [{id}]";

}