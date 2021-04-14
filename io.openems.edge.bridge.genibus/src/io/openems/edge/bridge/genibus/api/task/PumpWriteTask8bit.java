package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;

public class PumpWriteTask8bit extends PumpWriteTask16bitOrMore {

    public PumpWriteTask8bit(int address, int headerNumber, WriteChannel<Double> channel, String unitString, Priority priority, double channelMultiplier) {
        super(1, address, headerNumber, channel, unitString, priority, channelMultiplier);
    }

    public PumpWriteTask8bit(int address, int headerNumber, WriteChannel<Double> channel, String unitString, Priority priority) {
        this(address, headerNumber, channel, unitString, priority, 1);
    }

}
