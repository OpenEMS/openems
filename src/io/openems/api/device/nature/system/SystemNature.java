package io.openems.api.device.nature.system;

import java.net.Inet4Address;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.DeviceNature;

public interface SystemNature extends DeviceNature {

	/*
	 * Read Channels
	 */
	public ReadChannel<Inet4Address> primaryIpAddress();
}
