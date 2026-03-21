package io.openems.edge.core.cycle;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import info.faljse.SDNotify.SDNotify;
import io.openems.common.event.EventBuilder;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.utils.DebugUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;

public class CycleWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(CycleWorker.class);
	private final CycleImpl parent;

	public CycleWorker(CycleImpl parent) {
		this.parent = parent;
	}

	@Override
	protected int getCycleTime() {
		return this.parent.getCycleTime();
	}

	@Override
	protected void forever() {
		final var verbosity = this.parent.getLogVerbosity();

		// Prepare Cycle-Time measurement
		var stopwatch = Stopwatch.createStarted();

		// Kick Operating System Watchdog
		var socketName = System.getenv().get("NOTIFY_SOCKET");
		if (socketName != null && !socketName.isEmpty()) {
			if (SDNotify.isAvailable()) {
				SDNotify.sendWatchdog();
			}
		}

		try {
			/*
			 * Trigger BEFORE_PROCESS_IMAGE event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);

			/*
			 * Before Controllers start: switch to next process image for each channel
			 */
			Stream.concat(//
					this.parent.componentManager.getEnabledComponents().stream(), //
					Stream.of(this.parent)//
			)//
					.filter(c -> c.isEnabled() && !(c instanceof Sum))//
					.forEach(component -> this.triggerComponentEvent(verbosity, component));

			/*
			 * Update the Channels in the Sum-Component.
			 */
			this.executeCycleStep(verbosity, CycleLogVerbosity.PHASES, "Update the Channels in the Sum-Component",
					() -> {
						this.parent.sumComponent.updateChannelsBeforeProcessImage();
						this.parent.sumComponent.channels().forEach(channel -> channel.nextProcessImage());
					});

			/*
			 * Trigger AFTER_PROCESS_IMAGE event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE);

			/*
			 * Trigger BEFORE_CONTROLLERS event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS);

			/*
			 * Execute Schedulers and their Controllers
			 */
			var hasDisabledController = this.executeSchedulersWithOptionalMeasure(verbosity);

			// announce ignoring disabled Controllers
			this.parent._setIgnoreDisabledController(hasDisabledController);

			/*
			 * Trigger AFTER_CONTROLLERS event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS);

			/*
			 * Trigger BEFORE_WRITE event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE);

			/*
			 * Trigger EXECUTE_WRITE event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE);

			/*
			 * Trigger AFTER_WRITE event
			 */
			this.triggerEvent(verbosity, EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE);

		} catch (Throwable t) {
			this.parent.logWarn(this.log,
					"Error in Scheduler. " + t.getClass().getSimpleName() + ": " + t.getMessage());
			if (t instanceof ClassCastException || t instanceof NullPointerException) {
				t.printStackTrace();
			}
		}

		// Measure actual Cycle-Time
		var totalMs = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		this.parent._setMeasuredCycleTime(totalMs);
		if (verbosity.ordinal() >= CycleLogVerbosity.SUMMARY.ordinal()) {
			this.log.info("_cycle [MeasuredCycleTime: " + totalMs + " ms]");
		}
	}

	/**
	 * Executes a Cycle-Step with optional measurement and error handling.
	 * 
	 * @param current  the current cycle log verbosity
	 * @param required the required verbosity to measure this step
	 * @param name     the name for the measurement log
	 * @param runnable the task to execute
	 */
	private void executeCycleStep(CycleLogVerbosity current, CycleLogVerbosity required, String name,
			ThrowingRunnable<Exception> runnable) {
		try {
			if (current.ordinal() < required.ordinal()) {
				runnable.run();
				return;
			}
			DebugUtils.measure(this.log, name, runnable);
		} catch (Exception e) {
			this.log.warn("Error in Cycle-Step [" + name + "]: " + e.getMessage(), e);
		}
	}

	private void triggerEvent(CycleLogVerbosity verbosity, String topic) {
		this.executeCycleStep(verbosity, CycleLogVerbosity.PHASES, topic,
				() -> EventBuilder.send(this.parent.eventAdmin, topic));
	}

	private void triggerComponentEvent(CycleLogVerbosity verbosity, OpenemsComponent component) {
		this.executeCycleStep(verbosity, CycleLogVerbosity.COMPONENTS, "[Component Channel Update] " + component.id(),
				() -> component.channels().forEach(channel -> channel.nextProcessImage()));
	}

	private boolean executeSchedulersWithOptionalMeasure(CycleLogVerbosity verbosity) {
		if (this.parent.schedulers.isEmpty()) {
			this.parent.logWarn(this.log, "There are no Schedulers configured!");
			return false;
		}

		var hasDisabledController = false;

		for (var scheduler : this.parent.schedulers) {
			var schedulerControllerIsMissing = false;

			for (var controllerId : scheduler.getControllers()) {
				try {
					var component = this.parent.componentManager.getPossiblyDisabledComponent(controllerId);
					if (!(component instanceof Controller controller)) {
						this.parent.logWarn(this.log, "Scheduler [" + scheduler.id() + "]: Controller [" + controllerId
								+ "] is not a Controller!");
						schedulerControllerIsMissing = true;
						continue;
					}

					if (!controller.isEnabled()) {
						hasDisabledController = true;
						continue;
					}

					try {
						this.executeCycleStep(verbosity, CycleLogVerbosity.CONTROLLERS,
								"Controller [" + controller.id() + "]", controller::run);
						controller._setRunFailed(false);

					} catch (Exception e) {
						this.parent.logWarn(this.log, "Error in Controller [" + controller.id() + "]. "
								+ e.getClass().getSimpleName() + ": " + e.getMessage());
						if (e instanceof ClassCastException//
								|| e instanceof NullPointerException//
								|| e instanceof IllegalArgumentException//
						) {
							this.parent.logWarn(this.log, "Scheduler [" + scheduler.id() + "]: Controller ["
									+ controllerId + "] is missing. " + e.getMessage());
						}
						controller._setRunFailed(true);
					}
				} catch (Exception e) {
					this.parent.logWarn(this.log, "Scheduler [" + scheduler.id() + "]: Controller [" + controllerId
							+ "] is missing. " + e.getMessage());
					schedulerControllerIsMissing = true;
				}
			}

			scheduler._setControllerIsMissing(schedulerControllerIsMissing);
		}

		return hasDisabledController;
	}
}
