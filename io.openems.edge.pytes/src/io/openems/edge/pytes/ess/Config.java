package io.openems.edge.pytes.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.pytes.enums.EnableDisable;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;
import io.openems.edge.pytes.enums.WorkMode;

@ObjectClassDefinition(//
		name = "Pytes Hybrid Inverter", //
		description = "Pytes Hybrid Inverter")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "WorkMode", description = "Work Mode. ExternalMode -> device is controlled by OpenEMS")
	WorkMode workMode() default WorkMode.EXTERNAL;

	@AttributeDefinition(name = "ESS SetPoint", description = "How the OpenEMS AC set-point is applied (see readme). BATTERY_CONTROL (reg 44105 = 2, default and recommended): the EMS commands the battery power (set-point - PV, with bias/loss compensation and trim); the PV passes through, 'battery 0 W' exports everything the PV has, no PV curtailment, no battery transients at clouds, grid-point accuracy +-50-100 W - for self-consumption, limiters and PV pass-through. AC_OUTPUT_CONTROL (44105 = 4): the inverter holds its AC output at the set-point and curtails PV above 'set-point + battery charging' without reporting it; with a full battery the measured PV then only reflects the set-point (surplus search workaround), clouds are bridged from the battery until the set-point follows, OpenEMS battery limits are not known to the inverter; grid-point accuracy +-20 W - only for peak shaving / exact grid targets.")
	RemoteDispatchRealtimeControlSwitch essSetpoint() default RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL;

	@AttributeDefinition(name = "Max. Apparent Power", description = "Inverter´s apparent power limit")
	int maxApparentPower() default 10000;

	@AttributeDefinition(name = "Max. battery charge current [A]", description = "Additional EMS-side upper limit for the battery charge current. The smallest of this value, the BMS limit and the inverter's own setting (reg 43117, app 'max. charge current') is used. Only enforceable while the inverter follows the EMS set-point - with an active grid feed-in limit the inverter charges autonomously up to its own setting, so do not configure a value below the inverter setting.")
	int maxChargeCurrent() default 40;

	@AttributeDefinition(name = "Max. battery discharge current [A]", description = "Additional EMS-side upper limit for the battery discharge current. The smallest of this value, the BMS limit and the inverter's own setting (reg 43118, app 'max. discharge current') is used. Only enforceable while the inverter follows the EMS set-point.")
	int maxDischargeCurrent() default 40;

	@AttributeDefinition(name = "Battery capacity [Wh]", description = "Nominal battery capacity, e.g. 100 Ah x 51.2 V = 5120 Wh. The BMS does not report it via the inverter (BMS extension block 34345-34364 is empty, reg 43387 is a fixed 3.0 kWh placeholder). 0 = unknown.")
	int capacity() default 0;

	@AttributeDefinition(name = "Min SoC [5-100%]", description = "NOT applied: the SoC limits configured in the inverter are authoritative. Kept for a possible opt-in (see PytesJs3Impl.setDefaultValues()).")
	int minSoc() default 10;

	@AttributeDefinition(name = "Enable Backup Port", description = "NOT applied: the backup port setting configured in the inverter is authoritative. Kept for a possible opt-in (see PytesJs3Impl.setDefaultValues()).")
	boolean enableBackupPort() default true;

	@AttributeDefinition(name = "Failsafe timeout [min]", description = "Minutes without an EMS write after which the inverter falls back to its own self-use mode (reg 44101, 1-1440). Keep it short if loads on the backup port depend on the EMS.")
	int failsafeMinutes() default 5;

	@AttributeDefinition(name = "Feed-in limitation", description = "Write the grid feed-in hard limit from Core.Meta (gridFeedInLimitationType / maximumGridFeedInLimit) into the inverter (reg 44102/44104) as hardware backstop. The inverter then limits the export at its grid meter and curtails PV if the battery cannot absorb the surplus. The dynamic limitation (Grid-Meter-ID) aims below the limit (500 W, at most half of the limit), so the backstop usually acts first and the inverter respects the battery set-point in BATTERY_CONTROL while it curtails.")
	EnableDisable feedPowerEnable() default EnableDisable.DISABLE;

	@AttributeDefinition(name = "Grid-Meter-ID", description = "Grid meter for the dynamic feed-in limitation (Core.Meta gridFeedInLimitationType = DYNAMIC_LIMITATION): when the export exceeds the limit, the inverter AC output is capped via reg 43052 so that PV is curtailed. Leave empty to disable.")
	String meter_id() default "meter0";

	String webconsole_configurationFactory_nameHint() default "io.openems.edge.pytes [{id}]";

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Extended Debug mode", description = "Enables extended Debug mode")
	boolean extendedDebugMode() default false;

	@AttributeDefinition(name = "ReadOnly Mode", description = "read only mode")
	boolean readOnlyMode() default false;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. ")
	int modbusUnitId() default 1;

}