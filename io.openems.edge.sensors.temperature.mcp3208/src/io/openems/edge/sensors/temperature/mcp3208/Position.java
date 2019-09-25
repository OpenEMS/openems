package io.openems.edge.sensors.temperature.mcp3208;

import com.pi4j.io.spi.SpiChannel;
import sun.security.provider.ConfigFile;

public enum Position {

    POS_1(SpiChannel.CS0, 0),
    POS_2(SpiChannel.CS0, 1),
    POS_3(SpiChannel.CS0, 2),
    POS_4(SpiChannel.CS0, 3),
    POS_5(SpiChannel.CS0, 4),
    POS_6(SpiChannel.CS0, 5),
    POS_7(SpiChannel.CS0, 6),
    POS_8(SpiChannel.CS0, 7),
    POS_9(SpiChannel.CS1, 0),
    POS_10(SpiChannel.CS1, 1),
    POS_11(SpiChannel.CS1, 2),
    POS_12(SpiChannel.CS1, 3),
    POS_13(SpiChannel.CS1, 4),
    POS_14(SpiChannel.CS1, 5),
    POS_15(SpiChannel.CS1, 6),
    POS_16(SpiChannel.CS1, 7);


    private final SpiChannel spiChannel;
    private final int port;

    private Position(SpiChannel spiChannel, int port) {
        this.spiChannel = spiChannel;
        this.port = port;
    }

    public SpiChannel getSpiChannel() {
        return spiChannel;
    }

    public int getPort() {
        return port;
    }
}