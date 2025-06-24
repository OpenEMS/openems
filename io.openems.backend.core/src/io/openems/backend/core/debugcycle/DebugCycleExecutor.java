package io.openems.backend.core.debugcycle;

import java.time.ZonedDateTime;
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

import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.debugcycle.MetricsConsumer;
import io.openems.common.utils.ThreadPoolUtils;

@Component(//
		immediate = true, //
		scope = ServiceScope.SINGLETON //
)
public class DebugCycleExecutor implements Runnable {

	private final Logger log = LoggerFactory.getLogger(DebugCycleExecutor.class);
	
	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private volatile List<MetricsConsumer> debugCycleConsumer;
	
	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private volatile List<DebugLoggable> debugCycledObjects;

	private final ScheduledExecutorService debugCycleScheduledExecutor = Executors.newSingleThreadScheduledExecutor();

	@Activate
	public DebugCycleExecutor() {
		final int updateInterval = 5;
		this.log.info("Update metrics all {} Seconds", updateInterval);
		this.debugCycleScheduledExecutor.scheduleAtFixedRate(this, updateInterval, updateInterval, TimeUnit.SECONDS);
	}

	/**
	 * Deactivate method.
	 */
	@Deactivate
	public void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugCycleScheduledExecutor, 1);
	}

	@Override
	public void run() {
		final var now = ZonedDateTime.now();
		if (this.debugCycleConsumer.isEmpty()) {
			return;
		}
		
		for (var debugCycle : this.debugCycledObjects) {
			// handle console logs
			try {
				final var debugLog = debugCycle.debugLog();
				if (debugLog != null) {
					this.log.info(debugLog);
				}
			} catch (Exception e) {
				this.log.warn("An Exception occurred while getting debugLog from " + debugCycle, e);
			}

			// handle database metrics
			final var metrics = debugCycle.debugMetrics();
			if (metrics == null || metrics.isEmpty())  {
				return;
			}
			
			for (var consumer : this.debugCycleConsumer) {
				try {				
					consumer.consumeMetrics(now, metrics);
				} catch (Throwable e) {
					this.log.warn("An Exception occurred while getting debugMetrics from {}", debugCycle, e);
				}
			}
		}
	}

}
