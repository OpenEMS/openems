package io.openems.edge.controller.api.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Test class for RestHandler.
 */
public class RestHandlerTest {

	/**
	 * Tests the getChannels method with the legacy approach.
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

	/**
	 * Tests the new getChannels method with explicit regex parameters.
	 */
	@Test
	public void testGetChannelsWithRegexParams() {
		var foo0 = new DummyComponent("foo0");
		var bar0 = new DummyComponent("bar0");
		var dummyComponent = new DummyComponent("dummyComponent");
		var components = List.<OpenemsComponent>of(foo0, bar0, dummyComponent);

		// Create a dummy RestHandler for testing
		AbstractRestApi dummyApi = new DummyRestApi();
		RestHandler handler = new RestHandler(dummyApi);

		// Test with component regex parameter
		{
			var channelAddress = new ChannelAddress("dummy", "DummyChannel"); // Placeholder values, will be ignored
			var result = handler.getChannels(components, channelAddress, ".*0", null);
			assertEquals(2, result.size());
			assertTrue(result.contains(foo0.channel("DummyChannel")));
			assertTrue(result.contains(bar0.channel("DummyChannel")));
		}

		// Test with channel regex parameter
		{
			var channelAddress = new ChannelAddress("dummyComponent", "dummy"); // Placeholder values, will be ignored
			var result = handler.getChannels(components, channelAddress, null, "Dummy.*");
			assertEquals(1, result.size());
			assertEquals(dummyComponent.channel("DummyChannel"), result.get(0));
		}

		// Test with both component and channel regex parameters
		{
			var channelAddress = new ChannelAddress("ignore", "ignore"); // Will be ignored
			var result = handler.getChannels(components, channelAddress, "[a-z]*0", "Dummy.*");
			assertEquals(2, result.size());
			assertTrue(result.contains(foo0.channel("DummyChannel")));
			assertTrue(result.contains(bar0.channel("DummyChannel")));
		}

		// Test with invalid regex
		{
			var channelAddress = new ChannelAddress("valid", "valid");
			try {
				handler.getChannels(components, channelAddress, "*", null);
				fail("Expected PatternSyntaxException not thrown");
			} catch (Exception e) {
				// This is expected - should be a PatternSyntaxException
			}
		}
	}

	/**
	 * Tests the parseQueryParams method which is used to extract query parameters.
	 */
	@Test
	public void testParseQueryParams() throws Exception {
		// Create a dummy RestHandler for testing
		AbstractRestApi dummyApi = new DummyRestApi();
		RestHandler handler = new RestHandler(dummyApi);

		// Use reflection to access the private method
		var parseQueryParamsMethod = RestHandler.class.getDeclaredMethod("parseQueryParams", String.class);
		parseQueryParamsMethod.setAccessible(true);

		// Test with empty query string
		{
			@SuppressWarnings("unchecked")
			Map<String, String> params = (Map<String, String>) parseQueryParamsMethod.invoke(handler, (String) null);
			assertNotNull(params);
			assertTrue(params.isEmpty());
		}

		// Test with simple query parameters
		{
			@SuppressWarnings("unchecked")
			Map<String, String> params = (Map<String, String>) parseQueryParamsMethod.invoke(handler,
					"component=foo0&channel=DummyChannel");
			assertNotNull(params);
			assertEquals(2, params.size());
			assertEquals("foo0", params.get("component"));
			assertEquals("DummyChannel", params.get("channel"));
		}

		// Test with regex query parameters
		{
			@SuppressWarnings("unchecked")
			Map<String, String> params = (Map<String, String>) parseQueryParamsMethod.invoke(handler,
					"componentRegex=.*0&channelRegex=Dummy.*");
			assertNotNull(params);
			assertEquals(2, params.size());
			assertEquals(".*0", params.get("componentRegex"));
			assertEquals("Dummy.*", params.get("channelRegex"));
		}

		// Test with mixed parameters, including encoded values
		{
			@SuppressWarnings("unchecked")
			Map<String, String> params = (Map<String, String>) parseQueryParamsMethod.invoke(handler,
					"component=foo0&channelRegex=Dummy%5B0-9%5D");
			assertNotNull(params);
			assertEquals(2, params.size());
			assertEquals("foo0", params.get("component"));
			assertEquals("Dummy%5B0-9%5D", params.get("channelRegex")); // The method doesn't decode URL encoding
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
