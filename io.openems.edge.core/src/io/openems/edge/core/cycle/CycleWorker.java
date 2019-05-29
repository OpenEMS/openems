package io.openems.edge.core.cycle;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.faljse.SDNotify.SDNotify;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.scheduler.api.Scheduler;

public class CycleWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(CycleWorker.class);
	private final CycleImpl parent;

	private Instant startTime = null;

	public CycleWorker(CycleImpl parent) {
		this.parent = parent;
	}

	@Override
	protected int getCycleTime() {
		return this.parent.commonCycleTime;
	}

	@Override
	protected void forever() {
		// handle cycle number
		if (++this.parent.cycle > this.parent.maxCycles) {
			this.parent.cycle = 1;
		}

		// Kick Operating System Watchdog
		if (SDNotify.isAvailable()) {
			SDNotify.sendWatchdog();
		}

		try {
			/*
			 * Trigger BEFORE_PROCESS_IMAGE event
			 */
			this.parent.eventAdmin
					.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>()));

			/*
			 * Before Controllers start: switch to next process image for each channel
			 */
			this.parent.components.stream().filter(c -> c.isEnabled()).forEach(component -> {
				component.channels().forEach(channel -> {
					channel.nextProcessImage();
				});
			});
			this.parent.channels().forEach(channel -> {
				channel.nextProcessImage();
			});

			/*
			 * Trigger AFTER_PROCESS_IMAGE event
			 */
			this.parent.eventAdmin
					.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, new HashMap<>()));

			/*
			 * Trigger BEFORE_CONTROLLERS event
			 */
			this.parent.eventAdmin
					.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, new HashMap<>()));

			/*
			 * Execute Schedulers and their Controllers
			 */
			if (this.parent.schedulers.isEmpty()) {
				this.parent.logWarn(this.log, "There are no Schedulers configured!");
			} else {

				this.parent.schedulers.entrySet().forEach(entry -> {
					Scheduler scheduler = entry.getKey();
					if (this.parent.cycle % entry.getValue() != 0) {
						// abort if relativeCycleTime is not matching this cycle
						return;
					}
					scheduler.getControllers().stream().filter(c -> c.isEnabled()).forEachOrdered(controller -> {
						try {
							controller.run();

							// announce running was ok
							controller.getRunFailed().setNextValue(false);

						} catch (OpenemsNamedException e) {
							this.parent.logWarn(this.log,
									"Error in Controller [" + controller.id() + "]: " + e.getMessage());

							// announce running failed
							controller.getRunFailed().setNextValue(true);

						} catch (Exception e) {
							this.parent.logWarn(this.log, "Error in Controller [" + controller.id() + "]. "
									+ e.getClass().getSimpleName() + ": " + e.getMessage());
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
			this.parent.eventAdmin
					.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, new HashMap<>()));

			/*
			 * Trigger BEFORE_WRITE event
			 */
			this.parent.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, new HashMap<>()));

			/*
			 * Trigger EXECUTE_WRITE event
			 */
			this.parent.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, new HashMap<>()));

			/*
			 * Trigger AFTER_WRITE event
			 */
			this.parent.eventAdmin.sendEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, new HashMap<>()));

		} catch (Throwable t) {
			this.parent.logWarn(this.log,
					"Error in Scheduler. " + t.getClass().getSimpleName() + ": " + t.getMessage());
			if (t instanceof ClassCastException || t instanceof NullPointerException) {
				t.printStackTrace();
			}
		}

		// Measure actual cycle time
		Instant now = Instant.now();
		if (this.startTime != null) {
			this.parent.channel(Cycle.ChannelId.MEASURED_CYCLE_TIME)
					.setNextValue(Duration.between(this.startTime, now).toMillis());
		}
		this.startTime = now;
	}

}
