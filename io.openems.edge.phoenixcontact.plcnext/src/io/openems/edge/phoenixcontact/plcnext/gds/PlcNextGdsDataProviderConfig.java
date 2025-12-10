package io.openems.edge.phoenixcontact.plcnext.gds;

import java.util.Collection;

import io.openems.edge.common.channel.Channel;

public record PlcNextGdsDataProviderConfig(String dataUrl, String dataInstanceName, Collection<Channel<?>> channels) {

	public static final String PLC_NEXT_OPENEMS_COMPONENT_NAME = "OpenEMS_V1Component1";

}
