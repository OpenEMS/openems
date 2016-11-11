package io.openems.api.device.nature.realtimeclock;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.DeviceNature;

public interface RealTimeClockNature extends DeviceNature {
	public ReadChannel<Long> rtcYear();

	public ReadChannel<Long> rtcMonth();

	public ReadChannel<Long> rtcDay();

	public ReadChannel<Long> rtcHour();

	public ReadChannel<Long> rtcMinute();

	public ReadChannel<Long> rtcSecond();
}
