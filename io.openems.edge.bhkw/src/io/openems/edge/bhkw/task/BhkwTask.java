package io.openems.edge.bhkw.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.relaisboardmcp.task.McpTask;

public class BhkwTask extends McpTask {



    public BhkwTask(String id){
            super(id);
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public WriteChannel<Boolean> getWriteChannel() {
        return null;
    }
}
