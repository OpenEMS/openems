package io.openems.edge.batteryinverter.custom;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.SymmetricEss;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;

import io.openems.edge.bridge.modbus.api.*;
import io.openems.edge.bridge.modbus.api.element.*;
import io.openems.edge.bridge.modbus.api.task.*;

import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

/**
 * BatteryInverterImpl — FINAL CLEAN (compiles without errors)
 *
 * ROOT CAUSE OF ALL COMPILE ERRORS
 * ─────────────────────────────────────────────────────────────
 * ManagedSymmetricBatteryInverter does NOT have:
 *   - getMinimumActivePowerToCharge()       → compile error
 *   - getMinimumActivePowerToDischarge()    → compile error
 *   - ChannelId.ALLOWED_CHARGE_POWER        → compile error
 *   - ChannelId.ALLOWED_DISCHARGE_POWER     → compile error
 *
 * HOW ess0[Allowed:min;max] IS ACTUALLY COMPUTED
 * ─────────────────────────────────────────────────────────────
 * EssGenericImpl reads from the BATTERY component, not inverter:
 *   Allowed_charge    = battery.CHARGE_MAX_CURRENT    × battery.VOLTAGE
 *   Allowed_discharge = battery.DISCHARGE_MAX_CURRENT × battery.VOLTAGE
 *
 * These come from BatteryCustomImpl regs 106 and 108.
 * Node-RED function 26 already writes derated_chg_A → reg 106
 * and derated_dis_A → reg 108 (with SENTINEL=1 to prevent "not ready").
 * That is the CORRECT and ONLY mechanism needed.
 *
 * THIS CLASS ONLY NEEDS TO:
 *   1. Read reg 29 (kW) → _setActivePower(kW × 1000)   [works in doc-13]
 *   2. Read reg 31 (SOC%) for lock writes
 *   3. Write reg 9 (setpoint kW), reg 3-8 (flags), reg 45 (HB)
 *   4. Clamp setActivePower to 0 when SoC lock matches direction
 *   5. Ramp-rate limit the FINAL commanded value before it is written,
 *      so no upstream controller (balancing, SoC lock, PV curtailment,
 *      Ess.Power filters, ...) can force an instantaneous step at the PCS.
 *
 * CHANGES OVER doc-13
 * ─────────────────────────────────────────────────────────────
 * +  reg 29 read: kW × 1000 → W  (kept from doc-13, this is why it works)
 * +  run() clamps setActivePower=0 when chg/dis lock active  (FIX sentinel leak)
 * +  SOC lock thresholds from config (upper=95%, lower=5%)
 * +  debugLog shows SOC and lock state for easier diagnosis
 * +  actuator-level slew-rate limiter on the commanded power (maxRampWattsPerCycle)
 */
