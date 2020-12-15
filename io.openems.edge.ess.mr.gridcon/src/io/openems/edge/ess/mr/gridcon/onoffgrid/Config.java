package io.openems.edge.ess.mr.gridcon.onoffgrid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

@ObjectClassDefinition(//
		name = "MR Gridcon ESS On-Off-Grid", //
		description = "ESS MR Gridcon PCS on off grid variant" //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Gridcon-ID", description = "ID of Gridcon.")
	String gridcon_id() default "gridcon0";

	@AttributeDefinition(name = "Battery-A-ID", description = "ID of Battery A.")
	String bms_a_id() default "bms0";

	@AttributeDefinition(name = "Battery-B-ID", description = "ID of Battery B.")
	String bms_b_id() default "";

	@AttributeDefinition(name = "Battery-C-ID", description = "ID of Battery C.")
	String bms_c_id() default "";

	@AttributeDefinition(name = "Meter ID", description = "ID of Meter.")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "Enable IPU 1", description = "IPU 1 is enabled")
	boolean enableIpu1() default true;

	@AttributeDefinition(name = "Enable IPU 2", description = "IPU 2 is enabled")
	boolean enableIpu2() default false;

	@AttributeDefinition(name = "Enable IPU 3", description = "IPU 3 is enabled")
	boolean enableIpu3() default false;

	@AttributeDefinition(name = "Offset Current", description = "An offset current is put on the rack with the highest cell voltage")
	float offsetCurrent() default 0;

	@AttributeDefinition(name = "Parameter Set", description = "Parameter Set")
	ParameterSet parameterSet() default ParameterSet.SET_1;

	@AttributeDefinition(name = "Input NA 1", description = "Input for NA Protection 1")
	String inputNaProtection1() default "io0/DigitalInputM1C1";

	@AttributeDefinition(name = "Invert Input NA 1", description = "Flag if digital input NA 1 is inverted")
	boolean isNaProtection1Inverted() default false;

	@AttributeDefinition(name = "Input NA 2", description = "Input for NA Protection 2")
	String inputNaProtection2() default "io0/DigitalInputM1C2";

	@AttributeDefinition(name = "Invert Input NA 2", description = "Flag if digital input NA 2 is inverted")
	boolean isNaProtection2Inverted() default false;

	@AttributeDefinition(name = "Input Sync Device Bridge", description = "Input for sync device bridge")
	String inputSyncDeviceBridge() default "io0/DigitalInputM2C1";

	@AttributeDefinition(name = "Invert Input Sync Device Bridge", description = "Flag if digital input sync device bridge is inverted")
	boolean isInputSyncDeviceBridgeInverted() default false;

	@AttributeDefinition(name = "Output Sync Device Bridge", description = "Output for sync device bridge")
	String outputSyncDeviceBridge() default "io0/DigitalOutputM1C1";

	@AttributeDefinition(name = "Invert Output Sync Device Bridge", description = "Flag if digital output sync device bridge is inverted")
	boolean isOutputSyncDeviceBridgeInverted() default false;

	@AttributeDefinition(name = "Output Gridcon Hard Reset", description = "Output for hard reset for gridcon")
	String outputHardReset() default "io0/DigitalOutputM1C2";

	@AttributeDefinition(name = "Invert Output Hard Reset", description = "Flag if digital output for hard reset is inverted")
	boolean isOutputHardResetInverted() default false;

	@AttributeDefinition(name = "Target Frequency On Grid", description = "Target frequency in on grid mode")
	float targetFrequencyOnGrid() default 52.7f;

	@AttributeDefinition(name = "Target Frequency Off Grid", description = "Target frequency in off grid mode")
	float targetFrequencyOffGrid() default 50.6f;

	@AttributeDefinition(name = "Delta Frequency For Sync", description = "Delta Frequency For Sync")
	float deltaFrequency() default 0.2f;

	@AttributeDefinition(name = "Delta Voltage For Sync", description = "Delta Voltage For Sync")
	float deltaVoltage() default 5.0f;

	String webconsole_configurationFactory_nameHint() default "MR Gridcon ESS On-Off-Grid [{id}]";
}