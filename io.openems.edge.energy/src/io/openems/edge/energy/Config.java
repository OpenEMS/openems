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

	@AttributeDefinition(name = "ESS Mode: 00-01 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix00() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 01-02 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix01() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 02-03 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix02() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 03-04 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix03() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 04-05 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix04() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 05-06 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix05() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 06-07 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix06() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 07-08 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix07() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 08-09 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix08() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 09-10 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix09() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 10-11 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix10() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 11-12 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix11() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 12-13 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix12() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 13-14 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix13() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 14-15 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix14() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 15-16 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix15() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 16-17 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix16() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 17-18 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix17() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 18-19 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix18() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 19-20 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix19() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 20-21 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix20() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 21-22 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix21() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 22-23 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix22() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "ESS Mode: 23-24 Uhr", description = "")
	io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset essFix23() default io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset.OFF;

	@AttributeDefinition(name = "EVCS Mode: 00-01 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs00() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 01-02 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs01() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 02-03 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs02() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 03-04 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs03() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 04-05 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs04() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 05-06 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs05() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 06-07 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs06() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 07-08 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs07() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 08-09 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs08() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 09-10 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs09() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 10-11 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs10() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 11-12 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs11() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 12-13 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs12() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 13-14 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs13() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 14-15 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs14() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 15-16 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs15() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 16-17 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs16() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 17-18 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs17() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 18-19 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs18() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 19-20 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs19() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 20-21 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs20() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 21-22 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs21() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 22-23 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs22() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	@AttributeDefinition(name = "EVCS Mode: 23-24 Uhr", description = "")
	io.openems.edge.controller.evcs.ScheduleHandler.Preset evcs23() default io.openems.edge.controller.evcs.ScheduleHandler.Preset.FORCE_FAST_CHARGE;

	String webconsole_configurationFactory_nameHint() default "Energy [{id}]";

}