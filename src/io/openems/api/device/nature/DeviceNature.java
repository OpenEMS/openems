package io.openems.api.device.nature;

import io.openems.api.channel.Channel;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;

public interface DeviceNature extends Thing {

	void setAsRequired(Channel channel) throws ConfigException;

}
