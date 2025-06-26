package io.openems.edge.common.channel;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.DummyOptionsEnum;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.test.TestUtils;

public class ChannelUtilsTest {

	@Test
	public void testSetWriteValueIfNotReadBoolean() throws OpenemsNamedException {
		var channel = (BooleanWriteChannel) Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.createChannelInstance(null, null);
		testSetWriteValueIfNotRead(//
				channel, false, true, () -> setWriteValueIfNotRead(channel, true));
	}

	@Test
	public void testSetWriteValueIfNotReadInteger() throws OpenemsNamedException {
		var channel = (IntegerWriteChannel) Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.createChannelInstance(null, null);
		testSetWriteValueIfNotRead(//
				channel, 0, 123, () -> setWriteValueIfNotRead(channel, 123));
	}

	@Test
	public void testSetWriteValueIfNotReadEnum() throws OpenemsNamedException {
		var channel = (EnumWriteChannel) Doc.of(DummyOptionsEnum.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.createChannelInstance(null, null);
		testSetWriteValueIfNotRead(//
				channel, DummyOptionsEnum.UNDEFINED.getValue(), DummyOptionsEnum.VALUE_1.getValue(),
				() -> setWriteValueIfNotRead(channel, DummyOptionsEnum.VALUE_1));
	}

	private static <T, C extends WriteChannel<?>> void testSetWriteValueIfNotRead(C channel, T testValue1, T testValue2,
			ThrowingRunnable<OpenemsNamedException> method) throws OpenemsNamedException {
		// prepare
		var setvalue = new AtomicReference<Object>(null);
		channel.onSetNextWrite(v -> setvalue.set(v));

		// initialize
		TestUtils.withValue(channel, testValue1);
		assertEquals(testValue1, channel.value().get());
		assertEquals(Optional.empty(), channel.getNextWriteValue());
		assertNull(setvalue.get());

		// set value
		method.run();
		assertEquals(testValue2, setvalue.getAndSet(null)); // value was set
		assertEquals(testValue2, channel.getNextWriteValue().get());
		TestUtils.withValue(channel, testValue2);

		// NO-OP
		method.run();
		assertNull(setvalue.getAndSet(null)); // value was not set
		assertEquals(testValue2, channel.getNextWriteValue().get());
	}

}
