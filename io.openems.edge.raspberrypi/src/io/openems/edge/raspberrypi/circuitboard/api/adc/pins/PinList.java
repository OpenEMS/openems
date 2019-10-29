package io.openems.edge.raspberrypi.circuitboard.api.adc.pins;

import java.util.ArrayList;
import java.util.List;

public enum PinList {

    Mcp_3208(new long[]{0x060000, 0x064000, 0x068000, 0x06C000, 0x070000, 0x074000, 0x078000, 0x07C000}, 12),
    Mcp_3204(new long[]{0x060000, 0x064000, 0x06800, 0x06C000}, 12);
    private List<Long> pinList = new ArrayList<>();
    private int inputType;

    private PinList(long[] values, int inputType) {
        for (long pinValue : values) {
            pinList.add(pinValue);
        }
        this.inputType = inputType;
    }

    public List<Long> getPinList() {
        return this.pinList;
    }

    public int getInputType() {
        return this.inputType;
    }
}
