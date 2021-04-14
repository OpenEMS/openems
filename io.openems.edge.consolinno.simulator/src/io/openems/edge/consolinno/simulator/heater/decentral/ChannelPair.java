package io.openems.edge.consolinno.simulator.heater.decentral;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;

import java.util.Optional;

class ChannelPair {
    private Channel<Boolean> needHeatChannel;
    private WriteChannel<Boolean> heatEnableChannel;

    private boolean hasRequest;
    private boolean isEnabled;
    private static final int MAX_CYCLES_TO_WAIT = 36;
    private int currentWait = 0;

    ChannelPair(Channel<Boolean> needHeatChannel, WriteChannel<Boolean> heatEnableChannel) {
        this.needHeatChannel = needHeatChannel;
        this.heatEnableChannel = heatEnableChannel;
    }

    Channel<Boolean> getNeedHeatChannel() {
        return needHeatChannel;
    }

    WriteChannel<Boolean> getHeatEnableChannel() {
        return heatEnableChannel;
    }

    void updateValues() {
        if (this.needHeatChannel.value().isDefined()) {
            this.hasRequest = this.needHeatChannel.value().get();
        }
        Optional<Boolean> enabled = heatEnableChannel.getNextWriteValueAndReset();
        if (enabled.isPresent()) {
            this.isEnabled = enabled.get();
        } else if (currentWait < MAX_CYCLES_TO_WAIT) {
            currentWait++;
            this.isEnabled = false;
            if (currentWait <= MAX_CYCLES_TO_WAIT / 2) {
                this.isEnabled = true;
            }
        } else {
            this.isEnabled = true;
            currentWait = 0;
        }
    }

    boolean hasRequest() {
        return this.hasRequest;
    }

    boolean isEnabledSignal() {
        return this.isEnabled;
    }
}
