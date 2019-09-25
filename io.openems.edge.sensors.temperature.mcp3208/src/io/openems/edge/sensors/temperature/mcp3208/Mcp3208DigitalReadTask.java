package io.openems.edge.sensors.temperature.mcp3208;

import com.pi4j.io.spi.SpiChannel;

import io.openems.edge.bridge.spi.api.Board;
import io.openems.edge.bridge.spi.task.Task;
import io.openems.edge.common.channel.Channel;

public class Mcp3208DigitalReadTask extends Task {

    private final Channel<Integer> channel;
    private final int position;
    private final Board board;
//position == port of SPI Channel....position of input maybe? CH0-CH7 ?
    public Mcp3208DigitalReadTask(SpiChannel spiChannel, int position, Channel<Integer> channel, Board board) {
        super(spiChannel);
        this.position = position;
        this.channel = channel;
        this.board = board;
    }

    @Override
    public byte[] getRequest() {
        long n;
        switch (this.position) {
            case 0:
                n = 0x060000;
                break;
            case 1:
                n = 0x064000;
                break;
            case 2:
                n = 0x068000;
                break;
            case 3:
                n = 0x06C000;
                break;
            case 4:
                n = 0x070000;
                break;
            case 5:
                n = 0x074000;
                break;
            case 6:
                n = 0x078000;
                break;
            case 7:
            default:
                n = 0x07C000;
                break;
        }
        byte data[] = { 0, 0, 0 };
        for (int i = 0; i < 3; i++) {
            data[2 - i] = (byte) (n % 256);
            n = n >> 8;
        }
        return data;
    }

    @Override
    public void setResponse(byte[] data) {
        int digit = (data[1] << 8) + (data[2] & 0xFF);
        digit &= 0xFFF;
        int value = (int) (((this.board.getA() * (digit * digit)) + (this.board.getB() * digit) + this.board.getC())
                * 10);
        this.channel.setNextValue(value);
    }

}