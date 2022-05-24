package io.openems.edge.consolinno.evcs.limiter;


import java.time.Instant;

class EvcsOnHold {
    private final int power;
    private final Instant timestamp;
    private final int phases;
    private boolean wantToCharge;

    /**
     * This Object contains the information to Identify a waiting EVCS (this is an internal Object and is designed to
     * be used in a Map where the key is the id).
     *
     * @param power     last known power request in A
     * @param timestamp time when the EVCS was turned off
     * @param phases    amount of phases the EVCS has
     */
    public EvcsOnHold(int power, Instant timestamp, int phases, boolean wantToCharge) {
        this.power = power;
        this.timestamp = timestamp;
        this.phases = phases;
    }

    public int getPower() {
        return this.power;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public int getPhases() {
        if (this.phases == 0) {
            return 1;
        }
        return this.phases;
    }

    public void setWantToCharge(boolean wantToCharge) {
        this.wantToCharge = wantToCharge;
    }

    public boolean getWantToCharge() {
        return this.wantToCharge;
    }
}
