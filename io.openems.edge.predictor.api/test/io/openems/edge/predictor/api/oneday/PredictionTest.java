package io.openems.edge.predictor.api.oneday;

import static org.junit.Assert.assertArrayEquals;

import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.predictor.api.prediction.Prediction;

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

}
