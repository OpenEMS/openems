package io.openems.backend.core.debugcycle;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.utils.ThreadPoolUtils;

@Component(//
		immediate = true, //
		scope = ServiceScope.SINGLETON //
)
public class DebugCycleExecutor implements Runnable {

	private static final String EDGE_ID = "backend0";

	private final Logger log = LoggerFactory.getLogger(DebugCycleExecutor.class);

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private volatile List<DebugLoggable> debugCycledObjects;

	@Reference
	private TimedataManager timedataManager;

	private final ScheduledExecutorService debugCycleScheduledExecutor = Executors.newSingleThreadScheduledExecutor();

	@Activate
	public DebugCycleExecutor() {
		this.debugCycleScheduledExecutor.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
	}

	/**
	 * Deactivate method.
	 */
	@Deactivate
	public void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugCycleScheduledExecutor, 0);
	}

	@Override
	public void run() {
		final var now = Instant.now().toEpochMilli();
		for (var debugCycle : this.debugCycledObjects) {
			// handle console logs
			try {
				final var debugLog = debugCycle.debugLog();
				if (debugLog != null) {
					this.log.info(debugLog);
				}
			} catch (Exception e) {
				this.log.warn("An Exception occured while getting debugLog from " + debugCycle, e);
			}

			// handle database metrics
			try {
				final var metrics = debugCycle.debugMetrics();
				if (metrics != null && !metrics.isEmpty()) {
					final var data = TreeBasedTable.<Long, String, JsonElement>create();
					for (var entry : metrics.entrySet()) {
						data.put(now, entry.getKey(), entry.getValue());
					}
					this.timedataManager.write(EDGE_ID, new TimestampedDataNotification(data));
				}
			} catch (Exception e) {
				this.log.warn("An Exception occured while getting debugMetrics from " + debugCycle, e);
			}
		}
	}

}