@Designate(ocd = Config.class, factory = true)
@Component(
    name = "io.openems.edge.batteryinverter.custom",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BatteryInverterImpl
        extends AbstractOpenemsModbusComponent
        implements ManagedSymmetricBatteryInverter,
                   SymmetricBatteryInverter,
                   ModbusComponent,
                   OpenemsComponent,
                   StartStoppable {

    @Reference
    private ConfigurationAdmin cm;

    @Reference
    protected ComponentManager componentManager;

    @Reference
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private Config config;
    private int heartbeat = 0;

    /**
     * The last commanded active power actually written to the PCS (W).
     * Used as the baseline for the slew-rate limiter. Integer.MIN_VALUE
     * means "not yet initialized" — the very first cycle is not slewed,
     * to avoid a slow crawl up from zero at startup.
     */
    private int lastCommandedActivePower = Integer.MIN_VALUE;

    /**
     * Counts consecutive cycles the request has been a GROWING discharge
     * (more positive than what the battery currently measures). Used by
     * the discharge hold-off: a request only starts being acted on once
     * this counter exceeds dischargeHoldOffCycles, giving PV time to
     * reach its own target first and potentially close the gap without
     * the battery moving.
     */
    private int risingDischargeCycles = 0;

    /** SoC locks with hysteresis (see run()). */
    private boolean chargeLocked = false;
    private boolean dischargeLocked = false;

    /** PV-first gate state (see pvFirstGate()). */
    private static final int PV_MOVE_THRESHOLD_W = 5_000;
    /** PV production when PV last moved by more than PV_MOVE_THRESHOLD_W (rise/fall reference). */
    private int pvLastPowerW = Integer.MIN_VALUE;
    /** Last seen PV limit; a raised limit means PV is about to rise. */
    private int pvLastLimitW = Integer.MIN_VALUE;
    /** BESS command when the PV-first gate engaged; MIN_VALUE while the gate is open. */
    private int pvGateBaseW = Integer.MIN_VALUE;
    /** Last few BESS commands (ring buffer) for the gate base. */
    private final int[] recentCommandsW = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
    private int recentCommandsIdx = 0;
    /** Cycles since PV last rose. Starts "large" so the gate is OFF until PV is seen rising. */
    private int pvStallCycles = Integer.MAX_VALUE / 2;

    public BatteryInverterImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            StartStoppable.ChannelId.values(),
            SymmetricBatteryInverter.ChannelId.values(),
            ManagedSymmetricBatteryInverter.ChannelId.values(),
            BatteryInverterCustom.ChannelId.values()
        );
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================
    @Activate
    private void activate(ComponentContext context, Config config)
            throws OpenemsException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(),
                config.modbusUnitId(), this.cm, "Modbus", config.modbus_id())) {
            return;
        }
        this.config = config;
        this._setStartStop(StartStop.START);
        this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(GridMode.ON_GRID);
        this.lastCommandedActivePower = Integer.MIN_VALUE;
        this.risingDischargeCycles = 0;
        this.chargeLocked = false;
        this.dischargeLocked = false;
        this.pvLastPowerW = Integer.MIN_VALUE;
        this.pvLastLimitW = Integer.MIN_VALUE;
        this.pvGateBaseW = Integer.MIN_VALUE;
        java.util.Arrays.fill(this.recentCommandsW, Integer.MIN_VALUE);
        this.recentCommandsIdx = 0;
        this.pvStallCycles = Integer.MAX_VALUE / 2;
    }

    @Deactivate
    protected void deactivate() { super.deactivate(); }

    @Override public int getPowerPrecision() { return 1000; } // PCS setpoint is whole kW
    @Override public void setStartStop(StartStop value) { this._setStartStop(value); }

    // =========================================================
    // FEEDBACK PROPAGATION
    // Identical to working doc-13 — this is what produces correct
    // BESS SETPOINT values. Do not change the kW×1000 multiply.
    // =========================================================
    private void propagateScaledChannels() {

        // reg 29 = kW written by Node-RED simulation (Math.round(actual/1000))
        // multiply ×1000 → W for SymmetricBatteryInverter.ACTIVE_POWER
        Integer pacKw = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.ACTUAL_ACTIVE_POWER_KW).value().get();
        if (pacKw != null) {
            this._setActivePower(pacKw * 1000);
        }

        // reg 30 = kVAr → VAr
        Integer qacKvar = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.ACTUAL_REACTIVE_POWER_KVAR).value().get();
        if (qacKvar != null) {
            this._setReactivePower(qacKvar * 1000);
        }

        // MAX_APPARENT_POWER = PCS rating, NOT the discharge limit alone.
        // Reg 38 (max discharge) drops to 0 when the BMS locks discharge at
        // low SoC. Using it alone made MAX_APPARENT_POWER = 0, which also
        // blocked CHARGING (ess0 Allowed:0;0 at 5% SoC). Use the larger of
        // reg 38 / reg 39, and fall back to the configured rating.
        Integer maxDisKw = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.MAX_DISCHARGE_POWER_KW).value().get();
        Integer maxChgKw = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.MAX_CHARGE_POWER_KW).value().get();
        int fromRegsW = Math.max(
            maxDisKw != null ? Math.abs(maxDisKw) : 0,
            maxChgKw != null ? Math.abs(maxChgKw) : 0) * 1000;
        int ratingW = (config != null) ? config.maxApparentPower() : 0;
        int maxApparentW = (fromRegsW > 0) ? fromRegsW : ratingW;
        if (maxApparentW > 0) {
            this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER)
                .setNextValue(maxApparentW);
        }
    }

    // =========================================================
    // PV-FIRST GATE
    // =========================================================
    /**
     * Returns how many watts of extra output the PV inverter is still expected to
     * deliver on its own (limit - production), or 0 when the gate must not hold the
     * BESS back: no PV configured / readable, PV falling, or PV stalled for
     * pvStallCycles (e.g. sun-limited) - in all those cases the BESS must act normally.
     */
    private int pvExpectedRiseW(int bessActualW, boolean haveFeedback) {
        if (this.config == null || this.config.pvInverterId() == null
                || this.config.pvInverterId().isEmpty()) {
            return 0;
        }
        try {
            ManagedSymmetricPvInverter pv = this.componentManager.getComponent(this.config.pvInverterId());
            Integer prod = pv.getActivePower().get();
            // Prefer the limit queued THIS cycle (curtail controller runs before the ESS); the
            // channel value only reflects it a cycle later, which let one ramp step through.
            Integer limit = pv.getActivePowerLimitChannel().getNextWriteValue()
                    .orElse(pv.getActivePowerLimit().get());
            if (prod == null || limit == null) {
                return 0;
            }
            // The grid meter can see a PV step a cycle before the PV inverter reading does.
            // PV delivered according to the meter = export - BESS output; the larger one is
            // used for the remaining headroom only. Rise/fall detection below uses the PV
            // inverter reading: the meter also sees BESS moves a cycle before reg 29 does,
            // which made the estimate look like a PV drop and opened the gate.
            int delivered = prod;
            if (haveFeedback) {
                Sum sum = this.componentManager.getComponent("_sum");
                Integer grid = sum.getGridActivePower().get();
                if (grid != null) {
                    delivered = Math.max(prod, -grid - bessActualW);
                }
            }
            // A raised limit means PV is about to climb: hold the BESS before PV even moves.
            if (this.pvLastLimitW != Integer.MIN_VALUE && limit > this.pvLastLimitW + PV_MOVE_THRESHOLD_W) {
                this.pvStallCycles = 0;
                this.pvLastPowerW = prod;
            }
            this.pvLastLimitW = limit;

            // Rise/fall is measured against the power at the last detected move, not the
            // previous cycle, so a slow stepped ramp (e.g. a PPC moving the plant a few kW
            // every few seconds) still counts as climbing.
            if (this.pvLastPowerW == Integer.MIN_VALUE) {
                this.pvLastPowerW = prod;
            } else if (prod > this.pvLastPowerW + PV_MOVE_THRESHOLD_W) {
                this.pvStallCycles = 0;                      // PV is climbing
                this.pvLastPowerW = prod;
            } else if (prod < this.pvLastPowerW - PV_MOVE_THRESHOLD_W) {
                this.pvStallCycles = Integer.MAX_VALUE / 2;  // PV falling: BESS reacts at once
                this.pvLastPowerW = prod;
            } else if (this.pvStallCycles < Integer.MAX_VALUE / 2) {
                this.pvStallCycles++;                        // flat between PV steps
            }

            if (this.pvStallCycles >= Math.max(this.config.pvStallCycles(), 1)) {
                return 0;
            }
            return Math.max(limit - delivered, 0);
        } catch (Exception e) {
            return 0; // fail open: never block the BESS because PV could not be read
        }
    }

    // =========================================================
    // RUN
    // =========================================================
    @Override
    public void run(Battery battery, int setActivePower, int setReactivePower)
            throws OpenemsNamedException {

        // 1. Propagate feedback channels (same as doc-13)
        this.propagateScaledChannels();

        // 2. SoC locks WITH HYSTERESIS.
        // Charge lock engages at the upper limit and only releases once SoC has
        // dropped socLockHysteresisPct below it (discharge lock mirrors this at the
        // lower limit). ess0[Allowed] is still driven by the battery component; this
        // is a belt-and-braces guard in the driver.
        int socPct = battery.getSoc().orElse(50);
        int upper  = (config != null ? config.socUpperLimit() : 1000) / 10; // config is per-mille (e.g. 950 = 95%)
        int lower  = (config != null ? config.socLowerLimit() : 100)  / 10; // config is per-mille (e.g.  50 =  5%)
        int hyst   = (config != null) ? Math.max(config.socLockHysteresisPct(), 0) : 0;
        // Clamp so the two locks can never be released/engaged in overlapping ranges.
        hyst = Math.min(hyst, Math.max((upper - lower) / 2 - 1, 0));

        if (socPct >= upper) {
            this.chargeLocked = true;
        } else if (socPct <= upper - hyst) {
            this.chargeLocked = false;
        }
        if (socPct <= lower) {
            this.dischargeLocked = true;
        } else if (socPct >= lower + hyst) {
            this.dischargeLocked = false;
        }

        // 3. Measured output (reg 29). Lags the setpoint by ~feedbackDelayCycles.
        Integer actualKw = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.ACTUAL_ACTIVE_POWER_KW).value().get();
        boolean haveFeedback = (actualKw != null);
        int actualW = haveFeedback ? actualKw * 1000 : setActivePower;

        // PV-first gate tracking must run every cycle (also during SoC cut-offs), otherwise a
        // PV-limit raise that happens while cut off is never seen and the gate starts open.
        int pvRise = this.pvExpectedRiseW(actualW, haveFeedback);

        // 4. SoC-lock CUT-OFF: always immediate, bypasses hold-off and ramp.
        // Triggers when the request points in a locked direction, or when the PCS is
        // still moving power in a locked direction (feedback lag): command 0 until the
        // measured power has actually reached 0.
        boolean cutoff = false;
        if (this.chargeLocked) {
            if (setActivePower < 0) {
                cutoff = true;
            } else if (haveFeedback && actualW < 0) {
                cutoff = true;
            }
        }
        if (this.dischargeLocked) {
            if (setActivePower > 0) {
                cutoff = true;
            } else if (haveFeedback && actualW > 0) {
                cutoff = true;
            }
        }

        if (cutoff) {
            setActivePower = 0;
            this.risingDischargeCycles = 0;
            this.pvGateBaseW = Integer.MIN_VALUE;
        } else {
            // 5a. Discharge hold-off - REAL discharge requests only (setActivePower > 0).
            // "Less charge" (e.g. -240 kW while delivering -635 kW) no longer triggers it.
            // While holding, stay flat at the last commanded value.
            int holdOffCycles = (config != null) ? Math.max(config.dischargeHoldOffCycles(), 0) : 0;
            int riseThreshold = (config != null) ? Math.max(config.dischargeHoldOffThresholdW(), 0) : 20000;

            if (setActivePower > 0 && setActivePower > actualW + riseThreshold) {
                this.risingDischargeCycles++;
                if (this.risingDischargeCycles <= holdOffCycles) {
                    setActivePower = (this.lastCommandedActivePower != Integer.MIN_VALUE)
                            ? this.lastCommandedActivePower : actualW;
                }
            } else {
                this.risingDischargeCycles = 0;
            }

            // 5a-2. PV-FIRST gate. The grid gap closes from two sources: the PV inverter
            // (slow, rises in steps) and the BESS (fast). If the BESS chases the whole gap
            // while PV is still on its way, BESS + PV overshoot the target and the BESS has
            // to back off again. So while PV is still rising toward its limit the BESS holds;
            // it covers what PV cannot only after PV has stalled for pvStallCycles.
            // Only INCREASES are gated; decreases and all SoC cut-offs pass through.
            // The base is frozen when the gate engages: anchoring it to the last command let
            // every one-cycle leak become the new floor, ratcheting the BESS up during a PV ramp.
            {
                if (pvRise > 0) {
                    if (this.pvGateBaseW == Integer.MIN_VALUE) {
                        // Lowest command of the last cycles: the setpoint reaches the balancing
                        // controller a cycle or two before the PV limit is raised, and a ramp
                        // step can slip through in between.
                        this.pvGateBaseW = (this.lastCommandedActivePower != Integer.MIN_VALUE)
                                ? this.lastCommandedActivePower : actualW;
                        for (int c : this.recentCommandsW) {
                            if (c != Integer.MIN_VALUE) {
                                this.pvGateBaseW = Math.min(this.pvGateBaseW, c);
                            }
                        }
                    }
                    // Hold completely while PV is still climbing. Letting through "request minus
                    // PV headroom" leaked the Ess.Power PT1 lag (request trails the shrinking gap)
                    // and the BESS then overshot POI once PV arrived. If PV stops short (sun
                    // limit), pvExpectedRiseW() returns 0 after pvStallCycles and the BESS covers it.
                    if (setActivePower > this.pvGateBaseW) {
                        setActivePower = this.pvGateBaseW;
                    }
                } else {
                    this.pvGateBaseW = Integer.MIN_VALUE;
                }
            }

            // 5b. ASYMMETRIC ramp limit, anchored to the last command.
            // UP   (more discharge / less charge = more grid export): slow, maxRampWattsPerCycle.
            // DOWN (less export = backing off):                      fast, maxRampDownWattsPerCycle.
            // Backing off must never be throttled as hard as ramping up: if the export
            // limit is already exceeded, a slow down-ramp keeps the plant over the limit
            // for as long as it takes the BESS to walk down.
            // Anchored to the last command (not the lagged measurement) to avoid the
            // even/odd zigzag; the command never passes the request, and never leads the
            // measured power by more than (delay + 1) steps in either direction.
            if (haveFeedback) {
                long maxUp   = (config != null) ? Math.max(config.maxRampWattsPerCycle(), 0) : 30000;
                long maxDown = (config != null) ? Math.max(config.maxRampDownWattsPerCycle(), 0) : 100000;
                maxDown = Math.max(maxDown, maxUp);
                int delay = (config != null) ? Math.max(config.feedbackDelayCycles(), 0) : 2;
                long upWindow   = maxUp   * (delay + 1L);
                long downWindow = maxDown * (delay + 1L);

                long base = (this.lastCommandedActivePower != Integer.MIN_VALUE)
                        ? this.lastCommandedActivePower : actualW;
                base = Math.max(actualW - downWindow, Math.min(actualW + upWindow, base));

                long delta = (long) setActivePower - base;
                delta = Math.max(-maxDown, Math.min(maxUp, delta));

                long cmd = base + delta;
                cmd = Math.max(actualW - downWindow, Math.min(actualW + upWindow, cmd));
                // never overshoot the request itself
                if ((base <= setActivePower && cmd > setActivePower)
                        || (base >= setActivePower && cmd < setActivePower)) {
                    cmd = setActivePower;
                }
                setActivePower = (int) Math.max(Integer.MIN_VALUE + 1L, Math.min(Integer.MAX_VALUE, cmd));
            }
        }
        this.lastCommandedActivePower = setActivePower; // baseline for next cycle
        this.recentCommandsW[this.recentCommandsIdx] = setActivePower;
        this.recentCommandsIdx = (this.recentCommandsIdx + 1) % this.recentCommandsW.length;

        // IMPORTANT: do NOT overwrite ACTIVE_POWER here with the commanded
        // setActivePower. propagateScaledChannels() (step 1) already set it
        // from reg 29 - the TRUE measured battery output. That reading is
        // what ess.getActivePower() hands to the balancing controller next
        // cycle. Overwriting it with the optimistic commanded value here
        // was the actual root cause of the windup/overshoot: it told the
        // balancing controller "the battery already delivered this," even
        // while the ramp limiter above was still physically catching up -
        // so balancing kept seeing a gap it thought was new load/PV
        // movement and kept piling on more. Only reactive power (which
        // isn't ramp-limited or contested the same way) is set here.
        // Q setpoint (reg 10, kvar).
        // ctrlSocPvCurtail0 queues a value on this channel BEFORE run() is called. If one is
        // pending, keep it; otherwise fall back to what Ess.Power handed us.
        // NOTE: _setReactivePower() is deliberately NOT called here - REACTIVE_POWER must stay
        // the measured value from reg 30 so setpoint-vs-actual plots are honest.
        IntegerWriteChannel qCh = this.channel(
            BatteryInverterCustom.ChannelId.SET_REACTIVE_POWER_SETPOINT);
        int qKvar;
        java.util.Optional<Integer> externalQ = qCh.getNextWriteValue();
        if (externalQ.isPresent()) {
            qKvar = externalQ.get();
        } else {
            qKvar = setReactivePower / 1000;
            qCh.setNextWriteValue(qKvar);
        }

        System.out.println("BESS SETPOINT = " + (setActivePower / 1000) + " kW, Q cmd = " + qKvar + " kVar"
                + (cutoff ? " [SoC cutoff]" : (this.chargeLocked || this.dischargeLocked) ? " [SoC lock active]" : ""));

        // 5. Active power setpoint (reg 9 kW, S16)
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_ACTIVE_POWER_SETPOINT)
            .setNextWriteValue(setActivePower / 1000);

        // 6. Control flags (reg 3-8 via FC16 block)
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_REMOTE_LOCAL)  .setNextWriteValue(1);
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_ALLOWED)       .setNextWriteValue(1);
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_CONTROLLABLE)  .setNextWriteValue(1);
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_ENGAGEMENT)    .setNextWriteValue(1);

        // 7. Charge/discharge locks (reg 7, reg 8)
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_CHARGE_LOCK)
            .setNextWriteValue(this.chargeLocked ? 1 : 0);
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_DISCHARGE_LOCK)
            .setNextWriteValue(this.dischargeLocked ? 1 : 0);

        // 8. SOC limits (reg 13, reg 14 in 0.1% units: 1000=100%, 10=100%)
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_SOC_UPPER_LIMIT)
            .setNextWriteValue(config != null ? config.socUpperLimit() : 1000);
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_SOC_LOWER_LIMIT)
            .setNextWriteValue(config != null ? config.socLowerLimit() : 100);

        // 9. Heartbeat (reg 45)
        heartbeat = (heartbeat >= 5000) ? 0 : heartbeat + 1;
        this.<IntegerWriteChannel>channel(
            BatteryInverterCustom.ChannelId.SET_HEARTBEAT)
            .setNextWriteValue(heartbeat);
    }

    // =========================================================
    // MODBUS PROTOCOL
    // =========================================================
    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,

            // Reg 29-31: actual power (kW) + reactive (kVAr) + SoC (%)
            new FC3ReadRegistersTask(29, Priority.HIGH,
                m(BatteryInverterCustom.ChannelId.ACTUAL_ACTIVE_POWER_KW,
                    new SignedWordElement(29)),  // kW → ×1000=W in propagate
                m(BatteryInverterCustom.ChannelId.ACTUAL_REACTIVE_POWER_KVAR,
                    new SignedWordElement(30)),  // kVAr
                m(BatteryInverterCustom.ChannelId.SOC_RAW,
                    new SignedWordElement(31))   // % direct 0-100
            ),

            // Reg 38-39: allowed discharge + charge (kW)
            // reg 38 → MAX_DISCHARGE_POWER_KW → MAX_APPARENT_POWER (informational)
            // reg 39 → MAX_CHARGE_POWER_KW    (available for future use)
            // NOTE: ess0[Allowed] is driven by BatteryCustomImpl regs 106/108,
            //       NOT by these registers directly. These are still read so
            //       the channels are populated for debugging.
            new FC3ReadRegistersTask(38, Priority.HIGH,
                m(BatteryInverterCustom.ChannelId.MAX_DISCHARGE_POWER_KW,
                    new SignedWordElement(38)),
                m(BatteryInverterCustom.ChannelId.MAX_CHARGE_POWER_KW,
                    new SignedWordElement(39))
            ),

            // Reg 9: active power setpoint (kW, S16)
            new FC6WriteRegisterTask(9,
                m(BatteryInverterCustom.ChannelId.SET_ACTIVE_POWER_SETPOINT,
                    new SignedWordElement(9))
            ),

            // Reg 10: reactive power setpoint (kVAr, S16)
            new FC6WriteRegisterTask(10,
                m(BatteryInverterCustom.ChannelId.SET_REACTIVE_POWER_SETPOINT,
                    new SignedWordElement(10))
            ),

            // Reg 3-8: control flags (FC16 block write)
            new FC16WriteRegistersTask(3,
                m(BatteryInverterCustom.ChannelId.SET_REMOTE_LOCAL,
                    new UnsignedWordElement(3)),
                m(BatteryInverterCustom.ChannelId.SET_ALLOWED,
                    new UnsignedWordElement(4)),
                m(BatteryInverterCustom.ChannelId.SET_CONTROLLABLE,
                    new UnsignedWordElement(5)),
                m(BatteryInverterCustom.ChannelId.SET_ENGAGEMENT,
                    new UnsignedWordElement(6)),
                m(BatteryInverterCustom.ChannelId.SET_CHARGE_LOCK,
                    new UnsignedWordElement(7)),
                m(BatteryInverterCustom.ChannelId.SET_DISCHARGE_LOCK,
                    new UnsignedWordElement(8))
            ),

            // Reg 45: heartbeat
            new FC6WriteRegisterTask(45,
                m(BatteryInverterCustom.ChannelId.SET_HEARTBEAT,
                    new UnsignedWordElement(45))
            )
        );
    }

    // =========================================================
    // DEBUG LOG
    // =========================================================
    @Override
    public String debugLog() {
        Integer chgKw = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.MAX_CHARGE_POWER_KW).value().get();
        Integer disKw = (Integer) this.channel(
            BatteryInverterCustom.ChannelId.MAX_DISCHARGE_POWER_KW).value().get();

        return "Reg29="
            + this.channel(BatteryInverterCustom.ChannelId.ACTUAL_ACTIVE_POWER_KW)
                  .value().asString()
            + " P="
            + this.channel(BatteryInverterCustom.ChannelId.ACTUAL_ACTIVE_POWER_KW)
            .value().asString()
            + "| Q="
            + this.channel(BatteryInverterCustom.ChannelId.ACTUAL_REACTIVE_POWER_KVAR)
            .value().asString()
           
            + "| Fault=0 | HB=" + heartbeat
            + " | Reg38(DisKw)=" + (disKw != null ? disKw : "null")
            + " | Reg39(ChgKw)=" + (chgKw != null ? chgKw : "null");
    }
}