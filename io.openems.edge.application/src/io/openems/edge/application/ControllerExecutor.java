package io.openems.edge.application;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.Scheduler;

@Component(immediate = true)
public class ControllerExecutor extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(ControllerExecutor.class);

	/**
	 * Holds the Schedulers and their relative cycleTime. They are sorted ascending
	 * by their cycleTimes.
	 */
	private final TreeMap<Scheduler, Integer> schedulers = new TreeMap<Scheduler, Integer>(
			(a, b) -> a.getCycleTime() - b.getCycleTime());

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	private int commonCycleTime = Scheduler.DEFAULT_CYCLE_TIME;
	private int maxCycles = 1;
	private int cycle = 0;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addScheduler(Scheduler newScheduler) {
		synchronized (this.schedulers) {
			this.schedulers.put(newScheduler, 1); // relativeCycleTime is going to be overwritten by
													// recalculateCommonCycleTime
			this.recalculateCommonCycleTime();
		}
	}

	protected void removeScheduler(Scheduler scheduler) {
		this.schedulers.remove(scheduler);
		this.recalculateCommonCycleTime();
	}

	/**
	 * Called on change of Scheduler list: recalculates the commonCycleTime and all
	 * relativeCycleTimes
	 */
	private void recalculateCommonCycleTime() {
		// find greatest common divisor -> commonCycleTime
		int[] cycleTimes = new int[this.schedulers.size()];
		{
			int i = 0;
			for (Scheduler scheduler : this.schedulers.keySet()) {
				cycleTimes[i++] = scheduler.getCycleTime();
			}
		}
		this.commonCycleTime = Utils.getGreatestCommonDivisor(cycleTimes).orElse(Scheduler.DEFAULT_CYCLE_TIME);
		// fix relative cycleTime for all existing schedulers
		int[] relativeCycleTimes = new int[this.schedulers.size()];
		{
			int i = 0;
			for (Scheduler scheduler : this.schedulers.keySet()) {
				int relativeCycleTime = scheduler.getCycleTime() / this.commonCycleTime;
				this.schedulers.put(scheduler, relativeCycleTime);
				relativeCycleTimes[i++] = relativeCycleTime;
			}
		}
		// find least common multiple of relativeCycleTimes
		this.maxCycles = Utils.getLeastCommonMultiple(relativeCycleTimes).orElse(1);
	}

	@Activate
	protected void activate() {
		super.activate("ControllerExecutor");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected int getCycleTime() {
		return this.commonCycleTime;
	}

	@Override
	protected void forever() {
		// handle cycle number
		if (++this.cycle > this.maxCycles) {
			this.cycle = 1;
		}
		try {
			if (schedulers.isEmpty()) {
				log.warn("There are no Schedulers configured!");
				return;
			}
			log.info("===========");

			/*
			 * Before Controllers start: switch to next process image for each channel
			 */
			this.components.forEach(component -> {
				component.getChannels().forEach(channel -> {
					channel.nextProcessImage();
				});
			});

			/*
			 * Execute Schedulers and their Controllers
			 */
			for (Entry<Scheduler, Integer> entry : schedulers.entrySet()) {
				Scheduler scheduler = entry.getKey();
				if (cycle % entry.getValue() != 0) {
					// abort if relativeCycleTime is not matching this cycle
					continue;
				}
				log.info("Scheduler [" + scheduler.id() + "]");
				for (Controller controller : scheduler.getControllers()) {
					controller.run();
				}
			}
		} catch (Throwable t) {
			log.warn("Error in Scheduler. " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}
	}

}
