package io.openems.api.device.nature.ess;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.DeviceNature;

public interface EssNature extends DeviceNature {
	/*
	 * Constants
	 */
	public final int DEFAULT_MINSOC = 10;

	public final String OFF = "Off";
	public final String ON = "On";

	public final String OFF_GRID = "Off-Grid";
	public final String ON_GRID = "On-Grid";

	public final String STANDBY = "Standby";
	public final String START = "Start";
	public final String STOP = "Stop";

	/*
	 * Config
	 */
	public ConfigChannel<Integer> minSoc();

	/*
	 * Read Channels
	 */
	public ReadChannel<Long> gridMode();

	public ReadChannel<Long> soc();

	public ReadChannel<Long> systemState();

	public ReadChannel<Long> allowedCharge();

	public ReadChannel<Long> allowedDischarge();

	public StatusBitChannels warning();

	/*
	 * Write Channels
	 */
	public WriteChannel<Long> setWorkState();
}
