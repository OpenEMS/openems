package io.openems.edge.predictor.api.oneday;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;

public class Prediction24HoursTest {

	@Test
	public void testOf() {
		assertArrayEquals(new Integer[] { 1, 5, 7, 0 /* TO_POSITIVE */, 9, null, null }, //
				Arrays.copyOfRange(//
						Prediction24Hours.of(//
								new ChannelAddress("_sum", "ProductionActivePower"), //
								1, 5, 7, -1, 9 //
						).getValues(), 0, 7));

		assertArrayEquals(new Integer[] { 1, 5, 7, -1 /* NONE */, 9, null, null }, //
				Arrays.copyOfRange(//
						Prediction24Hours.of(//
								new ChannelAddress("foo", "bar"), //
								1, 5, 7, -1, 9 //
						).getValues(), 0, 7));
	}

}
