package io.openems.edge.controller.io.heatingelement;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;

@ObjectClassDefinition(//
		name = "Controller IO Heating Element", //
		description = "Controls a three-phase heating element via Relays, according to grid active power")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoHeatingElement0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Has Water Thermometer?", description = "Enable the temperature control, the water temperature limits and the Legionella protection using a water thermometer")
	boolean hasWaterThermometer() default false;

	@AttributeDefinition(name = "Water Thermometer-ID", description = "ID of the water Thermometer")
	String waterThermometer_id() default "";

	@AttributeDefinition(name = "Sensor Error Level", description = "Level that is applied if the water thermometer is not available; 'Level 0' switches the heating element off")
	Level sensorErrorLevel() default Level.LEVEL_0;

	@AttributeDefinition(name = "Low Water Temperature", description = "Below this temperature the heating element heats with all phases, even without excess power [°C]")
	int lowWaterTemperature() default 45;

	@AttributeDefinition(name = "High Water Temperature", description = "Above this temperature the heating element is switched off, even with excess power [°C]")
	int highWaterTemperature() default 60;

	@AttributeDefinition(name = "Maximum Water Temperature", description = "Safety limit: the heating element is always switched off above this temperature [°C]")
	int maxWaterTemperature() default 85;

	@AttributeDefinition(name = "Enable Legionella Protection?", description = "Periodically heat the water up to the Legionella temperature")
	boolean enableLegionellenProtection() default false;

	@AttributeDefinition(name = "Legionella Water Temperature", description = "Target water temperature of the Legionella protection [°C]")
	int legionellenWaterTemperature() default 65;

	@AttributeDefinition(name = "Legionella Interval", description = "Maximum number of days between two Legionella heatings")
	int legionellenInterval() default 7;

	@AttributeDefinition(name = "Minimum Legionella Time [h]", description = "Time in hours the water has to stay at or above the Legionella temperature; the Legionella interval is restarted afterwards")
	int minLegionellenTime() default 1;

	@AttributeDefinition(name = "Maximum Legionella Heating Time [h]", description = "Maximum time in hours to reach and hold the Legionella temperature; afterwards the heating is aborted, a warning is set and it is retried after one day")
	int legionellenMaxHeatingTime() default 6;

	@AttributeDefinition(name = "Output Channel Phase L1", description = "Channel address of the Digital Output for Phase L1")
	String outputChannelPhaseL1() default "io0/Relay1";

	@AttributeDefinition(name = "Output Channel Phase L2", description = "Channel address of the Digital Output for Phase L2")
	String outputChannelPhaseL2() default "io0/Relay2";

	@AttributeDefinition(name = "Output Channel Phase L3", description = "Channel address of the Digital Output for Phase L3")
	String outputChannelPhaseL3() default "io0/Relay3";

	@AttributeDefinition(name = "Default Level", description = "This is the default Level in manual mode and for force-heating in automatic mode")
	Level defaultLevel() default Level.LEVEL_1;

	@AttributeDefinition(name = "End Time", description = "End time for minimum run time")
	String endTime() default "17:00";

	@AttributeDefinition(name = "Work-Mode Time or None", description = "Sets the Work-Mode to Time (= run at least Minimum Time), to Energy (= run with a energy limit) or None (only run on excess power)")
	WorkMode workMode() default WorkMode.NONE;

	@AttributeDefinition(name = "Minimum Time [h]", description = "For Work-Mode 'Time': Minimum Time in hours for activating 'Levels'")
	int minTime() default 1;

	@AttributeDefinition(name = "Power per Phase", description = "Power of one single phase of the heating element in [W]")
	int powerPerPhase() default 2000;

	@AttributeDefinition(name = "Minimum switching time between two states", description = "Minimum time (Seconds) is applied to avoid continuous switching on threshold")
	int minimumSwitchingTime() default 60;

	@AttributeDefinition(name = "Minimum energylimit", description = "The minimum Energylimit in [Wh]")
	int minEnergylimit() default 10000;

	@AttributeDefinition(name = "End Time Minimum with meter", description = "End time for the minimum run time with a meter")
	String endTimeWithMeter() default "17:00";

	@AttributeDefinition(name = "Scheduler", description = "Schedule for repeating tasks in JSCalender format.")
	String schedule() default "";

	@AttributeDefinition(name = "Consumption-Meter-ID", description = "ID of the Consumption-Meter.")
	String meter_id() default "";

	String webconsole_configurationFactory_nameHint() default "Controller IO Heating Element [{id}]";
}