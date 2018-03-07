package io.openems.edge.api.device;

import java.util.Set;
import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.api.channel.ChannelInterface;


@ProviderType
public interface DeviceInterface {

	String getId();
	
	DeviceState getState();
	
	Set<DeviceMessage> getDeviceMessages();
	
	Set<ChannelInterface<?>> getDeviceChannels();
}
