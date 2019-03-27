package io.openems.edge.pvinverter.solarlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractWorker;

public class WatchdogWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(WatchdogWorker.class);

	private final SolarLog parent;
	private final int cycleTimeSeconds;

	public WatchdogWorker(SolarLog parent, int cycleTimeSeconds) {
		this.parent = parent;
		this.cycleTimeSeconds = cycleTimeSeconds;
	}

	@Override
	protected void forever() {
		try {
			this.parent.getWatchdogTagChannel().setNextWriteValue((int) System.currentTimeMillis());
		} catch (OpenemsNamedException e) {
			this.log.error("Unable to set SolarLog WatchDogTag: " + e.getMessage());
		}
	}

	@Override
	protected int getCycleTime() {
		return this.cycleTimeSeconds;
	}

}
