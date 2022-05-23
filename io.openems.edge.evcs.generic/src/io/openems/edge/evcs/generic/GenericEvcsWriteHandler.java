package io.openems.edge.evcs.generic;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.Status;

/**
 * This WriteHandler writes the Values from the Internal(GenericEvcs) Channels that where retrieved over Modbus
 * into the correct OpenEms Channels.
 * External READ_ONLY Register -> Internal OpenEms
 */
public class GenericEvcsWriteHandler {
    private final GenericEvcsImpl parent;
    private int scaleFactor;
    private boolean status;
    private boolean power;
    private int gridVoltage;

    GenericEvcsWriteHandler(GenericEvcsImpl parent, int scaleFactor, boolean status, boolean power) {
        this.parent = parent;
        this.scaleFactor = scaleFactor;
        this.status = status;
        this.power = power;
        this.gridVoltage = parent.getGridVoltage();
    }

    void run() {
        this.setPhaseCount();
        if (this.power) {
            this.parent._setChargePower(this.parent.getApparentPower() * this.scaleFactor);
        } else {
            this.parent._setChargePower((this.parent.getCurrentL1() + this.parent.getCurrentL2() + this.parent.getCurrentL3()) * this.gridVoltage);
        }
        this.parent._setChargingType(ChargingType.AC);
        this.setStatus();
    }

    private void setStatus() {
        if (this.status) {
            String status = this.parent.getGenericEvcsStatus();
            switch (status) {
                case "A":
                    this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
                    break;
                case "B":
                    this.parent._setStatus(Status.READY_FOR_CHARGING);
                    break;
                case "C":
                case "D":
                    this.parent._setStatus(Status.CHARGING);
                    break;
                case "E":
                case "F":
                    this.parent._setStatus(Status.ERROR);
                    break;
            }
        } else {
            if ((this.parent.getCurrentL1() + this.parent.getCurrentL2() + this.parent.getCurrentL3()) > 0) {
                this.parent._setStatus(Status.CHARGING);
            } else {
                this.parent._setStatus(Status.UNDEFINED);
            }
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


