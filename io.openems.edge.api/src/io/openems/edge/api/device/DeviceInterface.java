package io.openems.edge.api.device;

import java.util.Set;
import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.api.channel.ChannelInterface;
import io.openems.edge.api.message.Message;


@ProviderType
public interface DeviceInterface {

	String getId();
	
	DeviceState getState();
	
	Set<Message> getDeviceMessages();
	
	Set<ChannelInterface<?>> getDeviceChannels();
}
