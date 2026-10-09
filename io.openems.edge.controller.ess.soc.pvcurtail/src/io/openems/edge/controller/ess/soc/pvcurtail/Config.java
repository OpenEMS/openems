package io.openems.edge.controller.ess.soc.pvcurtail;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Controller Ess SoC PvCurtail with Q",
    description = "Curtails PV export when ESS SoC approaches the upper limit. "
            + "ALSO handles reactive power (Q) with priority: PV first, BESS takes overflow."
)
@interface Config {

    /** Day/night selection for the net-zero Q offset. */
    enum QOffsetMode {
        /** Night when PV active power <= nightPvThresholdW. */
        AUTO,
        FORCE_DAY,
        FORCE_NIGHT
    }

    String id() default "ctrlSocPvCurtail0";
    String alias() default "";
    boolean enabled() default true;

    // ────────────────────────────────────────────────────────────
    // ACTIVE POWER (P) CONFIGURATION  (unchanged)
    // ────────────────────────────────────────────────────────────

    @AttributeDefinition(name = "Ess-ID", description = "ID of the ESS")
    String ess_id() default "ess0";

    @AttributeDefinition(name = "PV Inverter-ID", description = "ID of the PV inverter")
    String pvInverter_id() default "pvInverter0";

    @AttributeDefinition(name = "BatteryInverter-ID",
            description = "Q is written straight to this component's SetReactivePowerSetpoint channel (reg 10), "
                    + "bypassing Ess.Power, which limits ess0 Q to 0.")
    String batteryInverter_id() default "batteryInverter0";

    @AttributeDefinition(name = "Meter-ID", description = "Grid (POI) meter ID. Used to log measured Q.")
    String meter_id() default "meter0";

    @AttributeDefinition(name = "SoC ramp-down start [%]")
    int socRampStart() default 80;

    @AttributeDefinition(name = "Upper SoC threshold [%]")
    int socUpperLimit() default 95;

    @AttributeDefinition(name = "Lower SoC threshold [%]")
    int socLowerLimit() default 85;

    @AttributeDefinition(name = "Target Grid Setpoint [W]",
            description = "Negative = export. The PV floor limit is derived from this.")
    int targetGridSetpoint() default -2000000;

    @AttributeDefinition(name = "Uncurtailed PV limit [W]")
    int wideOpenLimit() default 2000000;

    @AttributeDefinition(name = "Max PV P limit change per cycle [W]")
    int maxRampWattsPerCycle() default 50000;

    // ────────────────────────────────────────────────────────────
    // REACTIVE POWER (Q) CONFIGURATION
    // ────────────────────────────────────────────────────────────

    @AttributeDefinition(name = "Enable Reactive Power Control")
    boolean qControlEnabled() default true;

    @AttributeDefinition(
            name = "Write Q commands to devices",
            description = "false = dry run: calculate and publish channels only, write nothing to PV/BESS."
    )
    boolean qWriteEnabled() default true;

    @AttributeDefinition(
            name = "Fallback Q setpoint [var]",
            description = "Master Q setpoint as written by the HEMS (M_QAC_PC_RX1). A value written to channel "
                    + "QacPcRx overrides it until this setting changes again."
    )
    int qDemandVar() default 0;

    @AttributeDefinition(name = "Q offset mode", description = "AUTO / FORCE_DAY / FORCE_NIGHT")
    QOffsetMode qOffsetMode() default QOffsetMode.AUTO;

    @AttributeDefinition(
            name = "Day net-zero offset [var]",
            description = "Added to the master setpoint in the day. 200000 = +0.2 MVAr "
                    + "(cancels the -0.2 MVAr the plant consumes at the POI)."
    )
    int qOffsetDayVar() default 200000;

    @AttributeDefinition(
            name = "Night net-zero offset [var]",
            description = "Added to the master setpoint at night. -50000 = -0.05 MVAr."
    )
    int qOffsetNightVar() default -50000;

    @AttributeDefinition(
            name = "Night threshold: PV active power [W]",
            description = "AUTO mode: PV active power at or below this counts as night."
    )
    int nightPvThresholdW() default 1000;

    @AttributeDefinition(
            name = "PV Q capacity override [var]",
            description = "-1 = use live PV capacity (PvQacIndLimit / PvQacCapLimit). "
                    + ">= 0 = force this symmetric capacity for tests: "
                    + "1000000 (Test 1), 0 (Test 2), 250000 (Test 3)."
    )
    int pvQCapOverrideVar() default -1;

    @AttributeDefinition(name = "PV rated apparent power fallback [VA]",
            description = "Only used if PV Q-limit channels are not available.")
    int pvMaxApparentPowerVar() default 2000000;

    @AttributeDefinition(name = "BESS rated apparent power fallback [VA]",
            description = "Only used if ess.MaxApparentPower is not available.")
    int bessMaxApparentPowerVar() default 4250000;

    @AttributeDefinition(
            name = "Max Q limit change per cycle [var]",
            description = "Slew limit for PV and BESS Q. Keep high for step tests "
                    + "(a 0.66 MVAr step at 50000 needs ~14 cycles). Lower it for production."
    )
    int maxRampQVarPerCycle() default 1000000;

    String webconsole_configurationFactory_nameHint() default "Controller Ess SoC PvCurtail [{id}]";
}