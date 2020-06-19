package io.openems.edge.controller.api.meteocontrol;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Meteo Control", //
		description = "This Controller sends Data of selected components to Meteo Control Server via their REST Api")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlMeteoControl0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Host", description = "The host url of the meteo control Api (http or https)")
	String host() default "https://ws.meteocontrol.de";
	
	@AttributeDefinition(name = "Serial", description = "The serial of this Meteo Control client")
	String serial() default "";
	
	@AttributeDefinition(name = "User Name", description = "The user name of this client account")
	String user() default "";
	
	@AttributeDefinition(name = "Password", description = "The password for this client account",  type = AttributeType.PASSWORD)
	String password() default "";
	
	@AttributeDefinition(name = "Measurement Intervall", description = "The interval in seconds for data measurements")
	int mInterval() default 900;
	
	@AttributeDefinition(name = "Transfer Intervall", description = "The interval in hours for transfering data measurements (minimum 1 hrs)", type = AttributeType.INTEGER, min = "1")
	int tInterval() default 1;
	
	@AttributeDefinition(name = "Ess Id", description = "The Id of the Ess component")
	String essId() default "ess0";
	
	@AttributeDefinition(name = "Inverter/PV Id", description = "The Id of the Inverter/Pv component")
	String pvInverter() default "pvInverter0";
	
	@AttributeDefinition(name = "meter Id", description = "The Id of the meter component")
	String meter() default "meter0";
	
	

	String webconsole_configurationFactory_nameHint() default "Controller Meteo Control [{id}]";

}