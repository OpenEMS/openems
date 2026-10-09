package io.openems.edge.batteryinverter.custom;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.sum.GridMode;

@ObjectClassDefinition(
        name = "iPLON BatteryInverter (PCS) — Custom",
        description = "Custom OpenEMS driver for iPLON-controlled PCS/BatteryInverter"
)
@interface Config {

    @AttributeDefinition(name = "Component-ID")
    String id() default "batteryInverter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name")
    String alias() default "iPLON BatteryInverter";

    @AttributeDefinition(name = "Is enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge component")
    String modbus_id() default "modbus3";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "Modbus slave unit ID of iPLON")
    int modbusUnitId() default 1;

    @AttributeDefinition(
            name = "SOC Upper Limit (× 0.1 %)",
            description = "Charge lock activates at this SoC. 950 = 95.0%"
    )
    int socUpperLimit() default 950;

    @AttributeDefinition(
            name = "SOC Lower Limit (× 0.1 %)",
            description = "Discharge lock activates at this SoC. 50 = 5.0%"
    )
    int socLowerLimit() default 50;

    @AttributeDefinition(
            name = "Max Apparent Power (W)",
            description = "Rated apparent power of the PCS in Watts. Used as fallback for the "
                    + "MAX_APPARENT_POWER channel when the BMS power registers (38/39) are both 0 "
                    + "or not yet read. Set to your real PCS rating."
    )
    int maxApparentPower() default 4250000;

	@AttributeDefinition(name = "Grid mode")
	GridMode gridMode() default GridMode.ON_GRID;

    @AttributeDefinition(
            name = "Max Power Ramp UP Rate (W per cycle)",
            description = "Limits how fast the commanded power to the PCS (reg 9) may INCREASE per cycle "
                    + "(more discharge / less charge = more grid export). The ramp is anchored to the last "
                    + "commanded value, so this is the real rate. Set to a large value (e.g. 10000000) "
                    + "to effectively disable."
    )
    int maxRampWattsPerCycle() default 20000;

    @AttributeDefinition(
            name = "Max Power Ramp DOWN Rate (W per cycle)",
            description = "Limits how fast the commanded power may DECREASE per cycle (backing off, less "
                    + "grid export). Keep this well above the up-ramp so a slow down-ramp never keeps the "
                    + "plant over its export limit. Values below the up-ramp are treated as equal to it."
    )
    int maxRampDownWattsPerCycle() default 100000;

    @AttributeDefinition(
            name = "PV inverter component id",
            description = "e.g. pvInverter0. BESS discharge increases are held back while this PV is "
                    + "still rising. Empty = PV-first gate disabled."
    )
    String pvInverterId() default "";

    @AttributeDefinition(
            name = "PV stall cycles",
            description = "Cycles PV may stay flat before the BESS is allowed to take over. PV rises in "
                    + "steps ~4 cycles apart, so use >= 8."
    )
    int pvStallCycles() default 8;

    @AttributeDefinition(
            name = "SoC lock hysteresis (%)",
            description = "Charge lock releases once SoC is this many % below the upper limit; discharge "
                    + "lock releases this many % above the lower limit. 0 = no hysteresis."
    )
    int socLockHysteresisPct() default 2;

    @AttributeDefinition(
            name = "PCS feedback delay (cycles)",
            description = "Approximate number of cycles reg 29 lags the setpoint (limits ramp windup)."
    )
    int feedbackDelayCycles() default 2;

    @AttributeDefinition(
            name = "Discharge Hold-Off (cycles)",
            description = "When the requested power jumps to MORE discharge than the battery is "
                    + "currently delivering, hold the battery flat for this many cycles before "
                    + "ramping toward the new request (gives PV time to recover first). Every hold-off "
                    + "cycle is dead time in the balancing loop and can increase hunting, so keep it "
                    + "small. Does not apply to decreasing requests, charge requests or the SoC-lock "
                    + "cutoff. Set to 0 to disable."
    )
    int dischargeHoldOffCycles() default 2;

    @AttributeDefinition(
            name = "Discharge Hold-Off Threshold (W)",
            description = "Only trigger the hold-off when the requested increase in discharge "
                    + "exceeds this many Watts over the currently measured output."
    )
    int dischargeHoldOffThresholdW() default 20000;

    String webconsole_configurationFactory_nameHint()
            default "iPLON BatteryInverter [{id}]";
}