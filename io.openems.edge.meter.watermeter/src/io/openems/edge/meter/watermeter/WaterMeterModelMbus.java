package io.openems.edge.meter.watermeter;

/**
 * Water meter models with their record positions for Mbus.
 */
public enum WaterMeterModelMbus {
    AUTOSEARCH(0, 1),
    PAD_PULS_M2(0,1),
    ITRON_BM_M(1,4),
    ;

    int volumeCounterPosition;
    int timeStampPosition;

    WaterMeterModelMbus(int volume, int time) {
        this.volumeCounterPosition = volume;
        this.timeStampPosition = time;
    }

    /**
     * Gets the record position for the volume counter.
     *
     * @return the record position.
     */
    public int getVolumeCounterPosition() {
        return this.volumeCounterPosition;
    }

    /**
     * Gets the record position for the timestamp.
     *
     * @return the record position.
     */
    public int getTimeStampPosition() { return this.timeStampPosition; }
}
