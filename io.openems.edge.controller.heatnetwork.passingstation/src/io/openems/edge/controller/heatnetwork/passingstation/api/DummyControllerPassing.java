package io.openems.edge.controller.heatnetwork.passingstation.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyControllerPassing extends AbstractOpenemsComponent implements OpenemsComponent, ControllerPassing {

    public DummyControllerPassing(String id) {
        super(
                OpenemsComponent.ChannelId.values(),
                ControllerPassing.ChannelId.values()

        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }
}
