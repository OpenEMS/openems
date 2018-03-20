package io.openems.edge.application;

import java.util.Map.Entry;
import java.util.TreeMap;

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

	private int commonCycleTime = Scheduler.DEFAULT_CYCLE_TIME;
	private int maxCycles = 1;
	private int cycle = 0;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addScheduler(Scheduler newScheduler) {
		synchronized (this.schedulers) {
			this.schedulers.put(newScheduler, 1); // relativeCycleTime is going to be overwritten
			this.recalculateCommonCycleTime();
		}
	}

	protected void removeScheduler(Scheduler scheduler) {
		this.schedulers.remove(scheduler);
		this.recalculateCommonCycleTime();
	}

	private void recalculateCommonCycleTime() {
		// find greatest common divisor -> commonCycleTime
		int[] cycleTimes = new int[this.schedulers.size()];
		{
			int i = 0;
			for (Scheduler scheduler : this.schedulers.keySet()) {
				cycleTimes[i++] = scheduler.getCycleTime();
			}
		}
		this.commonCycleTime = getGreatestCommonDivisor(cycleTimes);
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
		this.maxCycles = getLeastCommonMultiple(relativeCycleTimes);
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
			}
			log.info("===========");
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

	// Source: https://stackoverflow.com/a/4202114/4137113
	private static int getGreatestCommonDivisor(int a, int b) {
		while (b > 0) {
			int temp = b;
			b = a % b; // % is remainder
			a = temp;
		}
		return a;
	}

	private static int getGreatestCommonDivisor(int[] input) {
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = getGreatestCommonDivisor(result, input[i]);
		}
		return result;
	}

	private static int getLeastCommonMultiple(int a, int b) {
		return a * (b / getGreatestCommonDivisor(a, b));
	}

	private static int getLeastCommonMultiple(int[] input) {
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = getLeastCommonMultiple(result, input[i]);
		}
		return result;
	}
}
