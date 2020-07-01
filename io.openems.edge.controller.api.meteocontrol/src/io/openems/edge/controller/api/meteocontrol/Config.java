package io.openems.edge.controller.api.meteocontrol;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Meteo Control", //
		description = "This Controller sends Data of selected components to Meteo Control Server via their VCOM Api")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlMeteoControl0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Host", description = "The host url of the meteo control Api (http or https)")
	String host() default "https://mii.meteocontrol.de";
	
	@AttributeDefinition(name = "API Key", description = "The Apikey for meteo control")
	String apikey() default "";
	
	@AttributeDefinition(name = "Serial", description = "The serial of this Meteo Control client")
	String serial() default "";
	
	@AttributeDefinition(name = "Measurement Intervall", description = "The interval in seconds for data measurements", type = AttributeType.INTEGER, min = "300")
	int mInterval() default 300;
	
	@AttributeDefinition(name = "Transfer Intervall", description = "The interval in minutes for transfering data measurements (minimum 5 minutes)", type = AttributeType.INTEGER, min = "5")
	int tInterval() default 5;
	
	@AttributeDefinition(name = "Ess Id", description = "The Id of the Ess component")
	String essId() default "ess0";
	
	@AttributeDefinition(name = "Inverter/PV Id", description = "The Id of the Inverter/Pv component")
	String pvInverter() default "pvInverter0";
	
	@AttributeDefinition(name = "meter Id", description = "The Id of the meter component")
	String meter() default "meter0";
	
	

	String webconsole_configurationFactory_nameHint() default "Controller Meteo Control [{id}]";

}