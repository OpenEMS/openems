package io.openems.edge.bridge.modbus.api.element;

import java.util.function.BiFunction;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.taskmanager.Priority;

public class ModbusTest<TASK extends AbstractTask, ELEMENT extends AbstractModbusElement<?>, CHANNEL extends Channel<?>>
		extends DummyModbusComponent {

	private static final String CHANNEL_ID = "CHANNEL";

	public final CHANNEL channel;
	public final ELEMENT element;
	public final TASK task;

	@SuppressWarnings("unchecked")
	private ModbusTest(ELEMENT element, BiFunction<Integer, Priority, TASK> taskFactory, AccessMode accessMode,
			OpenemsType openemsType) throws OpenemsException {
		super();
		var channelId = new ChannelIdImpl(CHANNEL_ID, Doc.of(openemsType).accessMode(accessMode));
		this.channel = (CHANNEL) this.addChannel(channelId);
		this.element = this.m(this.channel.channelId(), element);
		this.task = taskFactory.apply(0, Priority.LOW);
		this.getModbusProtocol().addTask(this.task);
	}

	public static class FC1ReadCoils<ELEMENT extends AbstractModbusElement<?>, CHANNEL extends AbstractReadChannel<?, ?>>
			extends ModbusTest<FC1ReadCoilsTask, ELEMENT, CHANNEL> {
		public FC1ReadCoils(ELEMENT element, OpenemsType openemsType) throws OpenemsException {
			super(element, //
					(startAddress, priority) -> new FC1ReadCoilsTask(startAddress, priority, element), //
					AccessMode.READ_ONLY, openemsType);
		}
	}

	public static class FC5WriteCoil<ELEMENT extends AbstractModbusElement<?>, CHANNEL extends WriteChannel<?>>
			extends ModbusTest<FC5WriteCoilTask, ELEMENT, CHANNEL> {
		@SuppressWarnings("unchecked")
		public FC5WriteCoil(ModbusCoilElement element, OpenemsType openemsType) throws OpenemsException {
			super((ELEMENT) element, //
					(startAddress, priority) -> new FC5WriteCoilTask(startAddress, element), //
					AccessMode.READ_WRITE, openemsType);
		}
	}

	public static class FC3ReadRegisters<ELEMENT extends AbstractModbusElement<?>, CHANNEL extends AbstractReadChannel<?, ?>>
			extends ModbusTest<FC3ReadRegistersTask, ELEMENT, CHANNEL> {
		public FC3ReadRegisters(ELEMENT element, OpenemsType openemsType) throws OpenemsException {
			super(element, //
					(startAddress, priority) -> new FC3ReadRegistersTask(startAddress, priority, element), //
					AccessMode.READ_ONLY, openemsType);
		}
	}

	public static class FC6WriteRegister<ELEMENT extends AbstractModbusElement<?>, CHANNEL extends WriteChannel<?>>
			extends ModbusTest<FC6WriteRegisterTask, ELEMENT, CHANNEL> {
		public FC6WriteRegister(ELEMENT element, OpenemsType openemsType) throws OpenemsException {
			super(element, //
					(startAddress, priority) -> new FC6WriteRegisterTask(startAddress, element), //
					AccessMode.READ_WRITE, openemsType);
		}
	}

	public static class FC16WriteRegisters<ELEMENT extends AbstractModbusElement<?>, CHANNEL extends WriteChannel<?>>
			extends ModbusTest<FC16WriteRegistersTask, ELEMENT, CHANNEL> {
		public FC16WriteRegisters(ELEMENT element, OpenemsType openemsType) throws OpenemsException {
			super(element, //
					(startAddress, priority) -> new FC16WriteRegistersTask(startAddress, element), //
					AccessMode.READ_WRITE, openemsType);
		}
	}

}