package io.openems.edge.raspberrypi.sensor.api.Adc.Pins;

import java.util.ArrayList;
import java.util.List;

public enum PinList {
    //Long necessary or is int/byte enough?
    Mcp_3208(new long[] {0x060000, 0x064000, 0x068000, 0x06C000, 0x070000, 0x074000, 0x078000, 0x07C000});

    private List<Long> PinList=new ArrayList<>();

    private PinList(long[] values){
        for(short x=0; x<values.length;x++) {
            PinList.add(values[x]);
        }
    }

    public List<Long> getPinList() {
        return PinList;
    }
}
