package io.openems.edge.bridge.genibus.api;

import io.openems.edge.common.channel.Channel;

public interface Genibus {

    void addDevice(PumpDevice pumpDevice);

    void removeDevice(String deviceId);

}
