package io.openems.edge.consolinno.leaflet.core;

import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;

/**
 * A ModuleRegister. Saves the Leaflet-Module Type, Module Number and mReg.
 */
class ModuleRegister {


    private final LeafletCore.ModuleType type;
    private final int moduleNumber;
    private final int mReg;

    /**
     * This Object contains the information of one Table entry (without the Register because
     * this object is designed to be put in a Map where the value is the Register).
     *
     * @param moduleType         Type of the Module(e.g TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param mReg         Usually the Position but sometimes Position-1. Check Register Map
     */
    public ModuleRegister(LeafletCore.ModuleType moduleType, int moduleNumber, int mReg) {
        this.type = moduleType;
        this.moduleNumber = moduleNumber;
        this.mReg = mReg;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof ModuleRegister) {
            ModuleRegister otherObject = (ModuleRegister) o;
            return otherObject.moduleNumber == this.moduleNumber && otherObject.mReg == this.mReg && otherObject.type == this.type;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (this.type.toString() + this.moduleNumber + this.mReg).hashCode();
    }
}
