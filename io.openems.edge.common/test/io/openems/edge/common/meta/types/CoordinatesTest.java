package io.openems.edge.common.meta.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CoordinatesTest {

	@Test
	public void testCoordinates_ShouldThrowException_WhenInvalidLatitude() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Coordinates(-91.0, 0.0);
		});
	}

	@Test
	public void testCoordinates_ShouldThrowException_WhenInvalidLongitude() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Coordinates(0.0, 181.0);
		});
	}

	@Test
	public void testCoordinatesOf_ShouldReturnNotNull_WhenValidCoordinates() {
		var result = Coordinates.of(52.5200, 13.4050);
		assertNotNull(result);
		assertEquals(52.5200, result.latitude(), 0.0);
		assertEquals(13.4050, result.longitude(), 0.0);
	}

	@Test
	public void testCoordinatesOf_ShouldReturnNull_WhenInvalidValues() {
		double[][] invalidCases = { //
				{ -91.0, 0.0 }, // latitude too low
				{ 91.0, 0.0 }, // latitude too high
				{ 0.0, -181.0 }, // longitude too low
				{ 0.0, 181.0 } // longitude too high
		};

		for (var coords : invalidCases) {
			var result = Coordinates.of(coords[0], coords[1]);
			assertNull(result);
		}
	}

	@Test
	public void testCoordinatesOf_ShouldAcceptEdgeCases() {
		assertNotNull(Coordinates.of(-90.0, -180.0));
		assertNotNull(Coordinates.of(90.0, 180.0));
	}
}
