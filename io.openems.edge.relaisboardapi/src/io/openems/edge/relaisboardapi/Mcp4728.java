package io.openems.edge.relaisboardapi;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import io.openems.edge.relaisboardapi.task.McpTask;

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
    private int maxDigitValue = 4095;


    public Mcp4728(String address, String parentCircuitBoard, I2CBus device) throws IOException {
        values = new int[4];
        prevValues = new int[4];
        this.parentCircuitBoard = parentCircuitBoard;
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


    public void shift() {

        for (McpTask task : tasks.values()) {
            //-69 default value of digitValue in BhkwTask
            int digitValue = task.getDigitValue();
            if (digitValue != -69) {
                values[task.getPosition()] = digitValue;
                if (values[task.getPosition()] < 0) {
                    values[task.getPosition()] = 0;
                } else if (values[task.getPosition()] > this.maxDigitValue) {
                    values[task.getPosition()] = this.maxDigitValue;

                }
                if (values[task.getPosition()] != prevValues[task.getPosition()]) {
                    prevValues[task.getPosition()] = values[task.getPosition()];
                }
            } else {
                values[task.getPosition()] = prevValues[task.getPosition()];
            }
        }
        try {
            byte[] allVoltage = setAllVoltage();
            this.device.write(0x50, allVoltage);
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
        this.values[this.tasks.get(id).getPosition()] = 0;
        this.prevValues[this.tasks.get(id).getPosition()] = 0;
        this.tasks.remove(id);
    }

    public byte[] setAllVoltage() {

        byte[] allVoltage = new byte[8];
        int change1;
        int change2;
        for (short x = 0, y = 0; x < 8; y++) {
            change1 = this.values[y];
            change2 = this.values[y];
            allVoltage[x] = (byte) ((change1 >> 8) & 0xFF);
            x++;
            allVoltage[x] = (byte) (change2 & 0xFF);
            x++;
        }
        return allVoltage;
    }

    public void deactivate() {
        for (short x = 0; x < length; x++) {
            this.values[x] = 0;
            this.prevValues[x] = 0;
            try {
                byte[] allVoltage = setAllVoltage();
                this.device.write(0x50, allVoltage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getParentCircuitBoard() {
        return parentCircuitBoard;
    }

    @Override
    public void addToDefault(int position, boolean activate) {
        return;
    }

    @Override
    public void setPosition(int position, boolean activate) {
    }
}
