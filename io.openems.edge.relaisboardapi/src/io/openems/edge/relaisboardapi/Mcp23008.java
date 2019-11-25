package io.openems.edge.relaisboardapi;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import io.openems.edge.relaisboardapi.task.McpTask;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Mcp23008 extends Mcp implements McpChannelRegister {

    private final String address;
    private String parentCircuitBoard;
    private final int length = 8;
    private final I2CDevice device;
    private Map<Integer, Boolean> valuesPerDefault = new ConcurrentHashMap<>();
    private final boolean[] shifters;
    private boolean[] wasOpenerAndActivated = new boolean[this.length];
    private final Map<String, McpTask> tasks = new ConcurrentHashMap<>();

    public Mcp23008(String address, I2CBus device, String parentCircuitBoard) throws IOException {
        this.address = address;
        this.parentCircuitBoard = parentCircuitBoard;
        this.shifters = new boolean[length];

        for (int i = 0; i < length; i++) {
            this.shifters[i] = false;
            this.wasOpenerAndActivated[i] = false;
        }
        switch (address) {
            case "0x22":
                this.device = device.getDevice(0x22);
                break;
            case "0x24":
                this.device = device.getDevice(0x24);
                break;
            case "0x26":
                this.device = device.getDevice(0x26);
                break;
            default:
                this.device = device.getDevice(0x20);
                break;
        }
        int zero = 0x00;
        int data = 0x00;
        try {
            this.device.write(0x00, (byte) data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setPosition(int position, boolean activate) {
        if (position < this.length) {
            this.shifters[position] = activate;
        } else {
            throw new IllegalArgumentException("There is no such position." + position + " maximum is " + this.length);
        }
    }

    public void shift() {
        for (McpTask task : tasks.values()) {
			task.getWriteChannel().getNextWriteValueAndReset()
					.ifPresent(aBoolean -> setPosition(task.getPosition(), aBoolean));
		}

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

    @Override
    public String getParentCircuitBoard() {
        return parentCircuitBoard;
    }

    @Override
    public void deactivate() {
        for (Map.Entry<Integer, Boolean> entry : getValuesPerDefault().entrySet()) {
            setPosition(entry.getKey(), entry.getValue());
        }
        shift();
    }

    @Override
    public void addTask(String id, McpTask mcpTask) {
        this.tasks.put(id, mcpTask);
    }

    @Override
    public void removeTask(String id) {
        setPosition(this.tasks.get(id).getPosition(), valuesPerDefault.get(this.tasks.get(id).getPosition()));
        this.tasks.get(id);
        this.tasks.remove(id);
    }
}
