package io.openems.edge.relaisBoard.api;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Mcp23008 extends Mcp implements McpChannelRegister {

    private final int address;
    private final int length = 8;
    private final I2CDevice device;
    Map<Integer, Boolean> valuesPerDefault = new ConcurrentHashMap<>();
    private final boolean[] shifters;

    public Mcp23008(int address, I2CBus device) throws IOException {
        this.address = address;
        this.shifters = new boolean[length];

        for (int i = 0; i < length; i++) {
            this.shifters[i] = false;
        }
        this.device = device.getDevice(address);
        this.device.write(0x20, (byte) 0x00);

    }


    public void setPosition(int position, boolean activate) {
        if (position < this.length) {
            this.shifters[position] = activate;
        } else {
            throw new IllegalArgumentException("There is no such position." + position + " maximum is " + this.length);
        }
    }

    public void shift() {
        byte data = 0x00;
        for (int i = length - 1; i >= 0; i--) {
            data = (byte) (data << 1);
            if (this.shifters[i]) {
                data += 1;
            }
        }
        try {
            device.write(0x09, (byte) data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addToDefault(int position, boolean activate) {
        this.valuesPerDefault.put(position, activate);
    }

    public Map<Integer, Boolean> getValuesPerDefault() {
        return valuesPerDefault;
    }
}
