package io.openems.edge.relais.api;

import io.openems.edge.common.channel.WriteChannel;

public interface RelaisActuator {

WriteChannel<Boolean> getRelaisChannelValue();
String getRelaisId();
boolean isCloser();
}
