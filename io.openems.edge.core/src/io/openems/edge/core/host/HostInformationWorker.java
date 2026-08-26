package io.openems.edge.core.host;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;

/**
 * This Worker constantly checks host informations like free disk space, ram
 * usage, etc..
 */
public class HostInformationWorker extends AbstractWorker {

	private static final int CYCLE_TIME = 1_000; // in ms
	private static final long MINIMUM_FREE_DISK_SPACE = 100L /* MB */ * 1024 /* kB */ * 1024 /* bytes */; // in bytes
	private static final int CYCLES_FOR_DISK_UPDATE = 300;
	private static final int CYCLES_FOR_CPU_TEMPERATURE_UPDATE = 3;
	private static final int CYCLES_FOR_CPU_LOAD_UPDATE = 3;

	private final Logger log = LoggerFactory.getLogger(HostInformationWorker.class);

	private final HostImpl parent;

	private int cycle = 0;

	public HostInformationWorker(HostImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		this.execute("Memory", this::updateMemoryInfo);

		if (this.cycle % CYCLES_FOR_DISK_UPDATE == 0) {
			this.execute("DiskUsage", this::updateDiskIsFull);
		}
		if (this.cycle % CYCLES_FOR_CPU_TEMPERATURE_UPDATE == 0) {
			this.execute("CpuTemperature", this::updateCpuTemperature);
		}
		if (this.cycle % CYCLES_FOR_CPU_LOAD_UPDATE == 0) {
			this.execute("CpuLoad", this::updateCpuLoad);
		}

		this.cycle++;
	}

	protected void execute(String name, Runnable func) {
		try {
			func.run();
		} catch (Exception e) {
			this.parent.logError(this.log, "Error while updating host information '" + name + "': " + e.getMessage());
		}
	}

	protected void updateDiskIsFull() {
		var totalUsableSpace = this.getTotalUsableSpace();
		this.parent._setDiskIsFull(totalUsableSpace < MINIMUM_FREE_DISK_SPACE);
	}

	private long getTotalUsableSpace() {
		var totalUsableSpace = 0L;
		for (Path root : FileSystems.getDefault().getRootDirectories()) {
			try {
				var store = Files.getFileStore(root);
				totalUsableSpace += store.getUsableSpace();
			} catch (IOException e) {
				this.parent.logInfo(this.log, "Unable to query disk space: " + e.getMessage());
			}
		}
		return totalUsableSpace;
	}

	protected void updateCpuTemperature() {
		if (this.parent.operatingSystem == null) {
			this.parent._setCpuTemperature(null);
			return;
		}

		var temperature = this.parent.operatingSystem.getCpuTemperature();
		this.parent._setCpuTemperature(temperature.orElse(null));
	}

	protected void updateCpuLoad() {
		if (this.parent.operatingSystem == null) {
			this.parent._setCpuLoad(null);
			return;
		}

		var cpuLoad = this.parent.operatingSystem.getCpuLoad() //
				.map(load -> (int) Math.floor(load * 100)) //
				.orElse(null);
		this.parent._setCpuLoad(cpuLoad);
	}

	protected void updateMemoryInfo() {
		if (this.parent.operatingSystem == null) {
			this.parent._setMemoryFree(null);
			this.parent._setMemoryUsage(null);
			return;
		}

		var memInfo = this.parent.operatingSystem.getSystemMemory();
		this.parent._setMemoryFree(memInfo //
				.map(OperatingSystem.MemoryInformation::availableMemoryInKBytes) //
				.orElse(null));
		this.parent._setMemoryUsage(memInfo //
				.map(i -> (double) i.availableMemoryInKBytes() / i.totalMemoryInKBytes())
				.map(i -> (int) Math.floor(i * 100)).orElse(null));
	}

	@Override
	protected int getCycleTime() {
		return CYCLE_TIME;
	}

}
