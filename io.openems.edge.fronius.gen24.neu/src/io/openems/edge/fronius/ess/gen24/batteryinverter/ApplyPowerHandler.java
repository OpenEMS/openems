package io.openems.edge.fronius.ess.gen24.batteryinverter;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.fronius.ess.enums.SetControlMode;
import io.openems.edge.fronius.ess.gen24.battery.FroniusGen24;

public class ApplyPowerHandler {

    private static final int RATE_100_PERCENT = 10000;
    private static final int RATE_HYSTERESIS = 25;
    private static final long MIN_WRITE_INTERVAL_MS = 2000;
    private static final long KEEP_ALIVE_INTERVAL_MS = 30000;

    // Wechselrichter-Steuerung (Model 123)
    // WMaxLimPct erwartet 0..100, wobei 100 = 100 %
    private static final int W_MAX_LIM_100_PERCENT = 100;
    private static final int W_MAX_LIM_HYSTERESIS = 1;
    private static final long W_MAX_LIM_KEEP_ALIVE_MS = 30000;

    private final BatteryInverterFroniusGen24Impl parent;

    private SetControlMode lastControlMode = null;
    private Integer lastOutWRte = null;
    private Integer lastInWRte = null;
    private long lastWriteMillis = 0L;

    // Wechselrichter-Zustand
    private Integer lastWMaxLimPct = null;
    private int lastWMaxLimEna = -1; // -1 = noch nie geschrieben
    private long lastWMaxLimWriteMillis = 0L;

    public ApplyPowerHandler(BatteryInverterFroniusGen24Impl parent) {
        this.parent = parent;
    }

    public synchronized void apply(
            FroniusGen24 battery,
            int setActivePower,
            int setReactivePower,
            ControlMode controlmode
    ) throws OpenemsNamedException {

        Result result = switch (controlmode) {
        case INTERNAL -> this.handleInternalMode();
        case REMOTE -> this.handleRemoteMode(setActivePower);
        };

        this.parent._setDebugControlMode(result.controlMode());

        // Wechselrichter-Leistungsbegrenzung – immer prüfen, unabhängig von shouldWrite
        var limitOpt = this.parent.getActivePowerLimitChannel().getNextWriteValueAndReset();
        if (limitOpt.isPresent() || this.lastWMaxLimEna == 1) {
            this.applyInverterPowerLimit(limitOpt.isPresent() ? limitOpt.get() : null);
        }

        // Im INTERNAL-Modus nichts schreiben – Fronius arbeitet autonom
        if (result.controlMode() == SetControlMode.DISABLED) {
            // Zustand zurücksetzen damit beim nächsten REMOTE-Start alles neu geschrieben wird
            this.lastControlMode = null;
            this.lastOutWRte = null;
            this.lastInWRte = null;
            return;
        }

        // StorCtl_Mod (40348) jeden Zyklus schreiben im REMOTE-Modus –
        // damit der Fronius den Modus nach Neustart/Verbindungsunterbruch sofort kennt
        EnumWriteChannel setControlMode =
                battery.channel(FroniusGen24.ChannelId.SET_STORAGE_CONTROL_MODE);
        setControlMode.setNextWriteValue(result.controlMode());

        if (!this.shouldWrite(result)) {
            return;
        }

        IntegerWriteChannel setOutWRte =
                battery.channel(FroniusGen24.ChannelId.SET_OUT_W_RTE);

        IntegerWriteChannel setInWRte =
                battery.channel(FroniusGen24.ChannelId.SET_IN_W_RTE);

        setOutWRte.setNextWriteValue(result.outWRte());
        setInWRte.setNextWriteValue(result.inWRte());

        this.rememberWrittenResult(result);
    }

    private Result handleInternalMode() {
        return new Result(SetControlMode.DISABLED, 0, 0);
    }

    private Result handleRemoteMode(int setActivePower) {

        Integer wChaMax = this.readWChaMax();

        if (wChaMax == null || wChaMax <= 0) {
            return this.handleInternalMode();
        }

        int limitedActivePower = clamp(setActivePower, -wChaMax, wChaMax);

        int rate = (int) Math.round(
                (double) limitedActivePower / (double) wChaMax * RATE_100_PERCENT
        );
        rate = clamp(rate, -RATE_100_PERCENT, RATE_100_PERCENT);

        int outWRte = rate;
        int inWRte = rate * (-1);

        return new Result(SetControlMode.CHARGE_AND_DISCHARGE_LIMIT, outWRte, inWRte);
    }

