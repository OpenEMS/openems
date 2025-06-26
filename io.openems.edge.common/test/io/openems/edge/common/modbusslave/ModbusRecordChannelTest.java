package io.openems.edge.common.modbusslave;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.edge.common.modbusslave.ModbusType.UINT16;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.TestUtils;

public class ModbusRecordChannelTest {

	private class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			WRITE_ONLY_CHANNEL(Doc.of(OpenemsType.INTEGER) //
					.accessMode(WRITE_ONLY)),
			READ_ONLY_CHANNEL(Doc.of(OpenemsType.INTEGER) //
					.accessMode(AccessMode.READ_ONLY)),
			READ_WRITE_CHANNEL(Doc.of(OpenemsType.INTEGER) //
					.accessMode(READ_WRITE)); //

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		public DummyComponent(String id) {
			super(//
					OpenemsComponent.ChannelId.values(), //
					ChannelId.values() //
			);
			super.activate(null, id, "", true);
		}

		protected DummyComponent withReadWriteChannel(int value) {
			TestUtils.withValue(this, ChannelId.READ_WRITE_CHANNEL, value);
			return this;
		}

		protected DummyComponent withReadOnlyChannel(int value) {
			TestUtils.withValue(this, ChannelId.READ_ONLY_CHANNEL, value);
			return this;
		}

	}

	@Test
	public void testGetValue() {
		var component = new DummyComponent("foo0") //
				.withReadOnlyChannel(100) //
				.withReadWriteChannel(200);

		assertArrayEquals(ModbusRecordUint16.UNDEFINED_VALUE,
				new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.WRITE_ONLY_CHANNEL, READ_WRITE)
						.getValue(component));

		assertArrayEquals(ModbusRecordUint16.toByteArray(100),
				new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.READ_ONLY_CHANNEL, READ_WRITE)
						.getValue(component));
	}

}
