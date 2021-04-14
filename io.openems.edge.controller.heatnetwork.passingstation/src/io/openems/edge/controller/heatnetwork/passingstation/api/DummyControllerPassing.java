package io.openems.edge.controller.heatnetwork.passingstation.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyControllerPassing extends AbstractOpenemsComponent implements OpenemsComponent, ControllerPassingChannel {

    public DummyControllerPassing(String id) {
        super(
                OpenemsComponent.ChannelId.values(),
                ControllerPassingChannel.ChannelId.values()

        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }
}
