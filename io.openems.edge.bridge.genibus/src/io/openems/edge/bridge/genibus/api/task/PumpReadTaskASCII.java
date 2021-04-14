package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

public class PumpReadTaskASCII extends AbstractPumpTask {

    private Channel<String> channel;

    private final Priority priority;
    private StringBuilder charStorage = new StringBuilder();

    public PumpReadTaskASCII(int address, int headerNumber, Channel<String> channel, String unitString, Priority priority) {
        super(address, headerNumber, unitString, 1);
        this.channel = channel;
        this.priority = priority;
    }

    @Override
    public void setResponse(byte data) {
        if (data != 0) {
            char dataInASCII = (char) data;
            charStorage.append(dataInASCII);
        } else {
            this.channel.setNextValue(charStorage);
            charStorage.delete(0, charStorage.length()-1);
        }
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
