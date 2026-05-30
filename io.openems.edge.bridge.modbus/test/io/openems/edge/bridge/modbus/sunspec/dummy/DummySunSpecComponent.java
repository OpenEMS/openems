package io.openems.edge.bridge.modbus.sunspec.dummy;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

public class DummySunSpecComponent extends AbstractOpenemsSunSpecComponent
		implements ModbusComponent, OpenemsComponent {

	private static final Map<SunSpecModel, Priority> DEFAULT_ACTIVE_MODELS = ImmutableMap
			.<SunSpecModel, Priority>builder() //
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_101, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.HIGH) //
			.put(DefaultSunSpecModel.S_701, Priority.HIGH) //
			.put(DefaultSunSpecModel.S_702, Priority.LOW) //
			.build();

	@Override
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public DummySunSpecComponent() {
		super(//
				DEFAULT_ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values() //
		);
	}

	public DummySunSpecComponent(List<SunSpecModelEntry> activeModels) {
		super(//
				activeModels, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values() //
		);
	}

	public DummySunSpecComponent(Map<SunSpecModel, Priority> activeModels) {
		super(//
				activeModels, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), true, config.modbusUnitId(), config.readFromModbusBlock());
	}

	@Override
	public ModbusProtocol getModbusProtocol() {
		return super.getModbusProtocol();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
