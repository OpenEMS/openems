package io.openems.core.referencetarget;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ComponentIdFromChannelTransformerTest {

	private final ComponentIdFromChannelTransformer transformer = new ComponentIdFromChannelTransformer();

	@Test
	public void testSingleChannelAddress() {
		assertEquals("io0", this.transformer.transform("io0/Relay1"));
	}

	@Test
	public void testArrayRemovesDuplicatesAndPreservesOrder() {
		final var input = new String[] { "io1/Relay1", "io0/Relay1", "io1/Relay2" };

		final var result = (String[]) this.transformer.transform(input);

		assertArrayEquals(new String[] { "io1", "io0" }, result);
		assertArrayEquals(new String[] { "io1/Relay1", "io0/Relay1", "io1/Relay2" }, input);
	}

	@Test
	public void testEmptyEntriesAreSkipped() {
		final var result = (String[]) this.transformer.transform(
				new String[] { "", "io0/Relay1", "" });

		assertArrayEquals(new String[] { "io0" }, result);
	}

	@Test
	public void testEmptyInputs() {
		assertEquals("", this.transformer.transform(""));
		assertArrayEquals(new String[0], (String[]) this.transformer.transform(new String[0]));
	}

	@Test
	public void testInvalidSingleAddressIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> this.transformer.transform("invalid"));
	}

	@Test
	public void testInvalidArrayEntryIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> this.transformer.transform(new String[] { "io0/Relay1", "invalid" }));
	}

	@Test
	public void testUnsupportedTypeIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> this.transformer.transform(Integer.valueOf(42)));
	}
}