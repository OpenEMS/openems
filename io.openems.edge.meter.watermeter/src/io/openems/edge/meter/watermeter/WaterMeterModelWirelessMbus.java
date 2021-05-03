package io.openems.edge.meter.watermeter;

/**
 * Water meter models with their record positions for WMbus.
 */
public enum WaterMeterModelWirelessMbus {
    AUTOSEARCH(0, 1),
    RELAY_PADPULS_M2W_CHANNEL1(0, 1),
    RELAY_PADPULS_M2W_CHANNEL2(0, 1),
    ENGELMANN_WATERSTAR_M(19, 18),
    ;


    int volumeCounterPosition;
    int timeStampPosition;

    WaterMeterModelWirelessMbus(int volume, int time) {
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
