package io.openems.edge.bridge.modbus.jsonrpc;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.test.DummyModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.taskmanager.Priority;

public class ModbusRegistersExportXlsxResponseTest {

	private static class MyDummyModbusComponent extends DummyModbusComponent {

		public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			CHANNEL1(Doc.of(OpenemsType.INTEGER));

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		public MyDummyModbusComponent(String id) throws OpenemsException {
			super(id, ChannelId.values());
		}

		@Override
		protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
			return new ModbusProtocol(this, //
					new FC3ReadRegistersTask(100, Priority.LOW, //
							m(MyDummyModbusComponent.ChannelId.CHANNEL1, new UnsignedWordElement(100)))); //
		}
	};

	@Test
	public void test() throws Exception {
		DummyModbusComponent component = new MyDummyModbusComponent("id0");
		ModbusRegistersExportXlsxResponse response = (ModbusRegistersExportXlsxResponse) component
				.handleJsonrpcRequest(null, new ModbusRegistersExportXlsxRequest()).get();
		System.out.println(response.getPayload());
	}

}
