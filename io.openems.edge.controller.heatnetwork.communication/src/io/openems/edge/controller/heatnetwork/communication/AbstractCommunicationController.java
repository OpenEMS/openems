package io.openems.edge.controller.heatnetwork.communication;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationController;

abstract class AbstractCommunicationController extends AbstractOpenemsComponent implements OpenemsComponent, CommunicationController {

    AbstractCommunicationController(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                    io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


}
