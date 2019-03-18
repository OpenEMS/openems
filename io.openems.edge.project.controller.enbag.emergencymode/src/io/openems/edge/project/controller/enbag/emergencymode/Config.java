package io.openems.edge.project.controller.enbag.emergencymode;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Emergency Cluster Mode", //
		description = "Switches the emergency power to next storage system when battery is empty.")
@interface Config {
	String id() default "ctrlEmergencyClusterMode0";

	boolean enabled() default true;

	/*
	 * Solar-Log
	 */
	@AttributeDefinition(name = "PV-Inverter-ID", description = "ID of PV-Inverter device.")
	String pvInverter_id();

	@AttributeDefinition(name = "Sufficient PV Power", description = "PV Power above this value is considered sufficient to supply loads in offgrid.")
	int pvSufficientPower() default 20_000;

	/*
	 * WAGO
	 */
	// Q1 - ess1
	@AttributeDefinition(name = "Q1: Ess 1 supply UPS", description = "Channel address of the Q1 Digital Input/Output that should be switched")
	String q1ChannelAddress();

	// Q2 - ess2
	@AttributeDefinition(name = "Q2: Ess 2 supply UPS", description = "Channel address of the Q2 Digital Input/Output that should be switched")
	String q2ChannelAddress();

	// Q3 - PV Off-Grid
	@AttributeDefinition(name = "Q3: PV in Off-Grid", description = "Channel address of the Q3 Digital Input/Output that should be switched")
	String q3ChannelAddress();

	// Q4 - PV On-Grid
	@AttributeDefinition(name = "Q4: PV in On-Grid", description = "Channel address of the Q4 Digital Input/Output that should be switched")
	String q4ChannelAddress();

	/*
	 * Meters
	 */
	@AttributeDefinition(name = "PV-Meter-ID", description = "ID of the PV-Meter.")
	String pvMeter_id();

	/*
	 * Ess
	 */
	@AttributeDefinition(name = "Ess2-ID", description = "ID of Ess2.")
	String ess2_id();

	@AttributeDefinition(name = "Ess1-ID", description = "ID of Ess1.")
	String ess1_id();

	String webconsole_configurationFactory_nameHint() default "Controller Emergency Cluster Mode [{id}]";
}
