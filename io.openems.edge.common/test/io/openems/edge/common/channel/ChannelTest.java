package io.openems.edge.common.channel;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class ChannelTest {

	@Test
	public void testWriteEnum() throws OpenemsException {
		WriteChannel<?> channel = new IntegerWriteChannel(null, ChannelId.TEST_CHANNEL_WITH_OPTIONS);

		// Test write with String
		channel.setNextWriteValueFromObject("Option 2");
		Optional<?> writtenValue = channel._getNextWriteValue();
		assertEquals(TestOptions.OPTION_2.getValue(), writtenValue.get());

		// Test write with Enum
		channel.setNextWriteValueFromObject(TestOptions.OPTION_1);
		writtenValue = channel._getNextWriteValue();
		assertEquals(TestOptions.OPTION_1.getValue(), writtenValue.get());
	}

}
