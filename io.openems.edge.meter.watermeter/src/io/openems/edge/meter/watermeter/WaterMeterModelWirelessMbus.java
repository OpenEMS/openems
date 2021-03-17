package io.openems.edge.meter.watermeter;

public enum WaterMeterModelWirelessMbus {
    // Water meter models with their record positions for WMbus.
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

    public int getVolumeCounterPosition() {
        return volumeCounterPosition;
    }
    public int getTimeStampPosition() { return timeStampPosition; }
}
