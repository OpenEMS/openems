package io.openems.api.device.nature.realtimeclock;

import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.DeviceNature;

public interface RealTimeClockNature extends DeviceNature {
	public WriteChannel<Long> rtcYear();

	public WriteChannel<Long> rtcMonth();

	public WriteChannel<Long> rtcDay();

	public WriteChannel<Long> rtcHour();

	public WriteChannel<Long> rtcMinute();

	public WriteChannel<Long> rtcSecond();
}
