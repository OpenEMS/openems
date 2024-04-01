package io.openems.shared.influxdb.proxy;

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

}
