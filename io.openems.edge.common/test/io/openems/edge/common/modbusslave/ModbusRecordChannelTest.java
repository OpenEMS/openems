package io.openems.edge.common.modbusslave;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.edge.common.modbusslave.ModbusRecordChannel.parseValue;
import static io.openems.edge.common.modbusslave.ModbusType.ENUM16;
import static io.openems.edge.common.modbusslave.ModbusType.FLOAT32;
import static io.openems.edge.common.modbusslave.ModbusType.FLOAT64;
import static io.openems.edge.common.modbusslave.ModbusType.STRING16;
import static io.openems.edge.common.modbusslave.ModbusType.UINT16;
import static io.openems.edge.common.modbusslave.ModbusType.UINT32;
import static io.openems.edge.common.modbusslave.ModbusType.UINT64;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

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
			WRITE_ONLY_CHANNEL(Doc.of(OpenemsType.INTEGER)//
					.accessMode(WRITE_ONLY)),
			READ_ONLY_CHANNEL(Doc.of(OpenemsType.INTEGER)//
					.accessMode(AccessMode.READ_ONLY)),
			READ_WRITE_CHANNEL(Doc.of(OpenemsType.INTEGER)//
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

		assertArrayEquals(ModbusRecordUint16.UNDEFINED_BYTE_ARRAY,
				new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.WRITE_ONLY_CHANNEL, READ_WRITE)
						.getValue(component));

		assertArrayEquals(ModbusRecordUint16.toByteArray(100),
				new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.READ_ONLY_CHANNEL, READ_WRITE)
						.getValue(component));
	}


	@Test
	public void testParseValueUint16() {
		assertEquals((short) 42, parseValue(UINT16, ByteBuffer.allocate(2).putShort((short) 42).array()));
	}

	@Test
	public void testParseValueUint16Undefined() {
		assertNull(parseValue(UINT16, ModbusRecordUint16.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testParseValueEnum16() {
		assertEquals((short) 3, parseValue(ENUM16, ByteBuffer.allocate(2).putShort((short) 3).array()));
	}

	@Test
	public void testParseValueEnum16Undefined() {
		assertNull(parseValue(ENUM16, ModbusRecordUint16.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testParseValueUint32() {
		assertEquals(42, parseValue(UINT32, ByteBuffer.allocate(4).putInt(42).array()));
	}

	@Test
	public void testParseValueUint32Undefined() {
		assertNull(parseValue(UINT32, ModbusRecordUint32.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testParseValueUint64() {
		assertEquals(42L, parseValue(UINT64, ByteBuffer.allocate(8).putLong(42L).array()));
	}

	@Test
	public void testParseValueUint64Undefined() {
		assertNull(parseValue(UINT64, ModbusRecordUint64.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testParseValueFloat32() {
		assertEquals(42.5f, parseValue(FLOAT32, ByteBuffer.allocate(4).putFloat(42.5f).array()));
	}

	@Test
	public void testParseValueFloat32Undefined() {
		assertNull(parseValue(FLOAT32, ModbusRecordFloat32.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testParseValueFloat64() {
		assertEquals(42.5, parseValue(FLOAT64, ByteBuffer.allocate(8).putDouble(42.5).array()));
	}

	@Test
	public void testParseValueFloat64Undefined() {
		assertNull(parseValue(FLOAT64, ModbusRecordFloat64.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testParseValueString16() {
		var raw = new byte[32];
		var hello = "hello".getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(hello, 0, raw, 0, hello.length);
		assertEquals("hello", parseValue(STRING16, raw));
	}

	@Test
	public void testParseValueString16Undefined() {
		assertNull(parseValue(STRING16, ModbusRecordString16.UNDEFINED_BYTE_ARRAY));
	}


	@Test
	public void testWriteValueCallsCallback() {
		var sut = new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.READ_WRITE_CHANNEL, READ_WRITE);
		var result = new AtomicReference<Object>(new Object()); // sentinel
		sut.onWriteValue(result::set);
		sut.writeValue(0, (byte) 0, (byte) 42);
		assertEquals((short) 42, result.get());
	}

	@Test
	public void testWriteValueUndefinedCallsCallbackWithNull() {
		var sut = new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.READ_WRITE_CHANNEL, READ_WRITE);
		var result = new AtomicReference<Object>(new Object()); // sentinel
		sut.onWriteValue(result::set);
		sut.writeValue(0, (byte) 0xFF, (byte) 0xFF);
		assertNull(result.get());
	}


	@Test
	public void testWriteValueReadOnlyIgnored() {
		var sut = new ModbusRecordChannel(0, UINT16, DummyComponent.ChannelId.READ_ONLY_CHANNEL, READ_WRITE);
		var result = new AtomicReference<Object>(null);
		sut.onWriteValue(result::set);
		sut.writeValue(0, (byte) 0, (byte) 42);
		assertNull(result.get()); // callback should not have been called
	}

}
