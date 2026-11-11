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

    /** Active power control mode, selected from the HEMS (only one at a time). */
    enum ControlMode {
        /** POI setpoint held by ctrlBalancing0, PV curtailed by SoC (targetGridSetpoint). */
        SCENARIO_1,
        /** Daily charge / discharge windows; this controller sets BESS power and PV limit. */
        SCENARIO_2,
        /** No active power control: BESS 0 W, PV not curtailed. */
        OFF
    }

    /** Where the BESS charges from inside the charge window. */
    enum ChargeSource {
        PV_ONLY,
        /** PV surplus first, the grid covers the rest of the allowed charge power. */
        PV_AND_GRID,
        /** Grid only; PV is curtailed to 0. */
        GRID_ONLY
    }

    /** PV_ONLY: who gets the PV surplus first inside the charge window. */
    enum ChargePriority {
        /** BESS charges first, export up to the charge grid setting gets the rest. */
        BATTERY_FIRST,
        /** Export up to the charge grid setting first, the BESS gets the rest. */
        EXPORT_FIRST
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

    @AttributeDefinition(name = "BatteryInverter-IDs",
            description = "BESS Q is written straight to each listed component's SetReactivePowerSetpoint channel "
                    + "(reg 10), bypassing Ess.Power, which limits ess0 Q to 0. With several PCS (Ess.Cluster as "
                    + "Ess-ID) list all of them; Q is split by their MaxApparentPower. Adding a PCS = adding its ID.")
    String[] batteryInverter_ids() default { "batteryInverter0" };

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
    // CONTROL MODE AND SCENARIO 2 (SCHEDULED CHARGE / DISCHARGE)
    // ────────────────────────────────────────────────────────────

    @AttributeDefinition(name = "Control mode", description = "SCENARIO_1 / SCENARIO_2 / OFF. "
            + "SCENARIO_2 and OFF need this controller to run before ctrlBalancing0 (scheduler order).")
    ControlMode controlMode() default ControlMode.SCENARIO_1;

    @AttributeDefinition(name = "Schedule time zone", description = "Time zone of the window times, e.g. Asia/Kolkata")
    String scheduleTimeZone() default "Asia/Kolkata";

    @AttributeDefinition(name = "Charge window from [HH:MM]", description = "Daily. From = To disables the window.")
    String chargeFrom() default "00:00";

    @AttributeDefinition(name = "Charge window to [HH:MM]", description = "Exclusive. To < From crosses midnight.")
    String chargeTo() default "00:00";

    @AttributeDefinition(name = "Charge SoC limit [%]", description = "Charging stops at this SoC.")
    int chargeSocLimit() default 95;

    @AttributeDefinition(name = "Charge grid setting [W]",
            description = "PV_ONLY / PV_AND_GRID: maximum export at the POI. GRID_ONLY: import power at the POI.")
    int chargeGridSettingW() default 0;

    @AttributeDefinition(name = "Charge source", description = "PV_ONLY / PV_AND_GRID / GRID_ONLY")
    ChargeSource chargeSource() default ChargeSource.PV_ONLY;

    @AttributeDefinition(name = "Charge priority", description = "PV_ONLY only: BATTERY_FIRST / EXPORT_FIRST")
    ChargePriority chargePriority() default ChargePriority.BATTERY_FIRST;

    @AttributeDefinition(name = "Grid import limit [W]", description = "PV_AND_GRID / GRID_ONLY: maximum import at the POI.")
    int gridImportLimitW() default 2000000;

    @AttributeDefinition(name = "Discharge window from [HH:MM]", description = "Daily. From = To disables the window.")
    String dischargeFrom() default "00:00";

    @AttributeDefinition(name = "Discharge window to [HH:MM]", description = "Exclusive. To < From crosses midnight.")
    String dischargeTo() default "00:00";

    @AttributeDefinition(name = "Discharge SoC limit [%]", description = "Discharging stops at this SoC.")
    int dischargeSocLimit() default 20;

    @AttributeDefinition(name = "Discharge grid setting [W]", description = "Export held at the POI.")
    int dischargeGridSettingW() default 0;

    @AttributeDefinition(name = "Scenario 2 PV headroom per cycle [W]",
            description = "How far the PV limit may run ahead of the BESS while it ramps. Bounds the "
                    + "short export overshoot while the BESS catches up; 0 in steady state.")
    int scheduleHeadroomW() default 20000;

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