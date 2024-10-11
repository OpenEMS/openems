package io.openems.edge.bridge.modbus.api.worker;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.bridge.modbus.api.worker.internal.CycleTasks;
import io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksManager;
import io.openems.edge.bridge.modbus.api.worker.internal.DefectiveComponents;
import io.openems.edge.bridge.modbus.api.worker.internal.TasksSupplierImpl;

/**
 * The ModbusWorker schedules the execution of all Modbus-Tasks, like reading
 * and writing modbus registers.
 *
 * <p>
 * It tries to execute all Write-Tasks as early as possible (directly after the
 * TOPIC_CYCLE_EXECUTE_WRITE event) and all Read-Tasks as late as possible to
 * have values available exactly when they are needed (i.e. at the
 * TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event). For this it uses a
 * {@link CycleTasksManager} that internally uses a {@link TasksSupplierImpl}
 * that supplies the tasks for one Cycle ({@link CycleTasks}).
 */
public class ModbusWorker extends AbstractImmediateWorker {

	// Callbacks
	private final Function<Task, ExecuteState> execute;
	private final Consumer<ModbusElement[]> invalidate;

	private final DefectiveComponents defectiveComponents;
	private final TasksSupplierImpl tasksSupplier;
	private final CycleTasksManager cycleTasksManager;

	/**
	 * Constructor for {@link ModbusWorker}.
	 * 
	 * @param execute                    executes a {@link Task}; returns number of
	 *                                   actually executed subtasks
	 * @param invalidate                 invalidates the given
	 *                                   {@link ModbusElement}s after read errors
	 * @param cycleTimeIsTooShortChannel sets the
	 *                                   {@link BridgeModbus.ChannelId#CYCLE_TIME_IS_TOO_SHORT}
	 *                                   channel
	 * @param cycleDelayChannel          sets the
	 *                                   {@link BridgeModbus.ChannelId#CYCLE_DELAY}
	 *                                   channel
	 * @param logVerbosity               the configured {@link LogVerbosity}
	 */
	public ModbusWorker(Function<Task, ExecuteState> execute, Consumer<ModbusElement[]> invalidate,
			Consumer<Boolean> cycleTimeIsTooShortChannel, Consumer<Long> cycleDelayChannel,
			AtomicReference<LogVerbosity> logVerbosity) {
		this.execute = execute;
		this.invalidate = invalidate;

		this.defectiveComponents = new DefectiveComponents(logVerbosity);
		this.tasksSupplier = new TasksSupplierImpl();
		this.cycleTasksManager = new CycleTasksManager(this.tasksSupplier, this.defectiveComponents,
				cycleTimeIsTooShortChannel, cycleDelayChannel, logVerbosity);
	}

	@Override
	protected void forever() throws InterruptedException {
		var task = this.cycleTasksManager.getNextTask();

		// execute the task
		var result = this.execute.apply(task);

		// NOTE: with Java 21 LTS this can be refactored to a pattern matching switch
		// statement
		if (result instanceof ExecuteState.Ok) {
			// no exception & at least one sub-task executed
			this.markComponentAsDefective(task.getParent(), false);

		} else if (result instanceof ExecuteState.NoOp) {
			// did not execute anything

		} else if (result instanceof ExecuteState.Error) {
			this.markComponentAsDefective(task.getParent(), true);

			// invalidate elements of this task
			this.invalidate.accept(task.getElements());
		}
	}

	/**
	 * Marks the given {@link ModbusComponent} as defective or non-defective.
	 * 
	 * <ul>
	 * <li>Sets 'ModbusCommunicationFailed' Channel of the ModbusComponent
	 * <li>Adds/Removes the component to/from the {@link DefectiveComponents}
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
		this.tasksSupplier.addProtocol(sourceId, protocol, this.invalidate);
		this.defectiveComponents.remove(sourceId); // Cleanup
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		this.tasksSupplier.removeProtocol(sourceId, this.invalidate);
		this.defectiveComponents.remove(sourceId); // Cleanup
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
		this.defectiveComponents.remove(sourceId);
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