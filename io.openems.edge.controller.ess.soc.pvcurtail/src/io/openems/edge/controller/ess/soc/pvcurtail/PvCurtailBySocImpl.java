package io.openems.edge.controller.ess.soc.pvcurtail;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "Controller.Ess.SocPvCurtail",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class PvCurtailBySocImpl
        extends AbstractOpenemsComponent
        implements PvCurtailBySoc, Controller, OpenemsComponent {

    private static final Logger LOG = LoggerFactory.getLogger(PvCurtailBySocImpl.class);

    @Reference
    private ComponentManager componentManager;

    private Config config;

    // ── P state ──
    private boolean curtailing = false;
    private int currentPLimit = -1;

    // ── Q state ──
    /** Last value received on QacPcRx; null until the first write. */
    private Integer latchedQSetpoint = null;
    private int currentQPvLimit = 0;
    private int currentQBessLimit = 0;

    // ── Scenario 2 state ──
    private boolean chargeSocLimitReached = false;
    private boolean dischargeSocLimitReached = false;

    private String lastDebug = "";
    private String pDebug = "";

    /** False until the first run() after activation has seeded the state from the devices. */
    private boolean seeded = false;

    public PvCurtailBySocImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            Controller.ChannelId.values(),
            PvCurtailBySoc.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.curtailing = false;
        this.currentPLimit = -1;
        this.latchedQSetpoint = null;
        this.currentQPvLimit = 0;
        this.currentQBessLimit = 0;
        this.seeded = false;
    }

    /*
     * Setpoint changes from the HEMS arrive as config updates (targetGridSetpoint,
     * qDemandVar). Applying them in place keeps the curtailment latch, the current PV
     * limit and the Q commands; a re-activation reset them (PV limit jumped to wide-open,
     * Q commands restarted from 0).
     */
    @Modified
    private void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.latchedQSetpoint = null; // a new qDemandVar from the HEMS takes over
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsNamedException {
        ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
        ManagedSymmetricPvInverter pv = this.componentManager.getComponent(this.config.pvInverter_id());

        // After a (re)start continue from what the devices currently have instead of
        // wide-open PV / zero Q, so an Edge restart does not cause a step at the POI.
        if (!this.seeded) {
            Integer pLimit = pv.getActivePowerLimit().get();
            if (pLimit != null && pLimit >= 0) {
                this.currentPLimit = pLimit;
            }
            Integer qPv = readInt(pv, "QacLimit");
            if (qPv != null) {
                this.currentQPvLimit = qPv;
            }
            Integer qBess = ess.getReactivePower().get();
            if (qBess != null) {
                this.currentQBessLimit = qBess;
            }
            this.seeded = true;
        }

        // P and Q are independent: an unknown SoC must not stop Q control.
        switch (this.config.controlMode()) {
        case SCENARIO_2:
            this.runScenario2(ess, pv);
            break;
        case OFF:
            this.runOff(ess, pv);
            break;
        default:
            this.channel(PvCurtailBySoc.ChannelId.SCHEDULE_STATE).setNextValue(ScheduleState.SCENARIO_1);
            this.runActivePower(ess, pv, this.config.targetGridSetpoint());
            this.pDebug = "S1 ";
            break;
        }
        if (this.config.qControlEnabled()) {
            this.runReactivePower(ess, pv);
        }
    }

    // =====================================================================
    // P — unchanged logic, moved into its own method
    // =====================================================================
    private void runActivePower(ManagedSymmetricEss ess, ManagedSymmetricPvInverter pv, int targetGridSetpoint)
            throws OpenemsNamedException {

        if (!ess.getSoc().isDefined()) {
            if (this.currentPLimit >= 0) {
                pv.setActivePowerLimit(this.currentPLimit);
            }
            return;
        }
        int soc = ess.getSoc().get();

        int wideOpen = this.config.wideOpenLimit();
        int floorLimit = Math.max(-targetGridSetpoint, 0);

        if (this.currentPLimit < 0) {
            this.currentPLimit = wideOpen;
        }

        if (!this.curtailing && soc >= this.config.socUpperLimit()) {
            this.curtailing = true;
        } else if (this.curtailing && soc <= this.config.socLowerLimit()) {
            this.curtailing = false;
        }

        if (this.curtailing) {
            this.currentPLimit = floorLimit;
        } else {
            int desiredPLimit;
            int rampStart = Math.min(this.config.socRampStart(), this.config.socUpperLimit() - 1);

            if (soc <= rampStart) {
                desiredPLimit = wideOpen;
            } else if (soc >= this.config.socUpperLimit()) {
                desiredPLimit = floorLimit;
            } else {
                double t = (double) (soc - rampStart) / (double) (this.config.socUpperLimit() - rampStart);
                desiredPLimit = wideOpen - (int) Math.round(t * (wideOpen - floorLimit));
            }
            this.currentPLimit = step(this.currentPLimit, desiredPLimit, this.config.maxRampWattsPerCycle());
        }

        pv.setActivePowerLimit(this.currentPLimit);
        this.channel(PvCurtailBySoc.ChannelId.PV_P_LIMIT_CMD).setNextValue(this.currentPLimit);
    }

    // =====================================================================
    // Scenario 2 — daily charge / discharge windows. This controller sets the BESS power
    // itself; it runs before ctrlBalancing0, which then finds the set-point fixed and skips.
    // =====================================================================
    private void runScenario2(ManagedSymmetricEss ess, ManagedSymmetricPvInverter pv)
            throws OpenemsNamedException {
        Integer pvW = pv.getActivePower().get();
        Integer bessW = ess.getActivePower().get();
        Integer gridW = null;
        try {
            ElectricityMeter meter = this.componentManager.getComponent(this.config.meter_id());
            gridW = meter.getActivePower().get();
        } catch (OpenemsNamedException | RuntimeException e) {
            // handled below
        }
        if (pvW == null || bessW == null || gridW == null || !ess.getSoc().isDefined()) {
            // Not enough data: hold the PV limit, BESS idle
            ess.setActivePowerEqualsWithoutFilter(0);
            if (this.currentPLimit >= 0) {
                pv.setActivePowerLimit(this.currentPLimit);
            }
            this.pDebug = "S2 waiting for PV/BESS/meter/SoC ";
            return;
        }
        int soc = ess.getSoc().get();
        int loadW = Math.max(0, gridW + pvW + bessW); // grid + import, BESS + discharge
        var power = ess.getPower();
        final var in = new Scenario2.Inputs(pvW, bessW, loadW, //
                Math.max(0, -power.getMinPower(ess, ALL, ACTIVE)), //
                Math.max(0, power.getMaxPower(ess, ALL, ACTIVE)));

        var now = LocalTime.now(this.componentManager.getClock().withZone(this.scheduleZone()));
        Scenario2.Window window;
        if (Scenario2.inWindow(Scenario2.parseTime(this.config.chargeFrom()),
                Scenario2.parseTime(this.config.chargeTo()), now)) {
            window = Scenario2.Window.CHARGE;
        } else if (Scenario2.inWindow(Scenario2.parseTime(this.config.dischargeFrom()),
                Scenario2.parseTime(this.config.dischargeTo()), now)) {
            window = Scenario2.Window.DISCHARGE;
        } else {
            window = Scenario2.Window.NONE;
        }

        // SoC limits with 1 % hysteresis
        if (soc >= this.config.chargeSocLimit()) {
            this.chargeSocLimitReached = true;
        } else if (soc < this.config.chargeSocLimit() - 1) {
            this.chargeSocLimitReached = false;
        }
        if (soc <= this.config.dischargeSocLimit()) {
            this.dischargeSocLimitReached = true;
        } else if (soc > this.config.dischargeSocLimit() + 1) {
            this.dischargeSocLimitReached = false;
        }

        int wideOpen = this.config.wideOpenLimit();
        int headroom = Math.max(0, this.config.scheduleHeadroomW());
        ScheduleState state;
        int bessCmd;
        int poiTarget;
        switch (window) {
        case CHARGE: {
            var r = Scenario2.charge(in, this.config.chargeSource(), this.config.chargePriority(),
                    this.config.chargeGridSettingW(), this.config.gridImportLimitW(), this.chargeSocLimitReached,
                    headroom);
            bessCmd = r.bessW();
            this.currentPLimit = Scenario2.clamp(r.pvLimitW(), 0, wideOpen);
            poiTarget = this.config.chargeSource() == Config.ChargeSource.GRID_ONLY
                    ? -this.config.chargeGridSettingW()
                    : this.config.chargeGridSettingW();
            state = this.chargeSocLimitReached ? ScheduleState.CHARGE_SOC_LIMIT : ScheduleState.CHARGE;
            ess.setActivePowerEqualsWithoutFilter(bessCmd);
            pv.setActivePowerLimit(this.currentPLimit);
            break;
        }
        case DISCHARGE: {
            var r = Scenario2.discharge(in, this.config.dischargeGridSettingW(), this.dischargeSocLimitReached,
                    headroom);
            bessCmd = r.bessW();
            this.currentPLimit = Scenario2.clamp(r.pvLimitW(), 0, wideOpen);
            poiTarget = this.config.dischargeGridSettingW();
            state = this.dischargeSocLimitReached ? ScheduleState.DISCHARGE_SOC_LIMIT : ScheduleState.DISCHARGE;
            ess.setActivePowerEqualsWithoutFilter(bessCmd);
            pv.setActivePowerLimit(this.currentPLimit);
            break;
        }
        default: {
            // Like Scenario 1 around the base POI: BESS balances, PV curtailed by SoC
            poiTarget = Scenario2.baseExportW(this.config.chargeGridSettingW(), this.config.chargeSource(),
                    this.config.dischargeGridSettingW());
            bessCmd = Scenario2.balanceW(in, poiTarget);
            state = ScheduleState.OUTSIDE_WINDOWS;
            ess.setActivePowerEqualsWithFilter(bessCmd);
            this.runActivePower(ess, pv, -poiTarget);
            break;
        }
        }

        this.channel(PvCurtailBySoc.ChannelId.SCHEDULE_STATE).setNextValue(state);
        this.channel(PvCurtailBySoc.ChannelId.BESS_P_CMD).setNextValue(bessCmd);
        this.channel(PvCurtailBySoc.ChannelId.PV_P_LIMIT_CMD).setNextValue(this.currentPLimit);
        this.channel(PvCurtailBySoc.ChannelId.POI_P_TARGET).setNextValue(poiTarget);
        this.pDebug = String.format("S2 %s soc=%d%% poi=%dk bess=%dk pvLim=%dk | ", state.name(), soc,
                poiTarget / 1000, bessCmd / 1000, this.currentPLimit / 1000);
    }

    private ZoneId scheduleZone() {
        try {
            return ZoneId.of(this.config.scheduleTimeZone());
        } catch (DateTimeException | NullPointerException e) {
            return ZoneId.systemDefault();
        }
    }

    // No active power control: BESS idle, PV not curtailed.
    private void runOff(ManagedSymmetricEss ess, ManagedSymmetricPvInverter pv) throws OpenemsNamedException {
        this.curtailing = false;
        this.currentPLimit = this.config.wideOpenLimit();
        ess.setActivePowerEqualsWithoutFilter(0);
        pv.setActivePowerLimit(this.currentPLimit);
        this.channel(PvCurtailBySoc.ChannelId.SCHEDULE_STATE).setNextValue(ScheduleState.OFF);
        this.channel(PvCurtailBySoc.ChannelId.BESS_P_CMD).setNextValue(0);
        this.channel(PvCurtailBySoc.ChannelId.PV_P_LIMIT_CMD).setNextValue(this.currentPLimit);
        this.pDebug = "OFF | ";
    }

    // =====================================================================
    // Q — setpoint + net-zero offset, PV first, BESS overflow, then WRITE
    // =====================================================================
    private void runReactivePower(ManagedSymmetricEss ess, ManagedSymmetricPvInverter pv)
            throws OpenemsNamedException {

        // 1. Master setpoint (latched from channel QacPcRx; config value until first write)
        IntegerWriteChannel rx = this.channel(PvCurtailBySoc.ChannelId.QAC_PC_RX);
        Optional<Integer> in = rx.getNextWriteValueAndReset();
        if (in.isPresent()) {
            this.latchedQSetpoint = in.get();
            rx.setNextValue(in.get());
        }
        int setpoint = (this.latchedQSetpoint != null) ? this.latchedQSetpoint : this.config.qDemandVar();

        // 2. Current powers
        int pvP = pv.getActivePower().asOptional().orElse(0);
        int bessP = ess.getActivePower().asOptional().orElse(0);

        // 3. Day/night net-zero offset
        boolean night;
        switch (this.config.qOffsetMode()) {
        case FORCE_DAY:
            night = false;
            break;
        case FORCE_NIGHT:
            night = true;
            break;
        default:
            night = pvP <= this.config.nightPvThresholdW();
            break;
        }
        int offset = night ? this.config.qOffsetNightVar() : this.config.qOffsetDayVar();
        int total = setpoint + offset;

        // 4. Capabilities
        int capPos;
        int capNeg;
        if (this.config.pvQCapOverrideVar() >= 0) {
            capPos = capNeg = this.config.pvQCapOverrideVar();
        } else {
            Integer ind = readInt(pv, "PvQacIndLimit"); // + / inductive
            Integer cap = readInt(pv, "PvQacCapLimit"); // - / capacitive
            if (ind != null && cap != null) {
                capPos = Math.abs(ind);
                capNeg = Math.abs(cap);
            } else {
                Integer maxP = readInt(pv, "MaxActivePower");
                int s = (maxP != null && maxP > 0) ? maxP : this.config.pvMaxApparentPowerVar();
                capPos = capNeg = sqrtCapability(s, pvP);
            }
        }

        int bessS = ess.getMaxApparentPower().asOptional().orElse(this.config.bessMaxApparentPowerVar());
        int bessCap = sqrtCapability(bessS, bessP);

        // 5. Split: PV first, BESS takes the rest
        int desiredQPv = clamp(total, -capNeg, capPos);
        int desiredQBess = clamp(total - desiredQPv, -bessCap, bessCap);

        // 6. Slew, then re-clamp so a shrinking capacity is honoured immediately
        int maxStep = Math.max(this.config.maxRampQVarPerCycle(), 0);
        this.currentQPvLimit = clamp(step(this.currentQPvLimit, desiredQPv, maxStep), -capNeg, capPos);
        this.currentQBessLimit = clamp(step(this.currentQBessLimit, desiredQBess, maxStep), -bessCap, bessCap);

        // 7. Write to devices
        if (this.config.qWriteEnabled()) {
            // Direct to each PCS (reg 10, kvar). Ess.Power caps ess0 Q at 0, so it is bypassed.
            this.writeBessQ(this.currentQBessLimit);
            try {
                writeInt(pv, "PvCtl", 1);
                writeInt(pv, "PvQacCtl", 1);
                writeInt(pv, "QacLimit", this.currentQPvLimit);
            } catch (RuntimeException | OpenemsNamedException e) {
                this.logWarn(LOG, "PV Q write failed (is this an InverterSimulated?): " + e.getMessage());
            }
        }

        // 8. Measured Q at POI, for the setpoint-vs-actual record
        Integer poiQ = null;
        try {
            ElectricityMeter meter = this.componentManager.getComponent(this.config.meter_id());
            poiQ = meter.getReactivePower().get();
        } catch (OpenemsNamedException | RuntimeException e) {
            // meter is optional for this feature
        }

        // 9. Publish
        this.channel(PvCurtailBySoc.ChannelId.Q_OFFSET_USED).setNextValue(offset);
        this.channel(PvCurtailBySoc.ChannelId.Q_TOTAL_CMD).setNextValue(total);
        this.channel(PvCurtailBySoc.ChannelId.PV_Q_CMD).setNextValue(this.currentQPvLimit);
        this.channel(PvCurtailBySoc.ChannelId.BESS_Q_CMD).setNextValue(this.currentQBessLimit);
        this.channel(PvCurtailBySoc.ChannelId.PV_Q_CAP_POS).setNextValue(capPos);
        this.channel(PvCurtailBySoc.ChannelId.PV_Q_CAP_NEG).setNextValue(capNeg);
        this.channel(PvCurtailBySoc.ChannelId.POI_Q_MEASURED).setNextValue(poiQ);

        this.lastDebug = this.pDebug + String.format(
                "Q rx=%dk %s off=%dk total=%dk | PV cmd=%dk (cap +%dk/-%dk) | BESS cmd=%dk (cap %dk) | POI=%s",
                setpoint / 1000, night ? "NIGHT" : "DAY", offset / 1000, total / 1000,
                this.currentQPvLimit / 1000, capPos / 1000, capNeg / 1000,
                this.currentQBessLimit / 1000, bessCap / 1000,
                poiQ == null ? "n/a" : (poiQ / 1000) + "k");
    }

    @Override
    public String debugLog() {
        return this.config.qControlEnabled() ? this.lastDebug : this.pDebug;
    }

    // =====================================================================
    // helpers
    // =====================================================================

    /* sqrt(S² − P²) in long math (int math overflows above ~46 kVA). */
    private static int sqrtCapability(long s, long p) {
        return (int) Math.sqrt(Math.max(0L, s * s - p * p));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /* Writes the BESS Q command to all configured PCS, split by their MaxApparentPower. */
    private void writeBessQ(int totalVar) {
        var ids = this.config.batteryInverter_ids();
        var pcs = new OpenemsComponent[ids.length];
        var weights = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            try {
                pcs[i] = this.componentManager.getComponent(ids[i]);
                Integer s = readInt(pcs[i], "MaxApparentPower");
                weights[i] = (s != null && s > 0) ? s : 1;
            } catch (RuntimeException | OpenemsNamedException e) {
                this.logWarn(LOG, "BESS Q: " + ids[i] + " not available: " + e.getMessage());
            }
        }
        var kvar = splitQ(totalVar / 1000, weights);
        for (int i = 0; i < ids.length; i++) {
            if (pcs[i] == null) {
                continue;
            }
            try {
                writeInt(pcs[i], "SetReactivePowerSetpoint", kvar[i]);
            } catch (RuntimeException | OpenemsNamedException e) {
                this.logWarn(LOG, "BESS Q write to " + ids[i] + " failed: " + e.getMessage());
            }
        }
    }

    /**
     * Splits a total (kvar) by weight; units with weight 0 (not available) get 0. The parts add up to the
     * total exactly (the rounding rest goes to the last available unit).
     */
    static int[] splitQ(int total, int[] weights) {
        var out = new int[weights.length];
        long sum = 0;
        int last = -1;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > 0) {
                sum += weights[i];
                last = i;
            }
        }
        if (last < 0) {
            return out;
        }
        int given = 0;
        for (int i = 0; i < last; i++) {
            if (weights[i] > 0) {
                out[i] = (int) Math.round((double) total * weights[i] / sum);
                given += out[i];
            }
        }
        out[last] = total - given;
        return out;
    }

    private static int step(int current, int desired, int maxStep) {
        int max = Math.max(maxStep, 0);
        return current + clamp(desired - current, -max, max);
    }

    /* Reads an Integer channel by id string; null if missing/undefined. Avoids a compile-time dependency on the PV driver bundle. */
    private static Integer readInt(OpenemsComponent c, String channelId) {
        try {
            Channel<?> ch = c.channel(channelId);
            Object v = ch.value().get();
            return (v instanceof Number) ? ((Number) v).intValue() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void writeInt(OpenemsComponent c, String channelId, int value) throws OpenemsNamedException {
        IntegerWriteChannel ch = c.channel(channelId);
        ch.setNextWriteValue(value);
    }
}