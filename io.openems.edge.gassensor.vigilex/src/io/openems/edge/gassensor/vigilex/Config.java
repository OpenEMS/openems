package io.openems.edge.gassensor.vigilex;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Gas Sensor Vigilex VIGI-DT1", //
		description = "Multi-Gas and Environmental Sensor for BESS. Detects Hydrogen, VOC, Temperature and Humidity via Modbus RTU.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "gasSensor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "H2 RE1 Low Setpoint", //
			description = "Hydrogen RE1 alarm release (reset) threshold. Raw unit: 0.01 %LEL. Valid range: 500…2500 (= 5.00…25.00 %LEL).")
	int hydrogen_re1_low_setpoint() default 500;

	@AttributeDefinition(name = "H2 RE1 High Setpoint", //
			description = "Hydrogen RE1 alarm trigger threshold. Raw unit: 0.01 %LEL. Valid range: 500…2500 (= 5.00…25.00 %LEL). Must be > RE1 Low.")
	int hydrogen_re1_high_setpoint() default 750;

	@AttributeDefinition(name = "H2 RE2 Low Setpoint", //
			description = "Hydrogen RE2 alarm release (reset) threshold. Raw unit: 0.01 %LEL. Valid range: 500…2500 (= 5.00…25.00 %LEL).")
	int hydrogen_re2_low_setpoint() default 1250;

	@AttributeDefinition(name = "H2 RE2 High Setpoint", //
			description = "Hydrogen RE2 alarm trigger threshold. Raw unit: 0.01 %LEL. Valid range: 500…2500 (= 5.00…25.00 %LEL). Must be > RE2 Low.")
	int hydrogen_re2_high_setpoint() default 2000;

	@AttributeDefinition(name = "VOC RE1 Low Setpoint", //
			description = "VOC RE1 alarm release (reset) threshold. Unit: VOC index. Valid range: 150…500.")
	int voc_re1_low_setpoint() default 150;

	@AttributeDefinition(name = "VOC RE1 High Setpoint", //
			description = "VOC RE1 alarm trigger threshold. Unit: VOC index. Valid range: 150…500. Must be > RE1 Low.")
	int voc_re1_high_setpoint() default 200;

	@AttributeDefinition(name = "VOC RE2 Low Setpoint", //
			description = "VOC RE2 alarm release (reset) threshold. Unit: VOC index. Valid range: 150…500.")
	int voc_re2_low_setpoint() default 250;

	@AttributeDefinition(name = "VOC RE2 High Setpoint", //
			description = "VOC RE2 alarm trigger threshold. Unit: VOC index. Valid range: 150…500. Must be > RE2 Low.")
	int voc_re2_high_setpoint() default 400;

	String webconsole_configurationFactory_nameHint() default "Gas Sensor Vigilex VIGI-DT1 [{id}]";
}
