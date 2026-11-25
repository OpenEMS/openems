package io.openems.edge.aircooling.seifert;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Air Cooling Seifert", //
		description = "Implements the air cooling Seifert")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "airCooling0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId();

	@AttributeDefinition(name = "Cooling setpoint", description = "Cooling setpoint in 0.1 °C (scale factor 10), e.g. 315 = 31.5 °C")
	int coolingSetpoint();

	@AttributeDefinition(name = "Heating setpoint", description = "Heating setpoint in 0.1 °C (scale factor 10), e.g. 150 = 15.0 °C")
	int heatingSetpoint();

	@AttributeDefinition(name = "High temperature alarm setpoint", description = "Alarm high setpoint in 0.1 °C (scale factor 10), e.g. 450 = 45.0 °C")
	int highTempAlarmSetpoint();

	@AttributeDefinition(name = "Low temperature alarm setpoint", description = "Alarm low setpoint in 0.1 °C (scale factor 10), e.g. 50 = 5.0 °C")
	int lowTempAlarmSetpoint();

	String webconsole_configurationFactory_nameHint() default "Air Cooling Seifert [{id}]";

}