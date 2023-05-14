//package io.openems.edge.bridge.modbus.api.worker;
//
//import io.openems.common.exceptions.OpenemsException;
//import io.openems.edge.bridge.modbus.DummyModbusComponent;
//import io.openems.edge.bridge.modbus.api.ModbusProtocol;
//import io.openems.edge.bridge.modbus.api.task.Task;
//import io.openems.edge.common.channel.Doc;
//
//public abstract class AbstractDummyComponent extends DummyModbusComponent {
//
//	/**
//	 * Builds an {@link AbstractDummyComponent}.
//	 * 
//	 * @param componentId the Component-ID of the new component
//	 * @param bridge      the {@link DummyModbusBridge}
//	 * @param tasks       the {@link Task}s
//	 * @return the new instance
//	 * @throws OpenemsException on error
//	 */
//	public static AbstractDummyComponent of(String componentId, DummyModbusBridge bridge, AbstractDummyTask... tasks)
//			throws OpenemsException {
//		return new AbstractDummyComponent(componentId, bridge) {
//
//			@Override
//			public Task[] getTasks() {
//				return tasks;
//			}
//		};
//
//	}
//
//	public AbstractDummyComponent(String componentId, DummyModbusBridge bridge) throws OpenemsException {
//		super(componentId, bridge, ModbusWorkerTest.UNIT_ID, ChannelId.values());
//	}
//
//	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
//		; //
//
//		private final Doc doc;
//
//		private ChannelId(Doc doc) {
//			this.doc = doc;
//		}
//
//		@Override
//		public Doc doc() {
//			return this.doc;
//		}
//	}
//
//	@Override
//	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
//		return new ModbusProtocol(this, this.getTasks());
//	}
//
//	/**
//	 * Gets the Component {@link Task}s.
//	 * 
//	 * @return array of Tasks
//	 */
//	public abstract Task[] getTasks();
//
//}