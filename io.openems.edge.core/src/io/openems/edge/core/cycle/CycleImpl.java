package io.openems.edge.core.cycle;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;

import io.openems.common.OpenemsConstants;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.scheduler.api.Scheduler;

@Component(//
		name = "Core.Cycle", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.CYCLE_ID, //
				"enabled=true" //
		})
public class CycleImpl extends AbstractOpenemsComponent implements OpenemsComponent, Cycle {

	private final CycleWorker worker = new CycleWorker(this);

	@Reference(policy = ReferencePolicy.STATIC)
	protected EventAdmin eventAdmin;

	/**
	 * Holds the Schedulers and their relative cycleTime. They are sorted ascending
	 * by their cycleTimes.
	 */
	protected final TreeMap<Scheduler, Integer> schedulers = new TreeMap<Scheduler, Integer>(
			(a, b) -> a.getCycleTime() - b.getCycleTime());

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Core.Cycle)))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	protected int commonCycleTime = Scheduler.DEFAULT_CYCLE_TIME;
	protected int maxCycles = 1;
	protected int cycle = 0;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addScheduler(Scheduler newScheduler) {
		if (newScheduler.isEnabled()) {
			synchronized (this.schedulers) {
				this.schedulers.put(newScheduler, 1); // relativeCycleTime is going to be overwritten by
														// recalculateCommonCycleTime
				this.commonCycleTime = Utils.recalculateCommonCycleTime(this.schedulers);
				this.maxCycles = Utils.recalculateRelativeCycleTimes(schedulers, this.commonCycleTime);
			}
		}
	}

	protected void removeScheduler(Scheduler scheduler) {
		this.schedulers.remove(scheduler);
		this.commonCycleTime = Utils.recalculateCommonCycleTime(this.schedulers);
		this.maxCycles = Utils.recalculateRelativeCycleTimes(schedulers, this.commonCycleTime);
	}

	public CycleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Cycle.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context) {
		super.activate(context, OpenemsConstants.CYCLE_ID, "Core.Cycle", true);
		this.worker.activate("Core.Cycle");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

}
