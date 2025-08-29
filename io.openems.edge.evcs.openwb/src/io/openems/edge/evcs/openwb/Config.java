package io.openems.edge.evcs.openwb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;



@ObjectClassDefinition(name = "Evcs OpenWB", //
		description = "Implements the evcs component for OpenWB Series2 with internal chargepoints via HTTP API")
@interface Config {
	
	   enum ChargePoint{
	       CP0(0), CP1(1);
	       private int value;
	       private ChargePoint(int value) {
	            this.value = value;
	       }
	       public int getValue(){
	        return value;
	       }
	   }

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcsOpenWB0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the OpenWB.")
	String ipAddress();

	@AttributeDefinition(name = "Port", description = "Port of the OpenWB")
	int port() default 8443;

	@AttributeDefinition(name = "Chargepoint", description = "Number of the internal chargepoint.")
	ChargePoint chargePoint() default ChargePoint.CP0;

	String webconsole_configurationFactory_nameHint() default "Evcs OpenWB[{id}]";

}