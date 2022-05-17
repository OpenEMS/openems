package io.openems.edge.evcs.easee;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

/**
 * This WriteHandler writes the Values from the Internal(Easee) Channels that where retrieved over the easee Cloud
 * into the correct OpenEms Channels.
 * External READ_ONLY Register -> Internal OpenEms
 */
public class EaseeWriteHandler {
    private final EaseeImpl parent;

    EaseeWriteHandler(EaseeImpl parent) {
        this.parent = parent;
    }

    void run() {
        this.setPhaseCount();
        this.parent._setChargingType(ChargingType.AC);
        this.setStatus();
    }

    /**
     * Sets the internal Status of the EVCS. Since the Easee Evcs does not have a register for this, this has to be done the "dirty" way.
     */
    private void setStatus() {
        if (this.parent.getCurrentSum() > 0) {
            this.parent._setStatus(Status.CHARGING);
        } else {
            this.parent._setStatus(Status.UNDEFINED);
        }
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getCurrentL1() >= 1) {
            phases += 1;
        }
        if (this.parent.getCurrentL2() >= 1) {
            phases += 1;
        }
        if (this.parent.getCurrentL3() >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);
    }

}


