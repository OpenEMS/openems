package io.openems.edge.common.linecharacteristic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class PolyLineTest {

	@Test
	public void test() throws OpenemsNamedException {
		var lineConfig = JsonUtils.buildJsonArray()//
				.add(JsonUtils.buildJsonObject()//
						.addProperty("xCoord", 0.9) //
						.addProperty("yCoord", 60) //
						.build()) //
				.add(JsonUtils.buildJsonObject()//
						.addProperty("xCoord", 0.93) //
						.addProperty("yCoord", 0) //
						.build()) //
				.add(JsonUtils.buildJsonObject()//
						.addProperty("xCoord", 1.07) //
						.addProperty("yCoord", 0) //
						.build()) //
				.add(JsonUtils.buildJsonObject()//
						.addProperty("xCoord", 1.1) //
						.addProperty("yCoord", -60) //
						.build() //
				).build();

		var polyline = new PolyLine("xCoord", "yCoord", lineConfig);

		// exactly first
		assertEquals(60f, polyline.getValue(0.9), 0.00001);

		// exactly last
		assertEquals(-60f, polyline.getValue(1.1), 0.00001);

		// beyond last
		assertEquals(-60f, polyline.getValue(1.2), 0.00001);

		// before first
		assertEquals(60f, polyline.getValue(0.7), 0.00001);

		// between first two
		assertEquals(30f, polyline.getValue(0.915), 0.001);
	}

	@Test
	public void testEmpty() throws OpenemsNamedException {
		var lineConfig = JsonUtils.buildJsonArray().build();

		var polyline = new PolyLine("xCoord", "yCoord", lineConfig);

		// exactly first
		assertEquals(null, polyline.getValue(0.9));
	}

}
