package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;

public class PumpCommandsTask extends AbstractPumpTask {

    private WriteChannel<Boolean> channel;

    public PumpCommandsTask(int address, int headerNumber, WriteChannel<Boolean> channel) {
        super(address, headerNumber, "", 1);
        this.channel = channel;
    }

    @Override
    public int getRequest(int byteCounter, boolean clearChannel) {
        if (this.channel.getNextWriteValue().isPresent()) {
            //for REST
            this.channel.setNextValue(this.channel.getNextWriteValue().get());
            if (this.channel.getNextWriteValue().get()) {
                // If the command is added to a telegram, reset channel to false to send command only once.
                if (clearChannel) {
                    try {
                        this.channel.setNextWriteValue(false);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        e.printStackTrace();
                    }
                }
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void setResponse(byte data) {
        //DO NOTHING
    }

    @Override
    public Priority getPriority() {
        // Commands task is always high priority.
        return Priority.HIGH;
    }
}
