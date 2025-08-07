package io.openems.edge.common.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ProgressTest {

	@Test
	public void assertPercentage() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Progress(-1);
		});
		new Progress(0);
		new Progress(1);
		new Progress(99);
		new Progress(100);
		assertThrows(IllegalArgumentException.class, () -> {
			new Progress(101);
		});
	}

	@Test
	public void testToStringWithTitle() {
		assertEquals("  5% some random title", new Progress(5, "some random title").toString());
		assertEquals(" 13% some random title", new Progress(13, "some random title").toString());
		assertEquals("100% some random title", new Progress(100, "some random title").toString());
	}

	@Test
	public void testToStringWithoutTitle() {
		assertEquals("  5%", new Progress(5).toString());
		assertEquals(" 13%", new Progress(13).toString());
		assertEquals("100%", new Progress(100).toString());
	}
}