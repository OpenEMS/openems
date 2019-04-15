package io.openems.edge.common.channel;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class ChannelTest {

	private static enum TestChannelId implements ChannelId {
		TEST_CHANNEL_WITH_OPTIONS(Doc.of(TestOptions.values()).accessMode(AccessMode.READ_WRITE));

		private final Doc doc;

		private TestChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Test
	public void testWriteEnum() throws OpenemsNamedException {
		TestChannelId channelId = TestChannelId.TEST_CHANNEL_WITH_OPTIONS;
		EnumWriteChannel channel = channelId.doc().createChannelInstance(null, channelId);

		// Test write with String
		channel.setNextWriteValue("Option 2");
		Optional<?> writtenValue = channel.getNextWriteValue();
		assertEquals(TestOptions.OPTION_2.getValue(), writtenValue.get());

		// Test write with Enum
		channel.setNextWriteValue(TestOptions.OPTION_1);
		writtenValue = channel.getNextWriteValue();
		assertEquals(TestOptions.OPTION_1.getValue(), writtenValue.get());
	}

}
