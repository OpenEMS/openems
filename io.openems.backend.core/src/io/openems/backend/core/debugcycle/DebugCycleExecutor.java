package io.openems.backend.core.debugcycle;

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

import io.openems.backend.common.debugcycle.DebugCycle;
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
	private volatile List<DebugCycle> debugCycledObjects;

	private final ScheduledExecutorService debugCycleScheduledExecutor = Executors.newSingleThreadScheduledExecutor();

	@Activate
	public DebugCycleExecutor() {
		this.debugCycleScheduledExecutor.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
	}

	@Deactivate
	public void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugCycleScheduledExecutor, 0);
	}

	@Override
	public void run() {
		for (var debugCycle : this.debugCycledObjects) {
			try {
				debugCycle.debugCycle();
			} catch (Exception e) {
				this.log.warn("An Exception occured while executing " + debugCycle, e);
			}
		}
	}

}
