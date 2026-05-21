package io.openems.edge.battery.fenecon.home.update;

import io.openems.edge.battery.fenecon.home.BatteryFeneconHome;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType;
import io.openems.edge.battery.fenecon.home.TwoPartVersion;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.startstop.StartStop;

public interface BatteryData {
	/**
	 * Returns the tower 0 software version of the battery.
	 * 
	 * @return TwoPartVersion
	 */
	TwoPartVersion getVersion();

	/**
	 * Returns the hardware type of the battery.
	 * 
	 * @return Enum
	 */
	BatteryFeneconHomeHardwareType getBatteryType();

	/**
	 * Returns true if the battery is fully running.
	 * 
	 * @return true if the battery is fully running.
	 */
	boolean isBatteryRunning();

	/**
	 * Creates BatteryData object by battery reference.
	 * 
	 * @param battery battery to get data from
	 * @return BatteryData object
	 */
	public static BatteryData byBattery(BatteryFeneconHome battery) {
		return new BatteryDataImpl(battery);
	}

	static class BatteryDataImpl implements BatteryData {
		private final BatteryFeneconHome battery;

		public BatteryDataImpl(BatteryFeneconHome battery) {
			this.battery = battery;
		}

		@Override
		public TwoPartVersion getVersion() {
			var version = this.battery
					.<StringReadChannel>channel(BatteryFeneconHome.ChannelId.TOWER_0_BMS_SOFTWARE_VERSION).value()
					.get();
			return TwoPartVersion.fromString(version);
		}

		@Override
		public BatteryFeneconHomeHardwareType getBatteryType() {
			return this.battery.getBatteryHardwareType();
		}

		@Override
		public boolean isBatteryRunning() {
			return this.battery.getStartStop() == StartStop.START;
		}
	}
}
