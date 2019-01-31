package io.openems.edge.core.cycle;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.faljse.SDNotify.SDNotify;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.worker.AbstractWorker;
import io.openems.edge.scheduler.api.Scheduler;

@Component(immediate = true)
public class Cycle extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(Cycle.class);

	@Reference(policy = ReferencePolicy.STATIC)
	private EventAdmin eventAdmin;

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

	@Activate
	protected void activate() {
		super.activate("ControllerExecutor");
	}

	@Deactivate
	public void deactivate() {
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

		// Kick Operating System Watchdog
		SDNotify.sendWatchdog();

		try {
			/*
			 * Trigger BEFORE_PROCESS_IMAGE event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>()));

			/*
			 * Before Controllers start: switch to next process image for each channel
			 */
			this.components.stream().filter(c -> c.isEnabled()).forEach(component -> {
				component.channels().forEach(channel -> {
					channel.nextProcessImage();
				});
			});

			/*
			 * Trigger AFTER_PROCESS_IMAGE event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, new HashMap<>()));

			/*
			 * Trigger BEFORE_CONTROLLERS event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, new HashMap<>()));

			/*
			 * Execute Schedulers and their Controllers
			 */
			if (schedulers.isEmpty()) {
				log.warn("There are no Schedulers configured!");
			} else {

				schedulers.entrySet().forEach(entry -> {
					Scheduler scheduler = entry.getKey();
					if (cycle % entry.getValue() != 0) {
						// abort if relativeCycleTime is not matching this cycle
						return;
					}
					scheduler.getControllers().stream().filter(c -> c.isEnabled()).forEachOrdered(controller -> {
						try {
							controller.run();

							// announce running was ok
							controller.getRunFailed().setNextValue(false);

						} catch (OpenemsNamedException e) {
							this.log.warn("Error in Controller [" + controller.id() + "]: " + e.getMessage());

							// announce running failed
							controller.getRunFailed().setNextValue(true);

						} catch (Exception e) {
							log.warn("Error in Controller [" + controller.id() + "]. " + e.getClass().getSimpleName()
									+ ": " + e.getMessage());
							if (e instanceof ClassCastException || e instanceof NullPointerException
									|| e instanceof IllegalArgumentException) {
								e.printStackTrace();
							}
							// announce running failed
							controller.getRunFailed().setNextValue(true);
						}
					});
				});
			}

			/*
			 * Trigger AFTER_CONTROLLERS event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, new HashMap<>()));

			/*
			 * Trigger BEFORE_WRITE event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, new HashMap<>()));

			/*
			 * Trigger EXECUTE_WRITE event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, new HashMap<>()));

			/*
			 * Trigger AFTER_WRITE event
			 */
			this.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, new HashMap<>()));

		} catch (Throwable t) {
			log.warn("Error in Scheduler. " + t.getClass().getSimpleName() + ": " + t.getMessage());
			if (t instanceof ClassCastException || t instanceof NullPointerException) {
				t.printStackTrace();
			}
		}
	}

}
