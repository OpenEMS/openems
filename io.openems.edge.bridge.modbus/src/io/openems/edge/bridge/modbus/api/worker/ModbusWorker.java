package io.openems.edge.bridge.modbus.api.worker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.worker.internal.CycleTasks;
import io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksManager;
import io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksSupplier;
import io.openems.edge.bridge.modbus.api.worker.internal.DefectiveComponents;

/**
 * The ModbusWorker schedules the execution of all Modbus-Tasks, like reading
 * and writing modbus registers.
 *
 * <p>
 * It tries to execute all Write-Tasks as early as possible (directly after the
 * TOPIC_CYCLE_EXECUTE_WRITE event) and all Read-Tasks as late as possible to
 * have values available exactly when they are needed (i.e. at the
 * TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event). For this it uses a
 * {@link CycleTasksManager} that internally uses a {@link CycleTasksSupplier}
 * that supplies the tasks for one Cycle ({@link CycleTasks}).
 */
public class ModbusWorker extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(ModbusWorker.class);

	// Callbacks
	private final ThrowingFunction<Task, Integer, OpenemsException> execute;
	private final Consumer<ModbusElement<?>[]> invalidate;
	private final BiConsumer<Logger, String> logWarn;

	private final AtomicReference<LogVerbosity> logVerbosity;
	private final DefectiveComponents defectiveComponents;
	private final CycleTasksSupplier cycleTasksSupplier;
	private final CycleTasksManager cycleTasksManager;

	/**
	 * Constructor for {@link ModbusWorker}.
	 * 
	 * @param execute                     executes a {@link Task}; returns number of
	 *                                    actually executed subtasks
	 * @param invalidate                  invalidates the given
	 *                                    {@link ModbusElement}s after read errors
	 * @param cycleTimeIsTooShortCallback sets the
	 *                                    {@link BridgeModbus.ChannelId#CYCLE_TIME_IS_TOO_SHORT}
	 *                                    channel
	 * @param logWarn                     logs a warning
	 * @param logVerbosity                the configured {@link LogVerbosity}
	 */
	public ModbusWorker(ThrowingFunction<Task, Integer, OpenemsException> execute,
			Consumer<ModbusElement<?>[]> invalidate, Consumer<Boolean> cycleTimeIsTooShortCallback,
			BiConsumer<Logger, String> logWarn, AtomicReference<LogVerbosity> logVerbosity) {
		this.execute = execute;
		this.invalidate = invalidate;
		this.logWarn = logWarn;
		this.logVerbosity = logVerbosity;

		this.defectiveComponents = new DefectiveComponents();
		this.cycleTasksSupplier = new CycleTasksSupplier();
		this.cycleTasksManager = new CycleTasksManager(this.cycleTasksSupplier, this.defectiveComponents,
				cycleTimeIsTooShortCallback, logVerbosity);
	}

	@Override
	protected void forever() throws InterruptedException {
		var task = this.cycleTasksManager.getNextTask();

		try {
			// execute the task
			var stopwatch = Stopwatch.createStarted();
			var noOfExecutedSubTasks = this.execute.apply(task);
			stopwatch.stop();

			if (noOfExecutedSubTasks > 0) {
				// TODO remove before merge
				this.log("  Execute " + task + "; elapsed: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
				// no exception & at least one sub-task executed
				this.markComponentAsDefective(task.getParent(), false);
			}

		} catch (OpenemsException e) {
			this.logWarn.accept(this.log, task.toString() + " execution failed: " + e.getMessage());
			this.markComponentAsDefective(task.getParent(), true);

			// invalidate elements of this task
			this.invalidate.accept(task.getElements());
		}
	}

	// TODO remove before release
	private void log(String message) {
		switch (this.logVerbosity.get()) {
		case DEV_REFACTORING:
			this.log.info(message);
			break;
		case NONE:
		case READS_AND_WRITES:
		case WRITES:
			break;
		}
	}

	/**
	 * Marks the given {@link ModbusComponent} as defective or non-defective.
	 * 
	 * <ul>
	 * <li>Sets 'ModbusCommunicationFailed' Channel of the ModbusComponent
	 * <li>Adds/Removes the component to/from the {@link DefectiveComponents}
	 * <li>Informs the {@link WaitHandler}
	 * </ul>
	 * 
	 * @param component   the {@link ModbusComponent}
	 * @param isDefective mark as defective (true) or non-defective (false)
	 */
	private void markComponentAsDefective(ModbusComponent component, boolean isDefective) {
		if (component != null) {
			if (isDefective) {
				// Component is defective
				this.defectiveComponents.add(component.id());
				component._setModbusCommunicationFailed(true);

			} else {
				// Read from/Write to Component was successful
				this.defectiveComponents.remove(component.id());
				component._setModbusCommunicationFailed(false);
			}
		}
	}

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.cycleTasksSupplier.addProtocol(sourceId, protocol);
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		this.cycleTasksSupplier.removeProtocol(sourceId);
	}

	/**
	 * Retry Modbus communication to given Component-ID.
	 * 
	 * <p>
	 * See {@link BridgeModbus#retryModbusCommunication(String)}
	 * 
	 * @param sourceId Component-ID of the source
	 */
	public void retryModbusCommunication(String sourceId) {
		// TODO
	}

	/**
	 * Called on EXECUTE_WRITE event.
	 */
	public void onExecuteWrite() {
		this.cycleTasksManager.onExecuteWrite();
	}

	/**
	 * Called on BEFORE_PROCESS_IMAGE event.
	 */
	public void onBeforeProcessImage() {
		this.cycleTasksManager.onBeforeProcessImage();
	}
}