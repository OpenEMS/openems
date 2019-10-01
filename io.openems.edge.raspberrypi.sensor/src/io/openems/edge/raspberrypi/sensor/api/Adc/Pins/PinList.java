package io.openems.edge.raspberrypi.sensor.api.Adc.Pins;

import java.util.ArrayList;
import java.util.List;

public enum PinList {
    //Long necessary or is int/byte enough?
    Mcp_3208(new long[] {0x060000, 0x064000, 0x068000, 0x06C000, 0x070000, 0x074000, 0x078000, 0x07C000}, 12),
//Look at datasheet 1 1000 0000_0000_0000_00
    Mcp_3204(new long [] {0x060000, 0x064000, 0x06800,0x06C000}, 12);
    private List<Long> PinList=new ArrayList<>();
    private int inputType;
    private PinList(long[] values, int inputType){
        for(short x=0; x<values.length;x++) {
            PinList.add(values[x]);
        }
        this.inputType=inputType;
    }

    public List<Long> getPinList() {
        return this.PinList;
    }
    public int getInputType(){return this.inputType;}
}
