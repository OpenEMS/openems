package io.openems.edge.controller.highloadtimeslot;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller high load timeslot", //
		description = "This controller discharges the storages ystem at a defined time with a defined load")
@interface Config {
	String service_pid();

	String id() default "ctrlHighLoadTimeslot0";

	boolean enabled() default true;
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess();
		
	@AttributeDefinition(name = "Startdate", description = "for example: 30.12.1998")
	String startdate();
	
	@AttributeDefinition(name = "Enddate", description = "for example: 31.12.1998")
	String enddate();
	
	@AttributeDefinition(name = "Daily Starttime", description = "for example: 06:46")
	String starttime();
	
	@AttributeDefinition(name = "Daily Endtime", description = "for example: 17:05")
	String endtime();
	
	@AttributeDefinition(name = "Charge Power", description = "Charge power per ess in Watt and neg. values for example: -10000")
	int chargePower();
	
	@AttributeDefinition(name = "Discharge Power", description = "Charge power per ess in Watt and positive values for example: 29000")
	int dischargePower();
	
	@AttributeDefinition(name = "Min SoC", description = "Discharges the ess until the minimal soc has been reached")
	int minSoc();
	
	@AttributeDefinition(name = "Hysteresis SoC", description = "Controller charges the ess until it is full, charging is started again when hysteresis soc is reached")
	int hysteresisSoc();
	
	String webconsole_configurationFactory_nameHint() default "Controller HighLoadTimeslot [{id}]";
}