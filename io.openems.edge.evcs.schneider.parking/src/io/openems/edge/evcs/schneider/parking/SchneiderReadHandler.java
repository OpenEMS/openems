package io.openems.edge.evcs.schneider.parking;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

public class SchneiderReadHandler {
    SchneiderImpl parent;
    private int energySession;

    SchneiderReadHandler(SchneiderImpl parent) {
        this.parent = parent;
    }

    void run() {
        this.setPhaseCount();
        this.setStatus();
        //Schneider reads in kW
        this.parent._setChargePower((int) this.parent.getStationPowerTotal() * 1000);
        this.parent._setChargingType(ChargingType.AC);
        this.setEnergySession();
    }

    /**
     * Sets all the Channels regarding Energy.
     */
    private void setEnergySession() {
        int currentEnergy = this.parent.getStationEnergyLSB() + this.parent.getStationEnergyMSB();
        this.parent._setActiveConsumptionEnergy(currentEnergy);
        this.energySession += currentEnergy;
        this.parent._setEnergySession(this.energySession);

    }
    /**
     * Reads the Status from the ModbusRegister and interprets it into the CPWState and the OpenEms EVCS Status.
     */
    private void setStatus() {
        int status = this.parent.getCPWState();
        switch (status) {
            case 0:
            case 6:
            case 10:
                this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
                break;
            case 1:
            case 2:
            case 4:
            case 5:
            case 7:
            case 8:
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case 9:
            case 11:
                this.parent._setStatus(Status.CHARGING);
                break;
            case 12:
                this.parent._setStatus(Status.CHARGING_FINISHED);
                break;
            case 13:
            case 14:
                this.parent._setStatus(Status.ERROR);
                break;
            default:
                this.parent._setStatus(Status.UNDEFINED);
                break;
        }
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getStationIntensityPhaseX() >= 1) {
            phases += 1;
        }
        if (this.parent.getStationIntensityPhase2() >= 1) {
            phases += 1;
        }
        if (this.parent.getStationIntensityPhase3() >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);

    }


}
