package io.openems.shared.influxdb.proxy;

import static io.openems.shared.influxdb.proxy.InfluxQlProxy.parseToJsonElement;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.shared.influxdb.proxy.InfluxQlProxy.Average;

public class InfluxQlProxyTest {

	@Test
	public void testAverage() {
		final var average = new Average();

		JsonElement last = JsonNull.INSTANCE;
		for (var next : List.of(//
				new JsonPrimitive(3.5), //
				JsonNull.INSTANCE, //
				new JsonPrimitive(4.5), //
				JsonNull.INSTANCE, //
				new JsonPrimitive(4d), //
				JsonNull.INSTANCE //
		)) {
			last = average.apply(last, next);
		}

		assertEquals(4d, last.getAsDouble(), 0d);
	}

	@Test
	public void testParseToJsonElement() {
		assertEquals(123.456, parseToJsonElement("123.456").getAsNumber());
		assertEquals(123L, parseToJsonElement("123").getAsNumber());
		assertEquals(" 123 ", parseToJsonElement(" 123 ").getAsString());
		assertEquals("foo-bar", parseToJsonElement("foo-bar").getAsString());
		assertEquals(JsonNull.INSTANCE, parseToJsonElement(null));
		assertEquals(JsonNull.INSTANCE, parseToJsonElement(""));
	}
}
