package io.openems.edge.bridge.modbus.api;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Builder class for creating {@link ModbusProtocol} instances.
 *
 * <p>
 * This builder allows combining multiple {@link Task}s or complete
 * {@link ModbusProtocol} definitions into a single protocol instance.
 */
public class ModbusProtocolBuilder {
	private final AbstractOpenemsModbusComponent component;
	private final List<Task> tasks = new ArrayList<>();

	private ModbusProtocolBuilder(AbstractOpenemsModbusComponent component) {
		this.component = component;
	}

	/**
	 * Creates a new {@link ModbusProtocolBuilder} instance.
	 *
	 * @param component the associated {@link OpenemsComponent}
	 * @return a new {@link ModbusProtocolBuilder}
	 */
	public static ModbusProtocolBuilder create(AbstractOpenemsModbusComponent component) {
		return new ModbusProtocolBuilder(component);
	}

	/**
	 * Adds all {@link Task}s from the given {@link ModbusProtocol} to this builder.
	 *
	 * @param protocol the protocol whose tasks should be added
	 * @return this builder instance for method chaining
	 */
	public ModbusProtocolBuilder addProtocol(ModbusProtocol protocol) {
		this.tasks.addAll(protocol.getTaskManager().getTasks());
		return this;
	}

	/**
	 * Adds a single {@link Task} to this builder.
	 *
	 * @param task the task to add
	 * @return this builder instance for method chaining
	 */
	public ModbusProtocolBuilder addTask(Task task) {
		this.tasks.add(task);
		return this;
	}

	/**
	 * Builds a new {@link ModbusProtocol} instance containing all previously added
	 * {@link Task}s.
	 *
	 * @return the constructed {@link ModbusProtocol}
	 */
	public ModbusProtocol build() {
		ModbusProtocol protocol = new ModbusProtocol(this.component);
		this.tasks.forEach(protocol::addTask);
		return protocol;
	}
}
