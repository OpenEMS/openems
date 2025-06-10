package io.openems.edge.predictor.api.prediction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.predictor.api.prediction.Prediction.ValueRange;

public class PredictionTest {

	@Test
	public void testOf() {
		var now = ZonedDateTime.now();
		var sum = new DummySum();

		assertArrayEquals(new Integer[] { 1, 5, 7, 0 /* ValueRange positive */, 9, null, null }, //
				Arrays.copyOfRange(//
						Prediction.from(sum, //
								new ChannelAddress("_sum", "ProductionActivePower"), now, //
								1, 5, 7, -1, 9 //
						).asArray(), 0, 7));

		assertArrayEquals(new Integer[] { 1, 5, 7, -1 /* NONE */, 9, null, null }, //
				Arrays.copyOfRange(//
						Prediction.from(sum, //
								new ChannelAddress("foo", "bar"), now, //
								1, 5, 7, -1, 9 //
						).asArray(), 0, 7));
	}

	@Test
	public void testValueRange() {
		{
			var vr = new ValueRange(null, null);
			assertNull(vr.fitWithin(null));
			assertEquals(100, vr.fitWithin(100).intValue());
		}
		{
			var vr = new ValueRange(-100, null);
			assertNull(vr.fitWithin(null));
			assertEquals(-100, vr.fitWithin(-150).intValue());
			assertEquals(150, vr.fitWithin(150).intValue());
		}
		{
			var vr = new ValueRange(null, 100);
			assertNull(vr.fitWithin(null));
			assertEquals(-150, vr.fitWithin(-150).intValue());
			assertEquals(100, vr.fitWithin(150).intValue());
		}
		{
			var vr = new ValueRange(-100, 100);
			assertNull(vr.fitWithin(null));
			assertEquals(-100, vr.fitWithin(-150).intValue());
			assertEquals(100, vr.fitWithin(150).intValue());
		}
	}
}
