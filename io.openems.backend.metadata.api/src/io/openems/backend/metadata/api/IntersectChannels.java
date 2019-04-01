package io.openems.backend.metadata.api;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

import java.util.TreeSet;

public interface IntersectChannels {

    /**
     * This method checks which channels are allowed to subscribe to
     *
     * @param userId
     * @param edgeId
     * @param requestedChannels
     * @return The intersection of the requested and the allowed channels
     */
    TreeSet<ChannelAddress> checkChannels(String userId, String edgeId, TreeSet<ChannelAddress> requestedChannels) throws OpenemsException;

}
