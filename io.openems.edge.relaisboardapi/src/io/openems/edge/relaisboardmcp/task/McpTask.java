package io.openems.edge.relaisboardmcp.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;

public abstract class McpTask {

    private String relaisBoard;

    public McpTask(String relaisBoard) {
        this.relaisBoard = relaisBoard;
    }

    public abstract int getPosition();

    public abstract WriteChannel<Boolean> getWriteChannel();


}