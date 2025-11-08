package io.openems.edge.common.doc;

import static io.openems.edge.common.doc.TestComponent.ChannelId.TEST_ENUM_CHANNEL;
import static io.openems.edge.common.doc.TestComponent.ChannelId.TEST_INTEGER_CHANNEL;
import static io.openems.edge.common.doc.TestComponent.ChannelId.TEST_STRING_CHANNEL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.test.DummyOptionsEnum;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;

public class DocTest {

	@Test
	public void testTextByChannel() throws Exception {

		final var component = new TestComponent("dummy0");
		final var docForEnum = TestComponent.ChannelId.TEST_ERROR_CHANNEL_FOR_ENUM.doc();
		final var docForInt = TestComponent.ChannelId.TEST_ERROR_CHANNEL_FOR_INTEGER.doc();
		final var docForString = TestComponent.ChannelId.TEST_ERROR_CHANNEL_FOR_STRING.doc();
		ComponentTest test;

		test = new ComponentTest(component) //
				.activate(new AbstractComponentConfig(null, "dummy0")) //
				.next(new AbstractComponentTest.TestCase() //
						.input(TEST_ENUM_CHANNEL, DummyOptionsEnum.VALUE_1) //
						.input(TEST_INTEGER_CHANNEL, 2500) //
						.input(TEST_STRING_CHANNEL, "Interstellar"));

		assertEquals("value is 1", docForEnum.getText());
		assertEquals("power has a consistent power range", docForInt.getText());
		assertEquals("<3", docForString.getText());

		test.next(new AbstractComponentTest.TestCase() //
				.input(TEST_ENUM_CHANNEL, DummyOptionsEnum.UNDEFINED) //
				.input(TEST_INTEGER_CHANNEL, 1000) //
				.input(TEST_STRING_CHANNEL, "Star Wars 8: The last Jedi Knight"));

		assertEquals("undefined", docForEnum.getText());
		assertEquals("power is fine", docForInt.getText());
		assertEquals(":(", docForString.getText());

		test.next(new AbstractComponentTest.TestCase() //
				.input(TEST_INTEGER_CHANNEL, 5001) //
				.input(TEST_STRING_CHANNEL, "Deadpool"));

		assertEquals("power is too high", docForInt.getText());
		assertEquals(":)", docForString.getText());

		test.next(new AbstractComponentTest.TestCase() //
				.input(TEST_INTEGER_CHANNEL, -100) //
				.input(TEST_STRING_CHANNEL, "WALLE"));

		assertEquals("power is negative", docForInt.getText());
		assertEquals(":|", docForString.getText());

		test.next(new AbstractComponentTest.TestCase() //
				.input(TEST_INTEGER_CHANNEL, null) //
				.input(TEST_STRING_CHANNEL, null) //
				.input(TEST_ENUM_CHANNEL, null));

		// FIXME assertEquals("power is fine", docForInt.getText());
		assertEquals(":|", docForString.getText());
		assertEquals("undefined", docForEnum.getText());

	}
}
