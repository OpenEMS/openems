package io.openems.edge.controller.api.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Test class for RestHandler.
 */
public class RestHandlerTest {

	/**
	 * Tests the getChannels method.
	 */
	@Test
	public void testGetChannels() {
		var foo0 = new DummyComponent("foo0");
		var bar0 = new DummyComponent("bar0");
		var dummyComponent = new DummyComponent("dummyComponent");
		var components = List.<OpenemsComponent>of(foo0, bar0, dummyComponent);

		// Create a dummy RestHandler for testing
		AbstractRestApi dummyApi = new DummyRestApi();
		RestHandler handler = new RestHandler(dummyApi);

		{
			var channelAddress = new ChannelAddress("foo0", "DummyChannel");
			var result = handler.getChannels(components, channelAddress);
			assertEquals(1, result.size());
			assertEquals(foo0.channel("DummyChannel"), result.get(0));
		}

		{
			var channelAddress = new ChannelAddress(".*0", "Dummy.*");
			var result = handler.getChannels(components, channelAddress);
			assertEquals(2, result.size());
			assertEquals(foo0.channel("DummyChannel"), result.get(0));
			assertEquals(bar0.channel("DummyChannel"), result.get(1));
		}

		{
			var channelAddress = new ChannelAddress(".*0", "DummyXY.*");
			var result = handler.getChannels(components, channelAddress);
			assertEquals(0, result.size());
		}

		{
			var channelAddress = new ChannelAddress("dummyComponent", "Dummy.*");
			var result = handler.getChannels(components, channelAddress);
			assertEquals(1, result.size());
			assertEquals(dummyComponent.channel("DummyChannel"), result.get(0));
		}

		{
			var channelAddress = new ChannelAddress("[a-z]*0", "Dummy.*");
			var result = handler.getChannels(components, channelAddress);
			assertEquals(2, result.size());
			assertEquals(foo0.channel("DummyChannel"), result.get(0));
			assertEquals(bar0.channel("DummyChannel"), result.get(1));
		}

		{
			var channelAddress = new ChannelAddress("*", "");
			try {
				handler.getChannels(components, channelAddress);
				fail("Expected PatternSyntaxException not thrown");
			} catch (Exception e) {
				// This is expected - should be a PatternSyntaxException
				// or would be caught by isLikelyRegex check
			}
		}
	}

	@Test
	public void testSplitPathPreservingBracketsWithoutRegex() {
		final var parts = RestHandler.splitPathPreservingBrackets("rest/channel/_sum/EssSoc");

		assertEquals(4, parts.size());
		assertEquals("rest", parts.get(0));
		assertEquals("channel", parts.get(1));
		assertEquals("_sum", parts.get(2));
		assertEquals("EssSoc", parts.get(3));
	}

	@Test
	public void testSplitPathPreservingBracketsWithRegex() {
		final var parts = RestHandler.splitPathPreservingBrackets("rest/channel/_sum/Ess[\\/]Soc");

		assertEquals(4, parts.size());
		assertEquals("rest", parts.get(0));
		assertEquals("channel", parts.get(1));
		assertEquals("_sum", parts.get(2));
		assertEquals("Ess[\\/]Soc", parts.get(3));
	}


}
