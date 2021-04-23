package io.openems.edge.meter.watermeter;

public enum WaterMeterModelMbus {
    // Water meter models with their record positions for Mbus.
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

    public int getVolumeCounterPosition(){ return volumeCounterPosition; }
    public int getTimeStampPosition(){ return timeStampPosition; }
}
