package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

public class PumpReadTask8bit extends PumpReadTask16bitOrMore {

    public PumpReadTask8bit(int address, int headerNumber, Channel<Double> channel, String unitString, Priority priority, double channelMultiplier) {
        super(1, address, headerNumber, channel, unitString, priority, channelMultiplier);
    }

    public PumpReadTask8bit(int address, int headerNumber, Channel<Double> channel, String unitString, Priority priority) {
        this(address, headerNumber, channel, unitString, priority, 1);
    }
}
