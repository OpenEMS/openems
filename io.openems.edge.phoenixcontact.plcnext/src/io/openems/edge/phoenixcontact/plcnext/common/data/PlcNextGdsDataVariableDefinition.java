package io.openems.edge.phoenixcontact.plcnext.common.data;

import java.util.List;

/**
 * TODO
 */
import io.openems.edge.common.channel.ChannelId;

public interface PlcNextGdsDataVariableDefinition {

	String getIdentifier();

	List<ChannelId> getOpenEmsChannelIds();

}