package io.openems.edge.relaisboardmcp;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import io.openems.edge.relaisboardmcp.task.McpTask;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Mcp4728 extends Mcp implements McpChannelRegister {
    private String address;
    private String parentCircuitBoard;
    private final int length = 4;
    private I2CDevice device;
    private int[] values;
    private int[] prevValues;
    private Map<String, McpTask> tasks = new ConcurrentHashMap<>();


    public Mcp4728(String address, String parentCircuitBoard, I2CBus device) throws IOException {
        values = new int[4];
        prevValues = new int[4];

        for (short x = 0; x < length; x++) {
            values[x] = 0;
            prevValues[x] = 0;

        }
        this.address = address;
        switch (address) {
            case "0x60":
            default:
                this.device = device.getDevice(0x60);
                break;
        }
        int zero = 0x00;
        int data = 0x00;
        try {
            this.device.write(zero, (byte) data);
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void setPosition(int position, int value) {
        if (position >= 0 && position < length) {
            this.values[position] = value;
        } else {
            throw new IllegalArgumentException("There is no position: " + position + "max is " + (this.length - 1));
        }

    }

    @Override
    public void setPosition(int position, boolean activate) {

    }

    public void shift() {

        for (McpTask task : tasks.values()) {
            //-69 default value of digitValue in BhkwTask
            if (task.getDigitValue() != -69) {
                values[task.getPosition()] = task.getDigitValue();
                if (values[task.getPosition()] < 0) {
                    values[task.getPosition()] = 0;
                } else if (values[task.getPosition()] > 4095) {
                    values[task.getPosition()] = 4095;

                }
                if (values[task.getPosition()] != prevValues[task.getPosition()]) {
                    prevValues[task.getPosition()] = values[task.getPosition()];
                }
            } else {
                values[task.getPosition()] = prevValues[task.getPosition()];
            }
        }
        try {
            this.device.write(0x50, setAllVoltage());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void addTask(String id, McpTask mcpTask) {
        this.tasks.put(id, mcpTask);
    }

    @Override
    public void removeTask(String id) {
    this.tasks.remove(id);
    }

    public byte[] setAllVoltage() {
        byte[] setBytes = new byte[this.length - 1];

        for (short x = 0; x < this.length; x++) {
            setBytes[x] = (byte) ((this.values[x] >> 8) & 0xFF);
        }
        return setBytes;
    }


    @Override
    public void addToDefault(int position, boolean activate) {
        return;
    }
}
