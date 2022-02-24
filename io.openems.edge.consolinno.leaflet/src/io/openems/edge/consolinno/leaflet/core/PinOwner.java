package io.openems.edge.consolinno.leaflet.core;

import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;

class PinOwner {
    private final LeafletCore.ModuleType type;
    private final int moduleNumber;
    private final int position;

    /**
     * This Object contains the information of one Device (this is an internal Object and is purely designed to
     * be used with a PositionMap).
     * @param moduleType Type of the Module (e.g. TEMP,RELAY,etc.)
     * @param moduleNumber Number of the module specified on the Device
     * @param position Pin position of the Device on the module
     */
    public PinOwner(LeafletCore.ModuleType moduleType, int moduleNumber, int position) {
        this.type = moduleType;
        this.moduleNumber = moduleNumber;
        this.position = position;

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof PinOwner) {
            PinOwner otherObject = (PinOwner) o;
            return otherObject.moduleNumber == this.moduleNumber && otherObject.position == this.position && otherObject.type == this.type;
        } else {
            return false;
        }
    }
}