    private Integer readWChaMax() {
        try {
            var channel = this.parent.getStorageWChaMaxChannel();
            var nextValue = channel.getNextValue();
            if (nextValue.isDefined()) {
                return Math.max(0, Math.round(Math.abs(nextValue.get())));
            }
            var value = channel.value();
            if (value.isDefined()) {
                return Math.max(0, Math.round(Math.abs(value.get())));
            }
            return null;
        } catch (OpenemsException e) {
            return null;
        }
    }

    private boolean shouldWrite(Result result) {

        long now = System.currentTimeMillis();

        if (this.lastControlMode == null) {
            return true;
        }

        if (now - this.lastWriteMillis < MIN_WRITE_INTERVAL_MS) {
            return false;
        }

        if (result.controlMode() != this.lastControlMode) {
            return true;
        }

        if (this.lastOutWRte == null
                || Math.abs(result.outWRte() - this.lastOutWRte) >= RATE_HYSTERESIS) {
            return true;
        }

        if (this.lastInWRte == null
                || Math.abs(result.inWRte() - this.lastInWRte) >= RATE_HYSTERESIS) {
            return true;
        }

        return now - this.lastWriteMillis >= KEEP_ALIVE_INTERVAL_MS;
    }

    private void rememberWrittenResult(Result result) {
        this.lastControlMode = result.controlMode();
        this.lastOutWRte = result.outWRte();
        this.lastInWRte = result.inWRte();
        this.lastWriteMillis = System.currentTimeMillis();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static record Result(
            SetControlMode controlMode,
            int outWRte,
            int inWRte
    ) {
    }

    // =========================================================================
    // Wechselrichter-Leistungsbegrenzung (SunSpec Model 123)
    // =========================================================================

    private void applyInverterPowerLimit(Integer limitW) {

        if (limitW != null) {
            Integer wMaxLimPct = this.convertWattsToPct(limitW);
            if (wMaxLimPct != null) {
                this.writeWMaxLim(wMaxLimPct, 1);
            }
        } else {
            // Kein Setpoint → Begrenzung deaktivieren
            this.writeWMaxLim(W_MAX_LIM_100_PERCENT, 0);
        }
    }

    private Integer convertWattsToPct(int limitW) {
        try {
            var wMaxChannel = this.parent.getWMaxChannel();
            Float wMax = wMaxChannel.getNextValue().isDefined()
                    ? (Float) wMaxChannel.getNextValue().get()
                    : wMaxChannel.value().isDefined()
                            ? (Float) wMaxChannel.value().get()
                            : null;

            if (wMax == null || wMax <= 0) {
                return null;
            }

            int pct = (int) Math.round(
                    (double) clamp(limitW, 0, Math.round(wMax))
                            / wMax
                            * W_MAX_LIM_100_PERCENT
            );

            return clamp(pct, 0, W_MAX_LIM_100_PERCENT);

        } catch (OpenemsException e) {
            return null;
        }
    }

    private void writeWMaxLim(int wMaxLimPct, int ena) {

        wMaxLimPct = clamp(wMaxLimPct, 0, W_MAX_LIM_100_PERCENT);
        ena = ena == 0 ? 0 : 1;

        long now = System.currentTimeMillis();

        boolean enaChanged = ena != this.lastWMaxLimEna;
        boolean pctChanged = this.lastWMaxLimPct == null
                || Math.abs(wMaxLimPct - this.lastWMaxLimPct) >= W_MAX_LIM_HYSTERESIS;
        boolean keepAlive = now - this.lastWMaxLimWriteMillis >= W_MAX_LIM_KEEP_ALIVE_MS;

        if (!enaChanged && !pctChanged && !keepAlive) {
            return;
        }

        try {
            this.parent.writeWMaxLimPct(wMaxLimPct);
            this.parent.writeWMaxLimEna(ena);
            this.parent._setDebugWMaxLimPct(wMaxLimPct);
            this.parent._setDebugWMaxLimEna(ena);

            this.lastWMaxLimPct = wMaxLimPct;
            this.lastWMaxLimEna = ena;
            this.lastWMaxLimWriteMillis = now;

        } catch (OpenemsNamedException e) {
            // S123 nicht verfügbar – kein fataler Fehler, Batteriesteuerung läuft weiter
        }
    }
}