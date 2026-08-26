package io.openems.backend.common.debugcycle;

import java.time.ZonedDateTime;
import java.util.Map;

import com.google.gson.JsonElement;

public interface MetricsConsumer {

	/**
	 * This method is called when the DebugCycle is running.
	 *
	 * @param now     the current timestamp when the metrics are consumed
	 * @param metrics the metrics to be consumed
	 */
	public void consumeMetrics(ZonedDateTime now, Map<String, JsonElement> metrics);

}
