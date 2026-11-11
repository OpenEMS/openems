package io.openems.edge.controller.ess.soc.pvcurtail;

import java.time.LocalTime;

import io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargePriority;
import io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargeSource;

/**
 * Scenario 2 (scheduled charge / discharge) set-point calculation, free of OSGi and devices.
 *
 * <p>
 * Sign conventions: BESS power + discharge / - charge, export at the POI positive, all values in W.
 * The PV limit always lets PV cover the load, the BESS and the allowed export, so whatever PV
 * produces beyond that is curtailed, never exported.
 */
final class Scenario2 {

    /** Window the current time is in. */
    enum Window {
        CHARGE, DISCHARGE, NONE
    }

    /** One cycle's measurements. */
    record Inputs(int pvW, int bessW, int loadW, int maxChargeW, int maxDischargeW) {
    }

    /** BESS set-point (+ discharge / - charge) and PV limit. */
    record Result(int bessW, int pvLimitW) {
    }

    private Scenario2() {
    }

    /**
     * Parses "HH:MM" (also "H:MM").
     *
     * @param hhmm the time
     * @return the time, or null if it is not a valid time
     */
    static LocalTime parseTime(String hhmm) {
        if (hhmm == null) {
            return null;
        }
        var parts = hhmm.trim().split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            return LocalTime.of(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Daily window [from, to). From = to (or an invalid time) disables it; to before from crosses midnight.
     *
     * @param from window start
     * @param to   window end, exclusive
     * @param now  current local time
     * @return true if now is inside the window
     */
    static boolean inWindow(LocalTime from, LocalTime to, LocalTime now) {
        if (from == null || to == null || from.equals(to)) {
            return false;
        }
        if (from.isBefore(to)) {
            return !now.isBefore(from) && now.isBefore(to);
        }
        return !now.isBefore(from) || now.isBefore(to);
    }

    /**
     * Outside the windows the POI is held at the lower of the two grid settings (equal settings:
     * the charge setting). GRID_ONLY charging never exports, so it counts as 0 export.
     *
     * @param chargeGridSettingW    charge grid setting
     * @param source                charge source
     * @param dischargeGridSettingW discharge grid setting
     * @return base export at the POI
     */
    static int baseExportW(int chargeGridSettingW, ChargeSource source, int dischargeGridSettingW) {
        int chargeExport = source == ChargeSource.GRID_ONLY ? 0 : chargeGridSettingW;
        return Math.min(chargeExport, dischargeGridSettingW);
    }

    /**
     * Charge window. The BESS never discharges.
     *
     * @param in              measurements
     * @param source          charge source
     * @param priority        PV_ONLY priority
     * @param gridSettingW    PV sources: maximum export; GRID_ONLY: import power
     * @param importLimitW    PV_AND_GRID / GRID_ONLY: maximum import
     * @param socLimitReached charging stopped at the SoC limit
     * @param headroomW       PV limit lead over the BESS while it ramps
     * @return BESS set-point and PV limit
     */
    static Result charge(Inputs in, ChargeSource source, ChargePriority priority, int gridSettingW,
            int importLimitW, boolean socLimitReached, int headroomW) {
        int maxCharge = Math.max(0, in.maxChargeW());
        if (source == ChargeSource.GRID_ONLY) {
            int importW = Math.min(gridSettingW, Math.max(0, importLimitW));
            int charge = socLimitReached ? 0 : clamp(importW - in.loadW(), 0, maxCharge);
            return new Result(-charge, 0);
        }

        int export = Math.max(0, gridSettingW);
        int pvBase = export + Math.max(0, in.loadW());
        if (socLimitReached) {
            return new Result(0, pvBase);
        }

        int surplus = Math.max(0, in.pvW() - in.loadW());
        int charge;
        if (source == ChargeSource.PV_AND_GRID) {
            // import = load + charge - PV must stay within the import limit
            charge = clamp(in.pvW() - in.loadW() + Math.max(0, importLimitW), 0, maxCharge);
        } else if (priority == ChargePriority.EXPORT_FIRST) {
            charge = clamp(surplus - export, 0, maxCharge);
        } else {
            charge = Math.min(surplus, maxCharge);
        }

        // PV may run ahead of the measured BESS charge by headroomW, never past the allowed charge
        int bessCharging = Math.max(0, -in.bessW());
        int pvForBess = Math.min(maxCharge, bessCharging + Math.max(0, headroomW));
        return new Result(-charge, pvBase + pvForBess);
    }

    /**
     * Discharge window: export held at the grid setting, PV first, the BESS covers the rest. The BESS
     * never charges.
     *
     * @param in              measurements
     * @param gridSettingW    export at the POI
     * @param socLimitReached discharging stopped at the SoC limit
     * @param headroomW       PV limit lead over the BESS while it ramps down
     * @return BESS set-point and PV limit
     */
    static Result discharge(Inputs in, int gridSettingW, boolean socLimitReached, int headroomW) {
        int export = Math.max(0, gridSettingW);
        int load = Math.max(0, in.loadW());
        if (socLimitReached) {
            return new Result(0, export + load);
        }
        int discharge = clamp(export + load - in.pvW(), 0, Math.max(0, in.maxDischargeW()));
        int bessDischarging = Math.max(0, in.bessW());
        // While the BESS discharges PV may rise by headroomW to take over from it
        int pvLimit = bessDischarging > 0 //
                ? Math.max(0, export + load - bessDischarging) + Math.max(0, headroomW)
                : export + load;
        return new Result(discharge, pvLimit);
    }

    /**
     * Outside the windows: the BESS holds the base export (charges surplus, covers deficit).
     *
     * @param in          measurements
     * @param baseExportW export held at the POI
     * @return BESS set-point (+ discharge / - charge)
     */
    static int balanceW(Inputs in, int baseExportW) {
        return clamp(baseExportW + in.loadW() - in.pvW(), -Math.max(0, in.maxChargeW()),
                Math.max(0, in.maxDischargeW()));
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
