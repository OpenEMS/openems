package io.openems.edge.io.revpi.hardware.api.dio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// Inheritance so that the protected functions can be tested
public class RevPiHardwareApiDioTest extends RevPiHardwareApiDio {

	public RevPiHardwareApiDioTest() {
		super("TestIn_", "TestOut_", "", "", 0, 0);
	}

	@org.junit.Test
	public void testGetChannelIndexBySplitWithValidIdShouldReturnIndex() {
		final int expectedIndex = 10;
		int actualIndex = -1;

		try {
			actualIndex = this.getChannelIndexBySplit("Out10");
		} catch (Exception e) {
			fail("Test failed because: " + e.getMessage());
		}
		assertEquals(expectedIndex, actualIndex);
	}

	@org.junit.Test
	public void testGetChannelIndexBySplitWithEmptyStringShouldReturnInvalidIndex() {
		final int expectedIndex = -1;
		int actualIndex = 0;
		try {
			actualIndex = this.getChannelIndexBySplit("");
		} catch (Exception e) {
		}

		assertEquals(expectedIndex, actualIndex);
	}

	@org.junit.Test
	public void testParseIsChannelUsedWithValidStringNotUsedShouldReturnFalse() {
		final int index = 2;
		final boolean expectedValue = true;
		boolean actualValue = false;

		actualValue = this.parseIsChannelUsed("0|1|0|1|1|1|1|1|1|1|1|1|1|1|1|1", index);

		assertEquals(expectedValue, actualValue);
	}

	@org.junit.Test
	public void testParseIsChannelUsedWithValidStringUsedShouldReturnTrue() {
		final int index = 4;
		final boolean expectedValue = false;
		boolean actualValue = true;

		actualValue = this.parseIsChannelUsed("1|1|1|0|1|1|1|1|1|1|1|1|1|1|1|1", index);

		assertEquals(expectedValue, actualValue);
	}

	@org.junit.Test
	public void testParseIsChannelUsedWithInvalidStringShouldReturnFalse() {
		final int index = 4;
		final boolean expectedValue = false;
		boolean actualValue = true;

		actualValue = this.parseIsChannelUsed("1|1|1|-1|1|1|1|1|1|1|1|1|1|1|1|1", index);

		assertEquals(expectedValue, actualValue);
	}

	@org.junit.Test
	public void testParseIsChannelUsedWithEmptyStringShouldReturnFalse() {
		// Tests both empty array and out of bounds access
		final int index = 1;
		final boolean expectedValue = false;
		boolean actualValue = true;

		actualValue = this.parseIsChannelUsed("", index);

		assertEquals(expectedValue, actualValue);
	}
}
