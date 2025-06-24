package io.openems.common.jsonrpc.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.utils.JsonUtils;

public class GetChannelsOfComponentTest {

	@Test
	public void testRequestSerializer() {
		final var result = GetChannelsOfComponent.Request.serializer().deserialize(JsonUtils.buildJsonObject() //
				.addProperty("componentId", "ess0") //
				.build());
		assertEquals(new GetChannelsOfComponent.Request("ess0", false), result);
	}

}