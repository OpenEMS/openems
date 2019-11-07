package io.openems.edge.relais;

import io.openems.edge.common.channel.WriteChannel;

public interface RelaisActuator {

WriteChannel<Boolean> getRelaisChannelValue();
String getRelaisId();
boolean isCloser();
}
