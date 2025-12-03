package io.openems.edge.io.phoenixcontact.gds;

import java.util.Collection;

import io.openems.edge.common.channel.Channel;

public record PlcNextGdsDataClientConfig(String dataUrl, String dataInstanceName, Collection<Channel<?>> channels) {

}
