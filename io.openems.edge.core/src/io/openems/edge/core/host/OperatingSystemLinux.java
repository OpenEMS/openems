package io.openems.edge.core.host;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sun.management.OperatingSystemMXBean;

public abstract class OperatingSystemLinux implements OperatingSystem {

	@Override
	public Optional<Double> getCpuLoad() {
		var bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		var load = bean.getCpuLoad();
		return load < 0 ? Optional.empty() : Optional.of(load);
	}

	@Override
	public Optional<MemoryInformation> getSystemMemory() {
		var memInfo = readMemInfo();
		var memAvailable = memInfo.get("MemAvailable");
		var memTotal = memInfo.get("MemTotal");

		var information = new MemoryInformation(memAvailable, memTotal);
		return Optional.of(information);
	}

	/**
	 * Reads the memory informtions from kernel file /proc/meminfo. All values are
	 * in kB.
	 * 
	 * @return Map with all memory informations.
	 */
	private static Map<String, Long> readMemInfo() {
		var result = new HashMap<String, Long>();

		try {
			try (var reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
				String line;
				while ((line = reader.readLine()) != null) {
					int splitter = line.indexOf(':');
					if (splitter == -1) {
						continue;
					}

					String key = line.substring(0, splitter).trim();
					String valueStr = line.substring(splitter + 1).trim().split(" ")[0];
					Long value;
					try {
						value = Long.parseLong(valueStr);
					} catch (NumberFormatException ex) {
						throw new RuntimeException(
								"Failed to parse value for key '" + key + "' in /proc/meminfo: " + valueStr, ex);
					}

					result.put(key, value);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read /proc/meminfo", ex);
		}

		return result;
	}
}
