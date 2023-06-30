package io.openems.edge.controller.api.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

public class RestHandlerTest {

	@Test
	public void testGetChannels() {
		var foo0 = new DummyComponent("foo0");
		var bar0 = new DummyComponent("bar0");
		var dummyComponent = new DummyComponent("dummyComponent");
		var components = List.<OpenemsComponent>of(foo0, bar0, dummyComponent);

		{
			var channelAddress = new ChannelAddress("foo0", "DummyChannel");
			var result = RestHandler.getChannels(components, channelAddress);
			assertEquals(1, result.size());
			assertEquals(foo0.channel("DummyChannel"), result.get(0));
		}

		{
			var channelAddress = new ChannelAddress(".*0", "Dummy.*");
			var result = RestHandler.getChannels(components, channelAddress);
			assertEquals(2, result.size());
			assertEquals(foo0.channel("DummyChannel"), result.get(0));
			assertEquals(bar0.channel("DummyChannel"), result.get(1));
		}

		{
			var channelAddress = new ChannelAddress(".*0", "DummyXY.*");
			var result = RestHandler.getChannels(components, channelAddress);
			assertEquals(0, result.size());
		}

		{
			var channelAddress = new ChannelAddress("dummyComponent", "Dummy.*");
			var result = RestHandler.getChannels(components, channelAddress);
			assertEquals(1, result.size());
			assertEquals(dummyComponent.channel("DummyChannel"), result.get(0));
		}

		{
			var channelAddress = new ChannelAddress("*", "");
			try {
				RestHandler.getChannels(components, channelAddress);
				fail();
			} catch (PatternSyntaxException e) {
				// ignore
			}
		}
	}

}
