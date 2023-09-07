package io.openems.edge.bridge.modbus.sunspec;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

public class DummySunSpecComponent extends AbstractOpenemsSunSpecComponent {

	/**
	 * All models are active with low priority.
	 */
	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = Stream.of(DefaultSunSpecModel.values())
			.collect(Collectors.toMap(model -> model, model -> Priority.LOW, (a, b) -> a, TreeMap::new));

	public DummySunSpecComponent() throws OpenemsException {
		super(ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values()); //
		this.addBlocks();
	}

	private void addBlocks() throws OpenemsException {
		var startAddress = 40000;
		for (var entry : ACTIVE_MODELS.keySet()) {
			this.addBlock(startAddress, entry, ACTIVE_MODELS.get(entry));
		}

	}

	@Override
	protected void onSunSpecInitializationCompleted() {
	}

	/**
	 * Gets the length of the longest modbus task.
	 *
	 * @return the maximum task length
	 * @throws OpenemsException on error
	 */
	public int maximumTaskLenghth() throws OpenemsException {
		return this.getModbusProtocol() //
				.getTaskManager() //
				.getTasks() //
				.stream() //
				.mapToInt(Task::getLength) //
				.max().orElse(0);

	}

}
