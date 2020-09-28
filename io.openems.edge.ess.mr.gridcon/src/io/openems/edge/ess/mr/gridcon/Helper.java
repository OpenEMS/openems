package io.openems.edge.ess.mr.gridcon;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.startstop.StartStop;

public class Helper {

	public static boolean isUndefined(Battery battery) {
		for (Channel<?> c : battery.channels()) {
			if (isBatteryApiChannel(c)) {
				if (c instanceof AbstractReadChannel<?, ?> && !(c instanceof WriteChannel<?>)) {
					if (!c.value().isDefined()) {
						System.out.println("Channel " + c + " is not defined!");
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isBatteryApiChannel(Channel<?> c) {
		for (io.openems.edge.common.channel.ChannelId id : Battery.ChannelId.values()) {
			if (id.equals(c.channelId())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isRunning(Battery battery) {
		return battery.getStartStop() == StartStop.START;
	}

	public static boolean isStopped(Battery battery) {
		return battery.getStartStop() == StartStop.STOP;
	}

	public static boolean isError(Battery battery) {
		return battery.hasFaults();
	}

	public static void startBattery(Battery battery) {
		if (battery != null && !Helper.isRunning(battery)) {
			try {
				battery.start();
			} catch (OpenemsNamedException e) {
				System.out.println("Was not able to start battery " + battery.id() + "!\n" + e.getMessage());
			}
		}

	}

	public static void stopBattery(Battery battery) {
		if (battery != null && Helper.isStopped(battery)) {
			try {
				battery.stop();
			} catch (OpenemsNamedException e) {
				System.out.println("Was not able to stop battery " + battery.id() + "!\n" + e.getMessage());
			}
		}

	}

}
